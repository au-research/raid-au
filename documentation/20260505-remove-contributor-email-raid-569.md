# RAID-569: Remove Contributor Email Field

## Date
2026-05-05

## Summary

Removed the `email` field from the Contributor entity across the full stack: OpenAPI spec, generated Java models, generated TypeScript types, frontend validation schemas, data generators, test fixtures, and integration tests.

## What Changed

### OpenAPI Spec
- Removed `email` property from `Contributor` schema in `api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml`

### Backend
- Regenerated Java models (automatic via openApiGenerate task)
- Removed `"email"` field from `api-svc/raid-api/src/test/resources/fixtures/create-raid.json`
- Removed `CONTRIBUTOR_EMAIL` constant from `TestConstants.java`
- Removed `email` parameter from `APIFixtures.orcidContributor()` method
- Removed duplicate `validContributor()` test from `ContributorValidatorTest.java`
- Removed `.email()` calls from `ContributorsIntegrationTest.java`
- Disabled email-specific integration tests (`setEmail`, `getEmail`)
- Removed `contributor.email(null)` from `RaidIntegrationTest.java`

### Frontend
- Removed `email?: string` from generated `Contributor.ts`
- Removed email-mapping loop and unused import from `RaidEdit.tsx`
- Removed `email: z.string().optional()` and third union variant from contributor validation schema
- Removed email from contributor data generator union type

## Why

The email field was never used in production, added unnecessary complexity to the contributor model, and created confusion in the UI. Removing it simplifies the data model and reduces maintenance burden.

## Test Results

- **Unit tests**: All pass
- **Integration tests**: All pass
- **E2e tests**: 20/27 pass; 3 failures are pre-existing (sandbox ORCID URL doesn't match `https://orcid.org/` validation pattern); 4 skipped due to serial dependency on failing test. Zero email-related regressions.

## JIRA

- [RAID-569](https://ardc.atlassian.net/browse/RAID-569)

## Branch

`feature/RAID-569` — commit `66f7498c`
