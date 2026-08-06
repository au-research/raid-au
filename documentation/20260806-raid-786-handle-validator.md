# RAID-786: Add Handle (hdl.handle.net) validator for relatedObject.schemaUri

- **JIRA (Story):** [RAID-786](https://ardc.atlassian.net/browse/RAID-786)
- **Parent epic:** [RAID-789](https://ardc.atlassian.net/browse/RAID-789) — Identifier & relatedObject validator hardening and coverage (RAID-699 follow-up)
- **Depends on:** [RAID-802](https://ardc.atlassian.net/browse/RAID-802) + [RAID-803](https://ardc.atlassian.net/browse/RAID-803) (superseded the original hardening dependency RAID-792, which was descoped; the `AbstractUriValidator` hardening those landed is inherited here)
- **PR:** [au-research/raid-au#602](https://github.com/au-research/raid-au/pull/602)
- **ADR:** `doc/adr/2026-08-06_related-object-scheme-validator-dispatch-map.md`

## What changed and why

`relatedObject.schemaUri` accepts a controlled set of resolver schemes. The enum
(`RelatedObjectSchemaUriEnum.HTTPS_HDL_HANDLE_NET_`) and OpenAPI specs already
declared `https://hdl.handle.net/`, but `RelatedObjectValidator` carried a
hard-coded two-item allow-list (`doi.org`, `web.archive.org`) that actively
**rejected** Handle-scoped related objects. RAID-786 adds a Handle validator,
wires it in, maps it for DataCite output, and corrects a scheme typo in the
vocabulary seed data that would otherwise break persistence.

Per Matthias's decision (2026-07-27), Handles are accepted and validated now
(ahead of documentation catching up). DOIs share the Handle namespace (prefix
`10.*`) but are **not** special-cased or rerouted to `DoiService` — a DOI-shaped
id declared under `schemaUri = https://hdl.handle.net/` is validated as a Handle
via the Handle resolver.

### Changes

- **`service/handle/HandleService.java`** (new) — extends `AbstractUriValidator`,
  mirroring `DoiService` (`@Getter`/`@RequiredArgsConstructor`, no method bodies).
  Regex `^https://hdl\.handle\.net/\d+(\.\d+)*/\S+$` (https-only, requires the
  resolver host, matches both `20.500.12345/abc123` and DOI-shaped `10.1234/xyz`).
  Validation is regex + resolver `HEAD`, inheriting the RAID-802 hardening
  (bounded timeouts, `RestClientException` handling).
- **`service/stub/HandleServiceStub.java`** (new) + two sentinels
  (`NONEXISTENT_TEST_HANDLE`, `SERVER_ERROR_TEST_HANDLE`) in `InMemoryStubTestData`
  — lets integration tests exercise the resolver-success/404/5xx paths without
  hitting live `hdl.handle.net`.
- **`config/properties/StubProperties.java`** — new nested `Handle` toggle; and
  `raid.stub.handle: {enabled, delay}` added to both `src/main/resources` and
  `src/intTest/resources` `application.yaml`.
- **`config/bean/ExternalPidService.java`** — new `@Bean @Primary handleService(...)`
  returning the stub when `raid.stub.handle.enabled`, else the real validator.
- **`validator/RelatedObjectValidator.java`** — refactored from the hard-coded
  if/else allow-list into a scheme→validator dispatch map
  (`Map<String, BiFunction<String, String, List<ValidationFailure>>>`) built in
  the constructor as an unmodifiable `LinkedHashMap`: `doi.org` →
  `doiService::validate`, `hdl.handle.net` → `handleService::validate`,
  `web.archive.org` → the existing inline regex lambda (no resolver call). The
  supported-scheme allow-list and the "unsupported schemaUri" error message are
  now both derived from the map's key set. Behaviour is otherwise unchanged
  (blank id → `NOT_SET`, null `schemaUri` → `NOT_SET`). See the ADR for rationale.
- **`factory/datacite/DataciteRelatedIdentifierFactory.java`** — added
  `HTTPS_HDL_HANDLE_NET_ → RelatedIdentifierType.HANDLE.getName()` ("Handle", a
  DataCite controlled-vocabulary term) to `IDENTIFIER_TYPE_MAP`.
- **`db/migration/V43__fix_handle_related_object_schema_scheme.sql`** (new) —
  corrects the `related_object_schema` vocabulary row from `http://hdl.handle.net/`
  (seeded incorrectly in V29) to `https://hdl.handle.net/`. Without this, a
  Handle-scoped related object passes validation but 500s at persistence
  (`RelatedObjectService.create`'s `findByUri` lookup misses). Idempotent and
  rolling-deploy-safe; the `http://` row is orphaned (Handle was never accepted
  before, so no `related_object` references it) — see below.

## The V43 migration (found by integration testing)

The architecture pass predicted no Flyway change was needed. Running the real
integration tests surfaced otherwise: `V29__update_vocabularies.sql` seeded the
Handle resolver as `http://hdl.handle.net/`, but the enum, validator and DataCite
factory all use `https://hdl.handle.net/`. A Handle related object therefore
passed validation and then threw `RelatedObjectSchemaNotFoundException` (HTTP 500)
at persistence. The `http://` row is safe to update in every environment because
Handle was never in the old allow-list, so no `related_object` row references it,
and the `UPDATE` preserves the row id that `related_object.schema_id` FKs to.

## Testing

- **Unit** — `./gradlew :api-svc:raid-api:test` green. `RelatedObjectValidatorTest`
  gains Handle happy-path, resolver-failure, and a no-rerouting case proving a
  `10.*`-shaped id under `schemaUri=hdl.handle.net` calls `handleService` and
  `verify(doiService, never())`. `DataciteRelatedIdentifierFactoryTest` gains a
  Handle case asserting `relatedIdentifierType == "Handle"`.
- **Integration** — all 8 `RelatedObjectIntegrationTest` cases pass (4 Handle
  scenarios + pre-existing web-archive cases, no regressions), with the stubbed
  resolver and V43 applied via Flyway at `bootRun` startup.
- **Live resolver check (NFR)** — against real `hdl.handle.net`:
  - known-good `20.1000/100` → HTTP 302 (resolves)
  - known-good DOI-shaped `10.1045/january99-bearman` → HTTP 302 (resolves)
  - known-bad `20.500.12345/definitely-does-not-exist-raid786` → HTTP 404

  Confirms the validator's behaviour: 302 from a valid handle raises no
  `HttpClientErrorException` (passes, same path DOIs use in prod); 404 maps to
  `URI_DOES_NOT_EXIST`.

## Acceptance criteria

- [x] Scenario 1 — Handle-scoped relatedObject accepted when resolver confirms it exists (201)
- [x] Scenario 2 — Handle-scoped relatedObject rejected when resolver fails (400, `relatedObject[0].id` / `invalidValue` / "uri not found")
- [x] Scenario 3 — DOI-shaped id under `schemaUri=Handle` validated via Handle resolver, not DOI resolver
- [x] Scenario 4 — Handle represented as `relatedIdentifierType="Handle"` in DataCite output (covered by `DataciteRelatedIdentifierFactoryTest`; no brittle outbound-payload intTest fabricated)

## Follow-ups

- **Cross-repo (required before prod):** add the per-env `raid.stub.handle.enabled: false`
  override in the `raido-v2-aws-private` CDK env config (mirroring `raid.stub.doi`)
  so stage/prod hit the real resolver. Local/intTest keep the stub enabled.
- **Extensibility:** the remaining spec-declared schemes (`arks.org`,
  `scicrunch.org`, `isbn-international.org`) can now be added as one-line
  dispatch-map entries rather than new if/else branches.
- **Docs:** update `metadata.raid.org` to document Handle support (reassign to
  docs owner if not dev work).
