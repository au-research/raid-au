# RAID-787: Add RRID (SciCrunch) validator for relatedObject.schemaUri

- **JIRA (Story):** [RAID-787](https://ardc.atlassian.net/browse/RAID-787)
- **Parent epic:** [RAID-789](https://ardc.atlassian.net/browse/RAID-789) — Identifier & relatedObject validator hardening and coverage (RAID-699 follow-up)
- **Depends on:** [RAID-786](https://ardc.atlassian.net/browse/RAID-786) (dispatch-map refactor + stub pattern) and [RAID-802](https://ardc.atlassian.net/browse/RAID-802) (`AbstractUriValidator` hardening + bounded timeouts, inherited here)
- **PR:** [au-research/raid-au#603](https://github.com/au-research/raid-au/pull/603)
- **ADR:** `doc/adr/2026-08-06_related-object-scheme-validator-dispatch-map.md` (dispatch-map design; this ticket adds one scheme to it)

## What changed and why

`relatedObject.schemaUri` accepts a controlled set of resolver schemes. The enum
(`RelatedObjectSchemaUriEnum.HTTPS_SCICRUNCH_ORG_RESOLVER_`) and OpenAPI specs
already declared `https://scicrunch.org/resolver/`, but `RelatedObjectValidator`'s
dispatch map only wired DOI, Handle and Web Archive, so RRID-scoped related
objects were rejected outright. RAID-787 adds an RRID validator, wires it into
the dispatch map, and represents RRID as its own DataCite `relatedIdentifierType`.

Per Matthias's decision (2026-07-27), RRIDs are accepted and validated now, ahead
of public documentation catching up.

RRID format is `RRID:{Source}_{id}` (e.g. `RRID:AB_2298772`), stored/URL form
`https://scicrunch.org/resolver/RRID:AB_2298772`.

### Changes

- **`service/rrid/RridService.java`** (new) — extends `AbstractUriValidator`,
  mirroring `DoiService`/`HandleService`. Regex
  `^https://scicrunch\.org/resolver/RRID:[A-Za-z0-9]+_[A-Za-z0-9:._-]+$`
  (restricted to the observed RRID charset rather than `\S+` so stored ids can't
  carry braces/quotes/query-fragments into the resolver URL, DataCite metadata or
  the static site). Overrides the new `resolverUri()` hook (see below).
- **`validator/AbstractUriValidator.java`** — added a
  `protected String resolverUri(final String uri)` hook that defaults to returning
  the stored uri unchanged; the existence `HEAD` now targets `resolverUri(uri)`.
  DOI/Handle/ORCID/ROR keep the identity behaviour and are regression-tested for it.
- **`service/stub/RridServiceStub.java`** (new) + two sentinels
  (`NONEXISTENT_TEST_RRID`, `SERVER_ERROR_TEST_RRID`) in `InMemoryStubTestData` —
  lets integration tests exercise the resolver success/404/5xx paths without
  hitting live scicrunch.org.
- **`config/properties/StubProperties.java`** — new nested `Rrid` toggle; and
  `raid.stub.rrid: {enabled, delay}` added to both `src/main/resources` and
  `src/intTest/resources` `application.yaml`.
- **`config/bean/ExternalPidService.java`** — new `@Bean @Primary rridService(...)`
  returning the stub when `raid.stub.rrid.enabled`, else the real validator (using
  the `uriValidatorRestTemplate` with RAID-802 bounded timeouts).
- **`validator/RelatedObjectValidator.java`** — inject `RridService` and add one
  dispatch-map entry `scicrunch.org/resolver/ → rridService::validate`. The
  supported-scheme allow-list and "unsupported schemaUri" message derive from the
  map key set, so both update automatically.
- **`vocabularies/datacite/RelatedIdentifierType.java`** — added `RRID("RRID")`.
- **`factory/datacite/DataciteRelatedIdentifierFactory.java`** — remapped
  `HTTPS_SCICRUNCH_ORG_RESOLVER_` from `URL` to `RRID` in `IDENTIFIER_TYPE_MAP`.
- **No Flyway migration** — `V29__update_vocabularies.sql` already seeds
  `('https://scicrunch.org/resolver/', 'active')`, matching the enum exactly
  (unlike the Handle scheme, which needed the V43 typo fix).

## The `.json` resolver check (found by the live-resolver NFR)

The ticket's original scope was "regex + resolver `HEAD` (pattern A)" against the
stored URL. The live-resolver verification NFR proved this cannot work:

| URL | GET | HEAD |
|-----|-----|------|
| `https://scicrunch.org/resolver/RRID:AB_2298772` (real, stored form) | 403 | 403 |
| `https://scicrunch.org/resolver/RRID:AB_0000000` (bogus) | 403 | 403 |
| `https://scicrunch.org/resolver/RRID:AB_2298772.json` (real) | 200 | 200 |
| `https://scicrunch.org/resolver/RRID:AB_0000000.json` (bogus) | 404 | 404 |

The bare resolver URL sits behind a **Cloudflare interactive challenge**
(`cf-mitigated: challenge`, "Just a moment"), returning HTTP 403 to every
server-side client regardless of user-agent or source IP, for valid and invalid
RRIDs alike. Under `AbstractUriValidator` a 403 is not a 404, so it maps to a
server error. Pattern A as written would have rejected **every** RRID related
object in production with "uri could not be validated - server error".

Resolution (agreed via the ticket): HEAD-check the `.json` resolver variant, which
is not behind the challenge and returns a clean 200 (exists) / 404 (not found).
`RridService.resolverUri()` appends `.json`; the stored id and the DataCite
relatedIdentifier value remain the bare URL. The identity-default hook keeps every
other validator unchanged. Findings are recorded on RAID-787.

## Testing

- **Unit** — `./gradlew :api-svc:raid-api:test` green.
  - `RridServiceTest` — asserts the `HEAD` targets the `.json` URL; parameterised
    accept cases (`AB_`, `SCR_`, `CVCL_`, `IMSR_JAX:` with embedded colon) and
    reject cases (missing source/id, whitespace, brace).
  - `AbstractUriValidatorTest` — `resolverUri` identity default regression test.
  - `RelatedObjectValidatorTest` — RRID happy path and a negative-routing case
    proving an RRID-shaped id under `schemaUri=scicrunch` calls `rridService` and
    `verify(doiService/handleService, never())`.
  - `DataciteRelatedIdentifierFactoryTest` — scicrunch case now asserts
    `relatedIdentifierType == "RRID"`.
- **Integration** — all 11 `RelatedObjectIntegrationTest` cases pass (3 RRID
  scenarios + pre-existing Handle/web-archive/DOI cases, no regressions), with the
  stubbed resolver.
- **Live resolver check (NFR)** — see table above; one known-good and one
  known-bad real RRID run against live scicrunch.org, which surfaced the Cloudflare
  block and drove the `.json` decision.

## Acceptance criteria

- [x] Scenario 1 — RRID-scoped relatedObject accepted when resolver confirms it exists (201)
- [x] Scenario 2 — RRID-scoped relatedObject rejected when resolver fails (400, `relatedObject[0].id` / `invalidValue` / "uri not found")
- [x] Scenario 3 — RRID represented as `relatedIdentifierType="RRID"` in DataCite output (covered by `DataciteRelatedIdentifierFactoryTest`; no brittle outbound-payload intTest fabricated)

## Follow-ups

- **Cross-repo (REQUIRED before prod):** add the per-env
  `raid.stub.rrid.enabled: false` override in the `raido-v2-aws-private` CDK env
  config (mirroring `raid.stub.doi` / `raid.stub.handle`) so stage/prod hit the
  real `.json` resolver. Local/intTest keep the stub enabled. Without this the RRID
  stub is active in every environment and no real resolution happens.
- **DataCite live-API NFR (open):** the ticket asks for an integration test hitting
  DataCite's real test API to confirm acceptance of `relatedIdentifierType: "RRID"`.
  No live-DataCite harness exists (all DataCite tests use MockServer under the `dev`
  profile). `RRID` is a valid type in DataCite schema 4.6 and the payload uses the
  unversioned `kernel-4` namespace, so it is accepted today; the mapping is covered
  at unit level. A credentialed, gated regression test is flagged on the ticket for
  a decision (add here vs split out).
- **Shared URI-template 500 gap (pre-existing, affects DOI/Handle too):**
  `RequestEntity.head(String)` treats the argument as a URI template, so a brace in
  any validator's input throws `IllegalArgumentException` — a `RuntimeException`
  that escapes both catch blocks in `AbstractUriValidator` and surfaces as HTTP 500
  rather than a clean validation failure. The tightened RRID regex closes this for
  RRID; the general fix belongs in `AbstractUriValidator` (pass the URI as a
  variable / use the `URI` overload) and should be a follow-up ticket in the
  RAID-802 hardening lineage.
- **`au-research/raid-skos` (ticket task):** replace the placeholder
  `skos:definition "RRID"@en` for `https://scicrunch.org/resolver/` in
  `data/core/relatedObject.ttl` with a real definition, regenerate the aggregated
  vocabulary, and publish to Research Vocabularies Australia.
- **Public docs (ticket task):** document that RRID is now accepted and validated
  (reassign to docs owner if not dev work).
