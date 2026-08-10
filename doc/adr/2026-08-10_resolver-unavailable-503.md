# Distinguish resolver-unavailable (503) from identifier-not-found (400) in validation

- Status: accepted
- Date: 2026-08-10
- Ticket: RAID-809

## Context

RAiD validates external identifiers at mint/update time by calling their resolvers
(DOI, Handle, RRID, ROR, GeoNames, OpenStreetMap, ORCID, ISNI). Historically every
non-success from a resolver was collapsed into a single outcome: some validators
returned a generic `SERVER_ERROR` validation failure (surfacing as HTTP 400), and
the contributor ORCID/ISNI path did not guard the call at all, so a resolver error
propagated as an unhandled 500/502.

This conflates two very different situations:

- The resolver answered and the identifier does not exist (a clean 404). This is a
  genuine problem with the caller's input.
- The resolver could not be reached or returned an error (5xx, timeout, or a non-404
  client error such as the 401/403 returned when the ORCID member API is called
  without OAuth). This is a transient, server-side condition; the caller's input may
  be perfectly valid.

Testing on stage (with the in-memory DOI stub temporarily disabled) surfaced the
second case: a transient ORCID member-API error failed the entire mint as an
unhandled 500, with no signal that the correct action was to retry. Because
validation runs across parallel futures, a request can also mix a genuine
not-found failure with an unavailable resolver.

## Decision

Split the resolver failure taxonomy across all resolver-backed validators:

- Clean 404 from a resolver keeps the existing `ValidationFailure` -> HTTP 400.
  Unchanged.
- Resolver unavailable (`RestClientException`: 5xx, timeout/DNS via
  `ResourceAccessException`, and non-404 `HttpClientErrorException`) is recorded as
  an `UnavailableResolver` entry and raised as a new `ResolverUnavailableException`,
  mapped to **HTTP 503** with a structured RFC 7807 body
  (`type = https://raid.org.au/errors#ResolverUnavailable`) that lists each affected
  `field`, submitted `value`, `resolver` name, `downstreamStatus`, and a short
  sanitised `downstreamMessage`, plus a `Retry-After` header.

`ValidationService` aggregates unavailable entries across the parallel validator
futures. **503 takes precedence** over both a 400 validation failure and an
unexpected non-resolver runtime exception in the same request, because when a
resolver is unavailable the caller cannot know whether their input is valid, and
the correct action is to retry the whole request.

The downstream response body is never echoed into the API response. The one place
that builds an `UnavailableResolver` (`ResolverUnavailableException.toUnavailableResolver`)
records only method, target, and status.

## Consequences

- External resolver outages now produce a clear, retryable 503 that names the
  offending identifier(s), instead of an opaque 500/502 or a misleading 400.
- Consistent behaviour across every resolver-backed validator.
- On a mixed request, genuine 400 validation failures are withheld until the
  unavailable resolver recovers; the caller sees them on retry. This is an accepted
  trade-off of the 503-precedence rule.
- The API contract gains a 503 response on mint/update/patch (additive; documented
  in the OpenAPI spec). Clients, including the agency app, should treat it as
  temporary rather than as a validation error (tracked in RAID-811).
- This is validation-layer robustness only. It does not change which resolvers are
  stubbed; the underlying stub-binding config fix and the non-prod ORCID endpoint
  correction are tracked separately (RAID-809 config work and RAID-810).
