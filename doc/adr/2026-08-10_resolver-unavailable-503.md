# Resolver-failure taxonomy (400 vs 503) and non-prod stubbing of identifier resolvers

- Status: accepted
- Date: 2026-08-10
- Ticket: RAID-809

## Context

RAiD validates external identifiers at mint/update time by calling their resolvers
(DOI, Handle, RRID, GeoNames, OpenStreetMap via `ExternalPidService`; ORCID, ISNI,
ROR via the contributor/organisation client family). Two related problems surfaced:

1. Resolver failures were not classified by cause. Some validators collapsed any
   failure into a 400; the ORCID/ISNI contributor path was unguarded, so a resolver
   error propagated as an unhandled 500/502. A transient ORCID outage failed the
   entire mint with no signal that the correct action was to retry, and — worse —
   a resolver hiccup could hide a caller's genuine input errors.

2. The ORCID, ISNI and ROR existence checks had no working stub. Only the
   `ExternalPidService` URI validators (DOI/Handle/RRID/GeoNames/OpenStreetMap) could
   be stubbed. So even the test environment validated ORCID/ISNI/ROR against live
   services, making CI dependent on ORCID sandbox uptime. This was proven in CI: an
   ORCID sandbox 500 cascaded across 91/189 integration tests plus e2e.

Related to this is RAID-809's root bug: the CDK's stub-*disable* env vars used stale
names that never bound, so prod silently ran the in-memory stubs. Any new stub
toggle had to avoid repeating that trap.

## Decision

### Failure taxonomy — classify by cause
- A **client-resolvable** problem returns **400**: bad or missing input, or a
  resolver cleanly reporting that an identifier does **not exist** (404).
- A **downstream** problem (resolver unreachable / 5xx / timeout / non-404 client
  error) returns **503** via `ResolverUnavailableException`, with a structured
  RFC 7807 body (`type = https://raid.org.au/errors#ResolverUnavailable`) listing
  each affected `field`, `value`, `resolver`, `downstreamStatus`, and a short
  sanitised `downstreamMessage`, plus a `Retry-After` header.
- A genuine non-resolver exception (a server bug) still returns 500 and keeps the
  highest precedence.

### Precedence — client-resolvable errors win
When a request contains both a client-resolvable failure and an unavailable
resolver, return **400** with all the client-resolvable failures. Return **503**
only when an unavailable resolver is the **sole** blocker (zero client-resolvable
failures). A client-resolvable 400 is never masked by a 503. This is implemented
with a `ValidationResult(failures, unavailableResolvers)` carried out of the four
top-level validators (which catch-collect-continue rather than throw-and-lose), and
a decision in `ValidationService`: any failures → 400; else any unavailable → 503;
else success. The downstream response body is never echoed to the client.

### Stub ORCID/ISNI/ROR in non-prod — with the trap inverted
`OrcidClient`/`RorClient` become stub-aware `@Bean @Primary` factories (mirroring the
existing `IsniClient` pattern), selected by `raid.stub.{orcid,isni,ror}.enabled`.
Crucially, the safety is inverted relative to the old design: **real is the default;
stubs are opt-in.** In `application.yaml` these default to real (ORCID explicitly
`false`, ISNI/ROR absent), with zero env-var dependency, so prod/stage/demo are real
no matter what. Non-prod enables the stubs explicitly (intTest via
`src/intTest/resources/application.yaml`; deployed test/branch envs via
`RAID_STUB_ORCID_ENABLED=true` etc. — relaxed-binding names that actually bind,
unlike the stale `*.in-memory-stub` names that caused this bug). A mis-set or absent
flag therefore fails loud (a test env hitting live) and can never silently stub prod.
The dead `OrcidService`/`OrcidServiceStub` classes are removed.

## Consequences

- External resolver outages produce a clear, retryable 503 naming the offending
  identifier(s), and never hide a caller's fixable input errors.
- Non-prod (including CI) gets predictable, offline ORCID/ISNI/ROR responses; CI no
  longer depends on ORCID sandbox uptime.
- Prod/stage/demo continue to use the real ORCID/ISNI/ROR clients. Making those
  reliable (the ORCID member-vs-public API endpoint) is RAID-810.
- The API contract gains a 503 on mint/update/patch (additive; in the OpenAPI spec).
  Clients, including the agency app, should treat it as temporary (RAID-811).
- **Scope limit:** this closes the stub-binding trap only for ORCID/ISNI/ROR. The
  `ExternalPidService` resolvers (DOI, Handle, RRID, GeoNames, OpenStreetMap) still
  default to `enabled: true` and remain subject to the original non-binding-disable
  trap, so prod still validates those against in-memory stubs. Flipping those
  defaults to real is a deliberate, prod-behaviour-changing follow-up to be rolled
  out stage-first with monitoring, backed by the 503 hardening introduced here.
- The stub error sentinels throw a plain `RuntimeException`, not a
  `RestClientException`, so the 503 path is exercised by unit tests but not
  reproducible through the stubs in intTest.
