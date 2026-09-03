# RAID-737: Accept sandbox ORCID contributor schemaUri in non-production

**Date:** 2026-07-30
**JIRA:** [RAID-737](https://ardc.atlassian.net/browse/RAID-737) — ORCID sandbox schemaUri incorrectly rejected in non-production environments
**PRs:**
- raid-au: https://github.com/au-research/raid-au/pull/588
- raido-v2-aws-private (deployed env config): https://github.com/au-research/raido-v2-aws-private/pull/33

## Problem

US pilot testers were instructed to use sandbox ORCiDs, but submitting a contributor with
`schemaUri: https://sandbox.orcid.org/` was rejected during validation in the demo environment.

Two independent causes:

1. **Enum did not contain the value.** Contributor `schemaUri` is a codegen-locked enum
   (`ContributorSchemaUriEnum`) materialised from the ARDC controlled-lists SPARQL vocabulary.
   It only contained `https://orcid.org/` and `https://isni.org/`, so a sandbox value failed at
   JSON deserialization (`fromValue` throws) before any validator ran.
2. **Validation was not environment-scoped.** The deployed non-prod environments override the
   contributor ORCID id `url-prefix` to sandbox but never override `schema-uri`, so schemaUri was
   still validated against the production default `https://orcid.org/`.

## Required behaviour (from the ticket)

- Production: accept only `https://orcid.org/`.
- Non-production (test, demo, sandbox): accept only `https://sandbox.orcid.org/`.

This keeps production free of sandbox-issued identifiers and vice versa.

## Vocabulary prerequisite

The value had to come from the vocabulary (the enum is generated, not hand-maintained). The ARDC
controlled-lists team published **RAiD-CL v1.7.1**, adding the "ORCID SANDBOX contributor ID schema"
concept (`https://sandbox.orcid.org/`). An earlier draft (v1.7.0) had modelling defects that made the
build's SPARQL query skip the value; these were reported and fixed before this change (see RAID-737
comments). Verified the v1.7.1 endpoint returns all three values with the correct trailing slash.

## Changes

### raid-au (PR #588)

- **`datamodel/src/v2/core-enums.yaml`** — repointed `ContributorSchemaUriEnum.reachable_from.source_ontology`
  to the v1.7.1 production endpoint
  (`https://vocabs.ardc.edu.au/repository/api/sparql/raid_research-activity-identifier-raid-controlled-lists_raid-cl-v1-7-1`).
- **Regenerated** (`./gradlew :api-svc:datamodel:generateStrictJsonSchemaV2 :api-svc:datamodel:generateReferenceDataV2`,
  then `assembleOpenAPIV2InternalSpecs` + `openApiGenerate`), producing a single semantic addition of
  `https://sandbox.orcid.org/` to: `raid-strict-jsonschema.json`, `raid-openapi-strict-3.1.yaml`,
  `ContributorSchemaUriEnum-allowed-values.yaml`, and `sparql-cache/ContributorSchemaUriEnum.json`.
  The git-ignored generated Java enum now includes `HTTPS_SANDBOX_ORCID_ORG_`.
- **`raid-api/.../application-dev.yaml`** — added `raid.contributor-validation.orcid.schema-uri: https://sandbox.orcid.org/`
  so local dev accepts sandbox schemaUri. The *validation* scoping is config-driven: `ContributorTypeValidator`
  already validates `schemaUri` against this config value, so no validator code change was needed.
- **`factory/datacite/DataciteCreatorFactory.java`** (production Java) — the DataCite mint path maps each
  contributor to a `DataciteCreator` and previously threw `RuntimeException` for any schemaUri other than
  `https://orcid.org/` / `https://isni.org/`. Without this fix, minting a raid with a (now-required) sandbox
  contributor in an environment that mints real DOIs (e.g. demo) would 500. It now treats
  `https://sandbox.orcid.org/` as the ORCID scheme (resolves the name via `orcidClient`,
  `nameIdentifierScheme: "ORCID"`). This gap was found by running the integration tests, not by the
  enum-usage grep — the check is a string comparison against a hardcoded literal, not a `switch`.
- **Flyway migration** `db/env/{dev,test,demo}/V42.1__add_sandbox_contributor_schema.sql` (production data) —
  `ContributorService.create()` looks the schemaUri up in the `contributor_schema` table and 500s
  (`ContributorSchemaNotFoundException`) if there is no matching row. The table is a registry of known
  schemes (seeded orcid.org in V27, isni.org in V29). This seeds `https://sandbox.orcid.org/` (status
  `active`) **only in the non-prod environment locations** — prod and stage deliberately omit it, consistent
  with them accepting only production ORCID. The insert is guarded with `where not exists` because
  `findByUri` uses `fetchOptional`, which throws on duplicate rows (defends against dump-restored test DBs).
  No JOOQ regen needed (data-only; the `status` column already exists). Also found by the integration tests.
- **`testFixtures/.../fixtures/TestConstants.java`** — `ORCID_SCHEMA_URI` changed to `https://sandbox.orcid.org/`.
  Integration tests run under the dev profile (which now enforces sandbox), and the fixture id
  (`REAL_TEST_ORCID`) was already sandbox; the schemaUri constant is referenced by ~20 intTest call sites so
  the single change cascades. The separate unit-test `TestConstants` keeps production values.
- **Tests** — four new `ContributorTypeValidatorTest` cases (prod rejects sandbox / accepts orcid; non-prod
  accepts sandbox / rejects orcid) and one new `DataciteCreatorFactoryTest` case (sandbox ORCID maps to the
  ORCID scheme).

### raido-v2-aws-private (companion PR)

Added `raid.contributor-validation.orcid.schema-uri: https://sandbox.orcid.org/` wherever the CDK already
overrides `url-prefix` to sandbox: **test**, **demo**, **branch deployments**, and the **agency non-prod**
environment. **Stage and prod are intentionally unchanged** — they keep production ORCID (they do not
override `url-prefix`).

## Notes / gotchas

- The `AddStaticEnums` task (`generateStrictJsonSchemaV2`) emits **minified** JSON, whereas the committed
  `raid-strict-jsonschema.json` is pretty-printed (2-space). After regen, re-serialise with 2-space indent
  (`ensure_ascii=False`, trailing newline) so the diff is only the semantic enum change.
- `generateReferenceDataV2` regenerates **all** enum example files; it surfaced an unrelated stale entry in
  `RelatedObjectSchemaUriEnum-allowed-values.yaml` (a dropped `https://archive.org/`) which was reverted to
  keep this change scoped.
- `datamodel/generated/v2/referencedata.sql` is generated but not tracked — do not commit it.

### raid-agency-app (frontend — part of PR #588)

The agency UI previously hardcoded the contributor `schemaUri` to `https://orcid.org/` (Zod schema +
data generator), which would fail non-prod validation once the backend enforces sandbox-only. Made it
environment-aware, mirroring the existing `containers/orcid-lookup/ORCID.tsx` pattern:

- New `src/utils/contributor-utils/contributor-schema-uri.ts` — `getContributorSchemaUri()` returns
  `https://orcid.org/` in prod, `https://sandbox.orcid.org/` otherwise. Called **lazily** only — never at
  module load, because `getRuntimeConfig()` throws before the runtime config is loaded.
- `entities/contributor/validation-schema/contributor-validation-schema.ts` — `schemaUri` is now a
  `z.string().refine(...)` that accepts only the env-appropriate value (refinement runs at parse time).
- `entities/contributor/data-generator/contributor-data-generator.ts` — uses the helper.
- Added a helper unit test; `npm run build` + `npm test` pass (one pre-existing unrelated ServicePoint test
  failure, confirmed at baseline).

## Deploy coordination (three changes must land together)

Because the rule is strict (non-prod accepts **only** sandbox), the branch pipeline proved these three must
deploy together or the pipeline can't be green — under the strict rule, intTest (fixture → sandbox) and E2E
(agency UI → its schemaUri) require the API and UI to agree:

1. CDK #33 — deployed env config sets non-prod `schema-uri` to sandbox (branch API then requires sandbox).
2. This PR's frontend change — UI sends sandbox in non-prod.
3. This PR's fixture change — intTest sends sandbox.

Deploying any subset causes a mismatch (e.g. #33 without the frontend → UI 400s; the frontend without #33 →
E2E 400s). Merge/deploy #588 and #33 together; the branch pipeline stays red until #33 reaches the branch
environment. Prod is unaffected (it keeps `https://orcid.org/` throughout).

## Verification

- Regenerated Java enum contains all three values including `https://sandbox.orcid.org/`.
- Enum-usage audit: no exhaustive `switch` on `ContributorSchemaUriEnum`, but `DataciteCreatorFactory`
  compared `schemaUri.getValue()` against a hardcoded `"https://orcid.org/"` string (fixed here). The only
  other hardcoded `orcid.org` literal, `SchemaValues.ORCID_SCHEMA_URI`, is defined but unused.
- Integration tests run under the dev profile; the full contributor intTest surface was run and confirmed
  green after the fixture + DataCite fixes (previously 41 failures, all traced to the mismatched fixture
  schemaUri). intTest uses local (`http://raid.local/`) handles so it does not exercise the DataCite mint
  path — the `DataciteCreatorFactoryTest` unit test covers the sandbox mapping directly.
- Unit tests pass (`ContributorTypeValidatorTest`, `ContributorValidatorTest`, `DataciteCreatorFactoryTest`);
  no other enum values drifted.
