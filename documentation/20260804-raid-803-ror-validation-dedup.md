# RAID-803: De-duplicate ROR validation onto a single hardened path

**Date:** 2026-08-04
**JIRA:** [RAID-803](https://ardc.atlassian.net/browse/RAID-803) (parent epic [RAID-789](https://ardc.atlassian.net/browse/RAID-789); depends on [RAID-802](https://ardc.atlassian.net/browse/RAID-802), scoped by spike [RAID-785](https://ardc.atlassian.net/browse/RAID-785))
**PR:** [au-research/raid-au#593](https://github.com/au-research/raid-au/pull/593)
**Branch:** `feature/RAID-803` (off `origin/main`)

## What changed and why

ROR validation had three overlapping classes. `RorValidator` (and its
collaborator `RorService`, extending `AbstractUriValidator`) was dead code
with no production callers. The live path, `OrganisationValidator ->
RorClient.exists()`, was the least graceful: `RorClient.exists()` maps 404
to false but rethrows any non-404, and `OrganisationValidator` had no
try/catch, so a ROR resolver outage surfaced as an HTTP 500.

This work collapses ROR validation onto the single live path and hardens it.

### Changes

- **Removed the dead ROR path:** `RorValidator`, `RorService`,
  `RorServiceStub`, the `rorService(...)` bean in `ExternalPidService`, and
  the now-orphaned `raid.stub.ror.*` config plus its stub constants. The
  sole ROR validation path is now `OrganisationValidator -> RorClient`.
- **Hardened `OrganisationValidator`:** `rorClient.exists()` is wrapped in a
  `try/catch (RestClientException)`. A non-404 outage (5xx, timeout,
  connection refused, DNS failure) now yields a structured server-error
  validation failure that mirrors `AbstractUriValidator.serverError()`,
  instead of propagating as a 500. Existing behaviour preserved: 404 ->
  "This ROR does not exist" (NOT_FOUND), duplicate-org detection, schemaUri
  checks. The per-organisation guard does not abort validation of other
  organisations in the request.
- **Bound `RorClient` to the bounded `uriValidatorRestTemplate`** (5s connect
  / 10s read, from RAID-802) instead of the `@Primary` unbounded template, so
  a hanging ROR resolver times out rather than hanging the request.
- **Hardened the DataCite mint path:** binding `RorClient` to a bounded
  template means a slow ROR lookup during minting now throws a
  `RestClientException`. A new `@ExceptionHandler(RestClientException.class)`
  in `RaidExceptionHandler` returns a structured **502** (upstream dependency
  unavailable) instead of the previous empty-body 500. The 422 mint-retry
  path in `RaidService.mintHandle` is unaffected (it catches
  `HttpClientErrorException` locally before the advice sees it).
- **Hardened the shared error-response contract:** the catch-all
  (`defaultExceptionHandler`) and `dataAccessExceptionHandler` now return
  structured `FailureResponse` bodies with generic details (no empty-body
  500s; no DB/SQL internals leaked).

## Scope note

The error-response hardening (mint-path 502, structured catch-all bodies) was
bundled into this ticket because binding `RorClient` to the bounded template
introduced a mint-time ROR timeout path. An earlier "reported as CORS"
concern does **not** apply at the application layer: error responses already
carry CORS headers via Spring Security's `CorsFilter`, and the JWT-header-size
CORS case was resolved separately by RAID-617 removing the `admin_raids`
claim. These changes improve error-body structure and status accuracy, not
CORS handling.

## Acceptance criteria (all met)

- A single ROR validation path.
- A ROR resolver outage yields a clean validation failure, not an HTTP 500.
- Existing ROR validation behaviour and tests preserved.
- Unused `RorValidator` removed.

## Testing

- Unit build (`./gradlew :api-svc:raid-api:build -x intTest`): 786 tests,
  0 failures.
- Integration suite (`./gradlew intTest`): 181 tests, 0 failures, 16
  pre-existing `@Disabled` skips. No ISNI live-call flake this run.
- One stale integration assertion updated to the new contract:
  `DataciteErrorIntegrationTest` (DataCite 429 during mint) now asserts the
  structured 502 instead of the old opaque 500.
