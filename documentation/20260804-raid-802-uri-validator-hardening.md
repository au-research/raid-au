# RAID-802: Harden AbstractUriValidator and bound RestTemplate timeouts

- **JIRA (Story):** [RAID-802](https://ardc.atlassian.net/browse/RAID-802)
- **Parent epic:** [RAID-789](https://ardc.atlassian.net/browse/RAID-789) — Identifier & relatedObject validator hardening and coverage (RAID-699 follow-up)
- **Scoped by spike:** RAID-785
- **PR:** [au-research/raid-au#591](https://github.com/au-research/raid-au/pull/591)
- **Sub-tasks:** none

## What changed and why

External URI validation performs a `HEAD` request against the resolver
(`doi.org`, `orcid.org`, `ror.org`, OpenStreetMap, GeoNames). Two gaps meant a
misbehaving resolver degraded the whole request instead of producing a clean
validation result:

1. `AbstractUriValidator.validate()` only caught `HttpClientErrorException`.
   Connect/read timeouts, DNS failures, connection-refused and 5xx responses
   propagated uncaught and surfaced to the caller as an **HTTP 500** rather than
   a validation failure.
2. The shared `RestTemplate` bean (`Api.java`) had **no connect or read
   timeout**, so an unreachable or hanging resolver could block the request
   indefinitely.

### Changes

- **`Api.java`** — introduces a **dedicated** `uriValidatorRestTemplate` bean
  backed by a `SimpleClientHttpRequestFactory` with bounded connect (5s) and
  read (10s) timeouts, configurable via `raid.uri-validation.connect-timeout` /
  `raid.uri-validation.read-timeout` (defaults baked into the `@Value` bindings
  so a missing property cannot break bean construction). The shared, general
  `restTemplate` bean is left **unbounded** (as before) and marked `@Primary` so
  all existing by-type injections keep resolving to it; its JAXB-converter-first
  ordering (relied on by the ISNI XML client) is unchanged.
- **`ExternalPidService.java`** — the four external URI validators built here
  (`RorService`, `DoiService`, `GeoNamesUriValidator`,
  `OpenStreetMapUriValidator`) now inject the bounded template via
  `@Qualifier("uriValidatorRestTemplate")`. Everything else (DataCite, legacy
  RAiD, Keycloak logout, ISNI/ROR/ORCID clients) continues to use the unbounded
  shared bean.
- **`AbstractUriValidator.java`** — a broad `catch (RestClientException e)` was
  added after the existing `HttpClientErrorException` block. It covers
  `ResourceAccessException` (timeout / DNS / connection refused),
  `HttpServerErrorException` (5xx) and, defensively, any other
  `RestClientException` subtype, returning the existing `SERVER_ERROR`
  (`"uri could not be validated - server error"`) validation failure. The
  404 -> `URI_DOES_NOT_EXIST` special-case is unchanged. Duplicated failure
  construction was pulled into a small `serverError(fieldId)` helper.
- **`application.yaml`** — documents the new `raid.uri-validation.*` timeout block.
- **`AbstractUriValidatorTest.java`** — new unit tests for the timeout,
  connection-refused, 5xx and generic `RestClientException` paths, each
  asserting a clean `SERVER_ERROR` failure (no exception thrown).

## Coverage

The four external-resolution validators built in `ExternalPidService`
(`RorService`, `DoiService`, `OpenStreetMapUriValidator`,
`GeoNamesUriValidator`) extend `AbstractUriValidator` and now use the bounded
template, so both fixes apply to them. `OrcidService` also extends
`AbstractUriValidator` but is **not currently wired** into the Spring context
(no `@Bean`/`@Component`/constructor call — only a dead import and the
`OrcidServiceStub` subclass), so ORCID URL validation does not run through it;
it will pick up the bounded template if/when it is wired. The ARK/Handle/RRID
validators from the RAID-699 follow-up do not exist yet (only a spike doc,
`20260710-raid-699-identifier-validator-spike.md`); they will inherit this
hardening when built.

## Design note: why a dedicated template (not the shared one)

The ticket originally called for bounding the **shared** `RestTemplate`. A first
pass did exactly that, and the branch pipeline
([Branch-Build-Deploy `f84d8d25`](https://ap-southeast-2.console.aws.amazon.com/codesuite/codepipeline/pipelines/Branch-Build-Deploy/executions/f84d8d25-5f7a-49e2-a5e0-e500fd616653/visualization?region=ap-southeast-2))
went red: every `POST /raid/` mint returned an empty-body HTTP 500. Root cause —
the shared bean is also used by `DataciteService.mint()`, which only catches
`HttpClientErrorException` (4xx). DataCite's test endpoint routinely takes longer
than the new 10s read bound, so a `ResourceAccessException` propagated uncaught
as a raw 500 on every mint. The read timeout is per-inactivity (`SO_TIMEOUT`),
not total request time, but DataCite test stalls exceeded even that.

Rather than bound the shared bean and add `ResourceAccessException` handling to
every non-validator caller, the timeout was scoped to a dedicated
`uriValidatorRestTemplate` used only by the URI validators — which is precisely
what RAID-802's "graceful external-resolver failure" is about. DataCite minting
and other outbound calls keep their prior (unbounded) behaviour, so there is no
blast radius outside URI validation.

## Testing

- `./gradlew :api-svc:raid-api:test` — full unit suite green, including the four
  new failure-mode tests.
- Local `dockerComposeUp bootRun` — the application context initialised all
  singleton beans (both `RestTemplate` beans plus the four qualified validators)
  successfully; the run only stopped at the final web-server bind because port
  8080 was already in use locally, confirming the DI wiring resolves.
- The branch pipeline re-run after this fix is the end-to-end gate (mint 500s
  resolved, `intTest` + `e2e` green).

## Acceptance criteria

- [x] External resolver timeout / DNS failure / 5xx / connection-refused during
  URI validation produces a validation failure, not an HTTP 500.
- [x] Unit tests cover each failure mode.
- [x] RestTemplate timeout is bounded and configurable.
