# Fix access.statement conditional validation (RAID-697)

## What changed

### Problem

Changing a RAiD's access type from Embargoed to Open Access on the edit page failed with `access.statement: field must be set`. Two root causes:

1. **Backend**: The `access.statement` field was marked `required: true` in the LinkML data model (`raid-core.yaml`), which propagated through JSON Schema → OpenAPI 3.1 → generated `Access.java` as a `@NotNull` annotation. JSR 303 bean validation rejected null statements before the custom `AccessValidator` could apply its conditional logic (statement is only required for Embargoed access, not Open Access).

2. **Frontend**: `RaidEdit.tsx` called `addMissingEndDateInPlace()` which mutated the response data on every render, causing React Hook Form to detect changes and reset the form — discarding user edits to the access type.

### Fix

**Backend (data model)**:
- `raid-core.yaml`: Changed `statement.required` from `true` to `false`
- Regenerated `raid-jsonschema.json`, `raid-strict-jsonschema.json`, `raid-openapi-3.1.yaml`, `raid-openapi-strict-3.1.yaml` — all now list only `type` as required on Access, not `statement`
- `raido-openapi-3.0.yaml`: Removed erroneous `required: [statement]` from `AccessStatement`
- No validator changes needed — `AccessValidator` already handles conditional logic correctly

**Frontend**:
- `RaidEdit.tsx`: Replaced mutating `addMissingEndDateInPlace()` with pure `addMissingEndDate()`, wrapped in `useMemo` to stabilise the reference
- Added `TransformResponseData.test.ts` with 10 unit tests covering both the pure and mutating variants

**Integration tests**:
- Added 4 new tests in `AccessIntegrationTest.java`: open access with null statement, open access with statement, embargoed missing statement, embargoed missing both statement and embargoExpiry
- Fixed pre-existing false positive in `missingEmbargoExpiry` test (was not actually nulling `embargoExpiry`)
- Added `fail()` guards to all negative tests to prevent silent false positives

### JSR 303 audit

Audited all 35 custom validators. The `access.statement` fix was the only case where a `@NotNull` annotation on a generated model conflicted with conditional validation logic.

## JIRA tickets

- Parent: [RAID-461](https://ardc.atlassian.net/browse/RAID-461)
- Sub-task: [RAID-697](https://ardc.atlassian.net/browse/RAID-697)

## PR

On branch `feature/RAID-461`, commit `a1c606e7`.
