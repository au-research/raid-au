# RAID-809: 503 for unavailable identifier resolvers

Date: 2026-08-10

## What changed and why

While verifying RAID-809 on the stage environment (with the in-memory DOI stub
temporarily disabled to measure real-resolver mint latency), mints failed with an
unhandled HTTP 500 — but on contributor **ORCID** validation, not on DOI. Root
cause: resolver-backed validators did not distinguish a resolver being
*unavailable* from an identifier being *not found*, and the ORCID/ISNI contributor
path (`ContributorTypeValidator`) called the resolver unguarded, so any error
propagated as an unhandled 500/502.

This change makes the distinction consistent across every resolver-backed validator
(DOI, Handle, RRID, ROR, GeoNames, OpenStreetMap, ORCID, ISNI):

- A clean `404` from a resolver keeps the existing `ValidationFailure` → HTTP 400.
- A resolver being unavailable (`RestClientException`: 5xx, timeout/DNS via
  `ResourceAccessException`, or a non-404 client error such as the ORCID member-API
  401/403) now raises a new `ResolverUnavailableException`, mapped to **HTTP 503**
  with a structured RFC 7807 body (`type = https://raid.org.au/errors#ResolverUnavailable`)
  listing each affected `field`, submitted `value`, `resolver`, `downstreamStatus`,
  and a short sanitised `downstreamMessage`, plus a `Retry-After` header.
- `ValidationService` aggregates unavailable entries across the parallel validator
  futures; **503 takes precedence** over a 400 validation failure and over an
  unexpected runtime exception, because the caller's correct action is to retry.
- Downstream response bodies are never echoed into the API response.

The change is validation-layer robustness only. It does not alter which resolvers
are stubbed; that binding fix and the non-prod ORCID endpoint correction are tracked
separately.

## Testing

`./gradlew :api-svc:raid-api:test` — 808 tests, 0 failures. New/updated coverage in
`AbstractUriValidatorTest`, `OrganisationValidatorTest`, `ContributorTypeValidatorTest`,
`GeoNamesUriValidatorTest`, `ValidationServiceTest`, `RaidExceptionHandlerTest`
(asserting field/value/resolver/downstreamStatus, 503-precedence-over-400, and no
downstream-body leakage). `openApiGenerate` (`validateSpec=true`) passed.

## Reviews

- api-code-reviewer: approved, no blocking issues. Non-blocking note: resolver
  exceptions are still logged as the exception object, which on Spring 6.2.12 may
  include a truncated downstream body preview in server logs only (matching
  pre-existing RAID-803 practice); can be tightened later if strict log hygiene is
  wanted.

## Links

- Parent bug: [RAID-809](https://ardc.atlassian.net/browse/RAID-809)
- Sub-task (CDK): [RAID-810](https://ardc.atlassian.net/browse/RAID-810) — correct
  non-prod ORCID existence-check endpoint/token (member → public API)
- Sub-task (frontend): [RAID-811](https://ardc.atlassian.net/browse/RAID-811) —
  agency app to handle the 503 as retryable
- PR: https://github.com/au-research/raid-au/pull/606
- ADR: `doc/adr/2026-08-10_resolver-unavailable-503.md`
