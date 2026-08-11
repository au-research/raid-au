# RAID-809: resolver-failure taxonomy (400 vs 503) + non-prod stubbing of ORCID/ISNI/ROR

Date: 2026-08-10

## What changed and why

While verifying RAID-809 on stage (DOI stub temporarily disabled to measure mint
latency), mints failed with an unhandled 500 on **contributor ORCID** validation,
not DOI. Investigation found two problems, both addressed here:

1. **Resolver failures were not classified by cause.** Validators collapsed failures
   into a 400, and the ORCID/ISNI contributor path was unguarded, so a resolver
   error surfaced as an unhandled 500/502.
2. **ORCID/ISNI/ROR had no working stub**, so even the test environment validated
   them against live services. A transient ORCID sandbox 500 in CI cascaded across
   91/189 integration tests plus 3 e2e specs.

### The change

- **Failure taxonomy:** client-resolvable problems (bad/missing input, or a resolver
  cleanly returning 404 "not found") return **400**; a downstream resolver being
  unavailable (5xx / timeout / non-404 client error) returns **503** via a new
  `ResolverUnavailableException` mapped to a structured `ResolverUnavailableResponse`
  (`type=https://raid.org.au/errors#ResolverUnavailable`, listing
  field/value/resolver/downstreamStatus) plus a `Retry-After` header. A genuine
  non-resolver server exception still returns 500.
- **Precedence (cause-based):** a `ValidationResult(failures, unavailableResolvers)`
  is carried out of the four top-level validators (`OrganisationValidator`,
  `ContributorValidator`, `RelatedObjectValidator`, `SpatialCoverageValidator`),
  which catch-collect-continue instead of throw-and-lose. `ValidationService` then
  returns **400 with all client-resolvable failures if any exist**, and **503 only
  when an unavailable resolver is the sole blocker**. A 400 is never masked by a 503.
  Downstream response bodies are never echoed to the client.
- **Stub ORCID/ISNI/ROR for non-prod, trap inverted:** `OrcidClient`/`RorClient`
  become stub-aware `@Bean @Primary` factories (mirroring `IsniClient`), gated on
  `raid.stub.{orcid,isni,ror}.enabled`. **Real is the default** (prod/stage/demo,
  zero env-var dependency); non-prod opts in (intTest config; deployed test/branch
  via `RAID_STUB_*_ENABLED=true`, names that actually bind). A mis-set flag fails
  loud rather than silently stubbing prod. Dead `OrcidService`/`OrcidServiceStub`
  removed.

## Scope and follow-ups

- This closes the stub-binding trap only for **ORCID/ISNI/ROR**. DOI/Handle/RRID/
  GeoNames/OpenStreetMap still default to `enabled: true` and remain subject to the
  original non-binding-disable trap; flipping those to real in prod is a deliberate,
  stage-first follow-up (backed by the 503 hardening added here).
- Deployed test/branch envs need the `RAID_STUB_*_ENABLED=true` env vars set in CDK
  for the stubs to take effect there (also required for the branch pipeline to be
  deterministic).
- [RAID-810](https://ardc.atlassian.net/browse/RAID-810): make the deployed-env ORCID
  existence check reliable (member → public API).
- [RAID-811](https://ardc.atlassian.net/browse/RAID-811): agency app to handle 503 as
  retryable.

## Testing

`./gradlew build -x intTest` — **831 tests, 0 failures**. Coverage includes: the
cause-based precedence (400-wins, 503-sole-blocker, merged unavailable,
non-resolver-exception precedence, sibling-not-aborted); the stubs
(`OrcidClientStubTest`, `RorClientStubTest`); and an `ExternalPidServiceTest` wiring
guard asserting `enabled=true` → stub and `false`/absent → real client (the
regression guard against the binding trap). Integration tests run in CI (Docker).

Note: the stub error sentinels throw a plain `RuntimeException`, so the 503 path is
unit-tested but not reproducible through the stubs in intTest.

## Reviews

- api-code-reviewer (v1 and v3): approved, no blocking issues.

## Links

- Parent bug: [RAID-809](https://ardc.atlassian.net/browse/RAID-809)
- [RAID-810](https://ardc.atlassian.net/browse/RAID-810), [RAID-811](https://ardc.atlassian.net/browse/RAID-811)
- PR: https://github.com/au-research/raid-au/pull/606
- ADR: `doc/adr/2026-08-10_resolver-unavailable-503.md`
