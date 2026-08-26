# RAID-849: Rate limiting decision for scoped client credential endpoints

- Ticket: [RAID-849](https://ardc.atlassian.net/browse/RAID-849) (parent
  [RAID-827](https://ardc.atlassian.net/browse/RAID-827), epic
  [RAID-711](https://ardc.atlassian.net/browse/RAID-711))
- Sibling sub-tasks: [RAID-845](https://ardc.atlassian.net/browse/RAID-845),
  [RAID-846](https://ardc.atlassian.net/browse/RAID-846),
  [RAID-847](https://ardc.atlassian.net/browse/RAID-847),
  [RAID-848](https://ardc.atlassian.net/browse/RAID-848)
- Follow-up tickets raised: [RAID-850](https://ardc.atlassian.net/browse/RAID-850),
  [RAID-851](https://ardc.atlassian.net/browse/RAID-851)
- Source investigation: [RAID-767](https://ardc.atlassian.net/browse/RAID-767)
- PR: [au-research/raid-au#624](https://github.com/au-research/raid-au/pull/624)
- ADR: `doc/adr/2026-08-26_scoped-client-credential-quota.md`

## What changed and why

RAID-827 adds Keycloak SPI endpoints letting a Service Point Admin self-serve
their own service point's API client credentials (create, list, rotate, revoke,
get-secret) through a broker, since a Service Point Admin's own token carries
none of the `realm-management` permissions needed to create a Keycloak client.
The story deliberately left rate limiting undecided. RAID-849 is the spike that
decided it.

This is a documentation-only change: an ADR recording the decision. No code or
configuration was touched. The implementation consequences land in RAID-846 and
RAID-848, whose descriptions were updated accordingly.

### What the investigation found

- **Nothing throttles anything today.** No rate limiting, throttling or
  backpressure in `api-svc/` or `iam/`: no bucket4j, resilience4j, Guava
  `RateLimiter`, and no request-counting filter. The only throttle code in the
  repo is outbound client-side backoff in build tooling
  (`buildSrc/.../Utils.java`) for polling the ARDC SPARQL vocabulary endpoint.
- **No edge layer to lean on.** `raido-v2-aws-private` has no WAFv2 WebACL, no
  rate-based rules, no API Gateway throttling or usage plans, and no CloudFront
  or ALB request limiting.
- **Keycloak is ALB-direct.** `iam.<zone>`, and therefore every custom SPI
  endpoint, is reached straight through the internet-facing ALB, whose only
  listener filtering is host-header matching. CloudFront fronts the SPA and
  proxies some `api.<zone>` paths, but it does not front `iam.<zone>`, so there
  is no existing hop where a WebACL could attach.
- **The `iam` module has no filter infrastructure**: no
  `ContainerRequestFilter`, no `ContainerResponseFilter`, no servlet filters,
  only `@Provider`-annotated JAX-RS resources.
- **There is no working per-endpoint SPI config mechanism.** All three
  `RealmResourceProviderFactory` implementations have empty
  `init(Config.Scope)` bodies, and `getResource()` builds a fresh controller
  per request, so `Config.Scope` never reaches the resource. `GroupController`
  works around this by reading `System.getenv("RAID_FLAT_GROUP_ADMIN_FALLBACK")`.
- **Keycloak brute-force protection is disabled**: `bruteForceProtected: false`
  in `iam/realms/raid-realm.json` (line 38), making the accompanying
  `failureFactor` and wait settings inert. It never covered custom SPI
  resources anyway, only login and credential flows.

### The decision

**No per-request rate limiting on the RAID-827 endpoints.** They are all
authenticated and scoped to a single service point, so there is nothing to
guess: a caller either holds `service-point-admin:<groupId>` or does not, and
`get-secret` is not an enumeration target. Building it would require new filter
infrastructure, a static counter with an eviction strategy, and an
environment-variable configuration workaround, for a control that would not be
a genuine guarantee anyway, because in-memory counters are per-node.

**A per-service-point quota on live credentials instead.** The material risk is
realm pollution from runaway credential creation, not request rate. Cap active
non-revoked credentials per service point at 10, enforce it in application code
beside the isolation checks that RAID-827 already identifies as the real
security boundary, and reject an excess create with 409 Conflict naming the
limit. Revoking frees a slot. 409 rather than 429 because the quota is a
standing limit on stored objects, not a rate, and overloading 429 would leave
no clean status code if a real rate limit is added later.

The cap of 10 is a starting value chosen without production evidence, since no
comparable feature exists yet to measure. It is a code constant, so it is cheap
to revisit.

### Alternatives rejected

Per-request rate limiting in a new `ContainerRequestFilter` (little value
against an authenticated, scoped surface for significant new infrastructure);
429 for quota exhaustion; relying on Keycloak brute-force protection
(disabled, and does not cover SPI resources); a WAFv2 rate-based rule as the
whole answer (coarse and source-IP based, so it cannot express "this admin has
created enough clients"); and a global per-realm cap (one noisy service point
could exhaust everyone's allowance, the opposite of the per-tenant isolation
this feature is built around).

## Impact on sibling sub-tasks

- **RAID-846** gains the quota check and the 409 response.
- **RAID-848** gains positive and negative coverage: creating up to the cap
  succeeds, the create that would exceed it returns 409, and revoking frees a
  slot so a subsequent create succeeds.

## Follow-up work raised

Two real gaps were found that do not belong inside RAID-827, so each became its
own ticket rather than scope creep:

- **RAID-850** — add a WAFv2 WebACL with a rate-based rule in front of the ALB.
  Noted sharp edge: the WebACL attaches to the regional ALB, and branch
  environments share both the test listener and the single Keycloak instance,
  so one WebACL would cover the test environment and every branch at once.
- **RAID-851** (High) — enable Keycloak realm brute-force protection. This is
  arguably the larger real-world exposure: `client_credentials` token attempts
  against the very credentials RAID-827 mints are currently unthrottled at
  Keycloak's token endpoint. Note the realm config is duplicated, with the
  authority environments' copy in raid-au and the agency deployment shipping
  its own in raido-v2-aws-private, also with brute-force disabled.

## Left unverified

The Keycloak ECS service's task count was not confirmed. It only matters if
in-memory rate limiting is ever attempted, since the per-node counter problem
described above bites only when more than one task runs.
