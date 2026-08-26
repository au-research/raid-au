### Cap client credentials per service point, defer rate limiting

* Status: final
* Who: proposed and finalised by RL
* When: 2026-08-26
* Related: RAID-711 (epic), RAID-827 (story), RAID-849 (this spike),
  RAID-767 (source investigation), RAID-714 (RAID-827 was split from it),
  RAID-851 (realm brute-force protection)


# Context

RAID-827 adds Keycloak SPI endpoints that let a Service Point Admin self-serve
their own service point's API client credentials: create, list, rotate, revoke
and get-secret. It listed as an open task "decide and document rate-limiting
behaviour for these endpoints". This ADR is that decision.

Two facts from the investigation frame it.

**Nothing throttles anything today.** There is no rate limiting, throttling or
backpressure in `api-svc/` or `iam/`: no bucket4j, resilience4j, Guava
`RateLimiter`, and no request-counting filter. Keycloak's own brute-force
protection is disabled (`bruteForceProtected: false` in
`iam/realms/raid-realm.json`), and in any case it only covers login and
credential flows, never custom SPI resources.

**Building it in the SPI is not cheap.** The `iam` module has no filter
infrastructure at all: no `ContainerRequestFilter`, no
`ContainerResponseFilter`, no servlet filters, only `@Provider`-annotated
JAX-RS resources. There is also no working per-endpoint SPI configuration
mechanism: every `RealmResourceProviderFactory` has an empty
`init(Config.Scope)` body and `getResource()` builds a fresh controller per
request, so `Config.Scope` never reaches the resource and `GroupController`
resorts to reading `System.getenv("RAID_FLAT_GROUP_ADMIN_FALLBACK")`.
Controller-per-request also means any counter must be `static`, bringing its own
eviction design.

So a rate limiter here is new cross-cutting infrastructure, a configuration
workaround, and a state-lifecycle problem, built to address a need that has not
yet been demonstrated.


# Deployment portability

This constraint shapes everything below and is easy to miss.

**Most RAiD registration agencies are not deployed on AWS.** Only controls that
live in the application and its shipped configuration travel to every
deployment. Anything implemented in the AU account's infrastructure protects
the AU deployment and nothing else.

Concretely, for the AU deployment there is no WAFv2 WebACL anywhere in
`raido-v2-aws-private`, no rate-based rules, no API Gateway throttling, and no
CloudFront or ALB request limiting. `iam.<zone>` is reached directly through the
internet-facing ALB, with no CloudFront hop where a WebACL could attach (unlike
some `api.<zone>` paths, which CloudFront does proxy). That gap could be closed
for AU. It cannot be closed for anyone else, so **the RAiD software must not
depend on an edge layer existing**, and edge protection must not be presented
as RAiD's answer to this question.


# Decision

**1. Defer rate limiting until there is a demonstrated need.**

Not because the risk is zero, but because the cost is real and the need is
unproven. Push back on building it until evidence says otherwise. The endpoints
are all authenticated and scoped to a single service point, so there is nothing
to guess: a caller either holds `service-point-admin:<groupId>` or does not,
and `get-secret` is not an enumeration target.

**2. Cap the number of credentials per service point instead.**

Capping stored objects is markedly cheaper than limiting request rate, and it
is portable: it is a count check in application code, so it works identically
in every deployment regardless of hosting.

* Cap active, non-revoked credentials per service point at **10**.
* Enforce it in application code alongside the per-service-point isolation
  checks that RAID-827 already identifies as the real security boundary.
* Reject a create that would exceed the cap with **409 Conflict** and an error
  body naming the limit. Revoking a credential frees a slot.

This lands in RAID-846, with coverage in RAID-848.

**3. Do not pursue edge rate limiting as part of this.**

An AU-only WAF rule is rate limiting, for the same unproven need, benefiting
one deployment. It fails the same test as decision 1 and is not being taken
forward. Recorded here so a future reader does not mistake its absence for an
oversight.

**4. Enable Keycloak realm brute-force protection (RAID-851).**

This is the exception that survives, because the implementation cost is
approximately zero: the settings already exist in the realm JSON and are simply
switched off. It is also portable, since realm configuration ships with the
software rather than living in one cloud account. It matters because
`client_credentials` token attempts against the very credentials this feature
mints are currently unthrottled at Keycloak's token endpoint, which is a larger
real exposure than the management endpoints themselves.


# What would constitute demonstrated need

Recorded so this deferral stays reviewable rather than simply forgotten.
Revisit decision 1 if any of the following appears:

* Credentials repeatedly hitting the cap at a rate suggesting automation rather
  than intent, which would show the cap is absorbing abuse rather than
  preventing sprawl.
* Rotate or get-secret call volume far above what human self-service explains.
* An agency operator reporting load or abuse on these endpoints.
* The endpoints being opened to a broader or less-trusted caller set than
  Service Point Admins.

The instrument for the first three already exists in this story: the RAID-847
audit records (who, when, which credential) are the telemetry that would show
it. No separate monitoring work is needed to keep this decision reviewable.


# Alternatives considered

* **Per-request rate limiting in a new JAX-RS `ContainerRequestFilter`:**
  deferred, per decision 1. This is the textbook answer, and the objection is
  cost against unproven need rather than a claim that it would not work.

* **Returning 429 for cap exhaustion:** rejected. The cap is a standing limit
  on stored objects, not a rate. Overloading 429 would stop a caller
  distinguishing "slow down and retry" from "delete something first", and would
  leave no clean status code if a real rate limit is added later.

* **A WAFv2 rate-based rule:** rejected, per decision 3. Beyond the portability
  problem, it is coarse: it aggregates on source IP and cannot parse a JWT, so
  it cannot see `service_point_group_id` and cannot express "this admin has
  created enough clients". It also carries a deployment sharp edge for AU,
  since the WebACL attaches to the whole regional ALB and branch environments
  share both the test listener and the single Keycloak instance, so one WebACL
  would cover the test environment and every branch at once.

* **A global cap on clients per realm:** rejected. One noisy service point
  could exhaust the allowance for everyone, the opposite of the per-tenant
  isolation this feature is built around.


# Consequences

* RAID-846 gains the cap check and the 409 response. RAID-848 gains positive
  and negative coverage, including that revoking frees a slot.
* The cap of 10 is a starting value chosen without production evidence, since
  no comparable feature exists yet to measure. It is a constant in application
  code, so raising it is a small change. Expect to revisit it once real usage
  exists.
* These endpoints remain without per-request rate limiting, in every
  deployment. That is an accepted and deliberate deferral, recorded alongside
  the residual risk RAID-827 already accepts (the broker's `manage-clients`
  role is realm-wide, with isolation enforced only in application code).
* Agency operators deploying outside AWS receive no edge protection from us and
  are responsible for their own. This should be stated in deployment
  documentation rather than left implicit.
* One item is unverified, and would need establishing before any future rate
  limiting is attempted: deployment topology and instance counts, both for AU
  and for agency deployments. A `static` in-memory counter is sound on a single
  Keycloak instance and only becomes per-node theatre across several, so the
  right implementation depends on facts not yet gathered.
