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

- **`Api.java`** — the shared `RestTemplate` now uses a
  `SimpleClientHttpRequestFactory` with bounded connect (5s) and read (10s)
  timeouts, configurable via `raid.rest-template.connect-timeout` and
  `raid.rest-template.read-timeout`. Defaults are baked into the `@Value`
  bindings so a missing property cannot break bean construction. The existing
  JAXB-converter-first ordering (relied on by the ISNI XML client) is preserved.
- **`AbstractUriValidator.java`** — a broad `catch (RestClientException e)` was
  added after the existing `HttpClientErrorException` block. It covers
  `ResourceAccessException` (timeout / DNS / connection refused),
  `HttpServerErrorException` (5xx) and, defensively, any other
  `RestClientException` subtype, returning the existing `SERVER_ERROR`
  (`"uri could not be validated - server error"`) validation failure. The
  404 -> `URI_DOES_NOT_EXIST` special-case is unchanged. Duplicated failure
  construction was pulled into a small `serverError(fieldId)` helper.
- **`application.yaml`** — documents the new `raid.rest-template.*` timeout block.
- **`AbstractUriValidatorTest.java`** — new unit tests for the timeout,
  connection-refused, 5xx and generic `RestClientException` paths, each
  asserting a clean `SERVER_ERROR` failure (no exception thrown).

## Coverage

All five external-resolution validators — `DoiService`, `OrcidService`,
`RorService`, `OpenStreetMapUriValidator`, `GeoNamesUriValidator` — extend
`AbstractUriValidator` and share the single `RestTemplate` bean, so both fixes
apply to them automatically. The ARK/Handle/RRID validators from the RAID-699
follow-up do not exist yet (only a spike doc,
`20260710-raid-699-identifier-validator-spike.md`); they will inherit this
hardening when built.

## Design note / known consideration

The timeout is applied to the **shared** `RestTemplate` bean, as the ticket
specifies. That bean is also injected into `DataciteService`,
`LegacyRaidService`, `RaidUpgradeService`, `KeycloakLogoutHandler` and the
ROR/ORCID/ISNI clients, so the 5s/10s bound now applies process-wide where those
callers previously relied on OS-level (effectively unbounded) timeouts. This is
intentional within the ticket scope. The read timeout is per-inactivity
(`SO_TIMEOUT`), not total request time, so a slow-but-streaming response is not
tripped. If any of those integrations (notably a DataCite mint/deposit) needs a
longer bound, a follow-up could either raise `raid.rest-template.read-timeout`
or move the validators onto a dedicated, tighter-bounded `RestTemplate`.

## Testing

- `./gradlew :api-svc:raid-api:test` — full unit suite green, including the four
  new failure-mode tests.

## Acceptance criteria

- [x] External resolver timeout / DNS failure / 5xx / connection-refused during
  URI validation produces a validation failure, not an HTTP 500.
- [x] Unit tests cover each failure mode.
- [x] RestTemplate timeout is bounded and configurable.
