### Rate limiting and quota for scoped client credential endpoints

* Status: final
* Who: proposed and finalised by RL
* When: 2026-08-26
* Related: RAID-711 (epic), RAID-827 (story), RAID-849 (this spike),
  RAID-767 (source investigation), RAID-714 (RAID-827 was split from it)


# Context

RAID-827 adds Keycloak SPI endpoints that let a Service Point Admin self-serve
their own service point's API client credentials: create, list, rotate, revoke
and get-secret. The story left rate limiting as an explicit open question.
RAID-849 investigated the current state and decided it.

The investigation established the following, all of which shape the decision.

1. **Nothing throttles anything today.** There is no rate limiting, throttling
   or backpressure in `api-svc/` or `iam/`: no bucket4j, resilience4j, Guava
   `RateLimiter`, and no request-counting filter. The only throttle code in the
   repo is outbound client-side backoff in build tooling
   (`buildSrc/src/main/java/au/org/raid/api/Utils.java`), used to poll the ARDC
   SPARQL vocabulary endpoint politely during code generation.

2. **There is no edge layer to lean on.** `raido-v2-aws-private` contains no
   WAFv2 WebACL, no rate-based rules, no API Gateway throttling or usage plans,
   and no CloudFront or ALB request limiting. Keycloak is reached at
   `iam.<zone>` through an internet-facing Application Load Balancer whose only
   listener filtering is host-header matching
   (`registration-authority/cdk/lib/raid/stack/api.ts`). CloudFront fronts the
   SPA and proxies some `api.<zone>` paths, but it does not front `iam.<zone>`,
   so there is no existing hop where a WebACL could attach.

3. **The `iam` module has no filter infrastructure.** It is a plain library jar
   dropped into `/opt/keycloak/providers/`, with no `ContainerRequestFilter`,
   no `ContainerResponseFilter` and no servlet filters of its own. The only
   extension points in use are `@Provider`-annotated JAX-RS resources reached
   through `RealmResourceProvider` dispatch.

4. **There is no working per-endpoint SPI configuration mechanism.** All three
   existing `RealmResourceProviderFactory` implementations have empty
   `init(Config.Scope)` bodies, and `getResource()` constructs a fresh
   controller per request, so `Config.Scope` never reaches the resource.
   `GroupController` works around this by reading
   `System.getenv("RAID_FLAT_GROUP_ADMIN_FALLBACK")`
   (`iam/src/main/java/au/org/raid/iam/provider/group/GroupController.java`,
   around lines 62 to 68). Controller-per-request also means any counter state
   would have to be static, with its own eviction design.

5. **Keycloak brute-force protection is disabled.** `bruteForceProtected` is
   `false` in `iam/realms/raid-realm.json` (line 38), which makes the
   accompanying `failureFactor: 30` and `maxFailureWaitSeconds: 900` inert. It
   would not have helped these endpoints in any case, because Keycloak's
   brute-force detection covers login and credential flows, not custom SPI
   resources.


# Decision

**1. No per-request rate limiting on the RAID-827 endpoints.**

Every one of these endpoints requires an authenticated bearer token and is
scoped to a single service point. There is nothing to guess: a caller either
holds `service-point-admin:<groupId>` or does not, so `get-secret` is not an
enumeration target and the brute-force threat that per-request rate limiting
exists to address is small. Building it would require new filter
infrastructure, a static counter with an eviction strategy, and an
environment-variable configuration workaround, for a control that would not be
a genuine guarantee: in-memory counters are per-node, so with more than one
Keycloak task the effective limit becomes the configured limit multiplied by
the task count.

**2. Enforce a per-service-point quota on live credentials instead.**

The material risk for this feature is not request rate, it is realm pollution.
An authenticated admin, or more likely a runaway script, could create thousands
of clients and degrade the realm. A quota addresses that directly.

* Cap the number of active, non-revoked credentials per service point at
  **10**.
* Enforce the check in application code, alongside the per-service-point
  isolation checks that RAID-827 already identifies as the real security
  boundary. This needs no new infrastructure and is deterministic, so it is
  straightforward to cover in the RAID-848 integration suite.
* Reject a create that would exceed the cap with **409 Conflict** and an error
  body naming the limit. Revoking a credential frees a slot.

This lands in RAID-846 (endpoint implementation) with test coverage in
RAID-848.

**3. Edge rate limiting and realm brute-force protection are separate work.**

Both are real gaps, but neither belongs inside RAID-827, and the second is
arguably the larger exposure. Each gets its own ticket.


# Alternatives considered

* **Per-request rate limiting in a new JAX-RS `ContainerRequestFilter`:**
  rejected for this story. It is the textbook answer, but here it buys little
  against an authenticated and scoped surface, while requiring infrastructure
  the module does not have. Revisit if these endpoints are ever exposed to a
  broader caller set, or once a shared limiter exists that the SPI can use.

* **Returning 429 for quota exhaustion:** rejected. The quota is a standing
  limit on stored objects, not a rate. Overloading 429 would make it impossible
  for a caller to distinguish "slow down and retry" from "delete something
  first", and would leave no clean status code if a real rate limit is added
  later.

* **Relying on Keycloak brute-force protection:** rejected as insufficient
  rather than wrong. It is disabled today, and even when enabled it does not
  cover custom SPI resources. Enabling it is still worth doing, for the token
  endpoint rather than for these endpoints, which is why it becomes its own
  ticket.

* **A WAFv2 rate-based rule as the whole answer:** rejected as the answer to
  this question, though it should be built. It is coarse, operating on source
  IP rather than on service point identity, so it cannot express "this admin
  has created enough clients". It also carries a deployment sharp edge: the
  WebACL would attach to the regional ALB, and branch environments share both
  the test listener and the single Keycloak instance, so one WebACL would cover
  the test environment and every branch environment at once. Useful as a blunt
  outer layer, not as tenant-aware protection.

* **A global cap on clients per realm:** rejected. It would let one noisy
  service point exhaust the allowance for everyone, which is the opposite of
  the per-tenant isolation this feature is built around.


# Consequences

* RAID-846 gains the quota check and the 409 response. RAID-848 gains positive
  and negative coverage for it, including that revoking frees a slot.
* The cap of 10 is a starting value chosen without production evidence, since
  no comparable feature exists yet to measure. It is a constant in application
  code, so raising it is a small change. Expect to revisit it once real usage
  exists.
* These endpoints remain without per-request rate limiting. That is an accepted
  risk, recorded here rather than solved, and it sits alongside the residual
  risk RAID-827 already accepts (the broker's `manage-clients` role is
  realm-wide, with isolation enforced only in application code).
* Two follow-up tickets are raised: a WAFv2 WebACL with a rate-based rule for
  the ALB, and enabling realm brute-force protection. The second matters
  because `client_credentials` token attempts against the very credentials this
  feature mints are currently unthrottled at Keycloak's token endpoint.
* One item is unverified and should be confirmed before any future in-memory
  rate limiting is attempted: the Keycloak ECS service's task count. The
  per-node counter problem described above only bites when more than one task
  runs.
* Note the existing Keycloak version skew, unchanged by this decision: the SPI
  jar is compiled against 26.5.6 (`gradle/libs.versions.toml`) and runs on
  26.6.2 (`iam/Dockerfile`).
