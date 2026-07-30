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
  so local dev accepts sandbox schemaUri. No Java production code changed: `ContributorTypeValidator`
  already validates `schemaUri` against this config value, so the environment scoping is config-driven.
- **`ContributorTypeValidatorTest`** — four new unit tests: prod config rejects sandbox / accepts orcid;
  non-prod config accepts sandbox / rejects orcid.

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

## Verification

- Regenerated Java enum contains all three values including `https://sandbox.orcid.org/`.
- No exhaustive `switch` on `ContributorSchemaUriEnum` exists, so adding a value needs no mapping changes.
- `ContributorTypeValidatorTest` and `ContributorValidatorTest` pass; no other enum values drifted.
