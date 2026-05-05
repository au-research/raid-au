# RAID-535: Playwright E2E Tests for RAiD Metadata Schema v1.6.3

## What changed

Added a comprehensive Playwright E2E test suite for the RAiD agency app, covering the core user flows for creating, editing, and validating RAiD metadata records against schema v1.6.3.

### New files

| Path | Purpose |
|------|---------|
| `e2e/auth/setup.ts` | Keycloak authentication setup (storageState-based) |
| `e2e/page-objects/RaidFormPage.ts` | Core form page object (navigate, save, wait for save) |
| `e2e/page-objects/RaidListPage.ts` | List page object (navigate, find row) |
| `e2e/page-objects/sections/*.ts` | Section page objects for each metadata block (Title, Date, Access, Contributor, Description, Organisation, RelatedObject, RelatedRaid, SpatialCoverage, AlternateIdentifier, AlternateUrl) |
| `e2e/test-data/vocabulary.ts` | Vocabulary URI constants from general-mapping.json |
| `e2e/test-data/valid-raid.ts` | Valid RAiD test data factory |
| `e2e/utils/wait-helpers.ts` | Shared wait/extract helpers |
| `e2e/utils/mui-helpers.ts` | Shared MUI interaction helpers (assertSelectOptions) |
| `e2e/utils/raid-api-client.ts` | Direct API client for test setup/teardown |
| `e2e/tests/01-smoke.spec.ts` | Smoke test (login, navigation) |
| `e2e/tests/02-create-raid.spec.ts` | Create RAiD with required fields |
| `e2e/tests/03-optional-metadata.spec.ts` | Create RAiD with optional metadata blocks |
| `e2e/tests/04-validation.spec.ts` | Client-side validation enforcement |
| `e2e/tests/05-edit-lifecycle.spec.ts` | Create, edit, verify lifecycle |
| `e2e/tests/06-controlled-vocabularies.spec.ts` | Dropdown vocabulary option verification |

### Test coverage summary

- **02-create-raid**: 2 serial tests — create with required fields, verify on list page
- **03-optional-metadata**: 2 serial tests — create with 6 optional blocks, verify on view page
- **04-validation**: 5 independent tests — title required, date format, end-before-start, ORCID format, embargo expiry format
- **05-edit-lifecycle**: 3 serial tests — create, edit (title/date/description), verify list page
- **06-controlled-vocabularies**: 9 independent tests — all MUI Select dropdowns verified against expected options

## Why

The RAiD agency app had no automated E2E test coverage. Manual testing was the only quality gate for UI regressions and schema conformance. This suite provides automated verification of:

- Form field rendering and interaction for all 14 metadata blocks
- Client-side Zod validation rules
- Create and edit workflows end-to-end
- Controlled vocabulary dropdown contents matching the schema

## JIRA tickets

- **Parent story**: [RAID-535](https://ardc.atlassian.net/browse/RAID-535)
- **Sub-tasks**:
  - [RAID-536](https://ardc.atlassian.net/browse/RAID-536) — Test infrastructure (auth, page objects, config)
  - [RAID-537](https://ardc.atlassian.net/browse/RAID-537) — RAiD create with required fields
  - [RAID-538](https://ardc.atlassian.net/browse/RAID-538) — Optional metadata blocks
  - [RAID-539](https://ardc.atlassian.net/browse/RAID-539) — Validation tests
  - [RAID-540](https://ardc.atlassian.net/browse/RAID-540) — Edit lifecycle
  - [RAID-541](https://ardc.atlassian.net/browse/RAID-541) — Controlled vocabularies

## Pull requests

- [PR #326](https://github.com/au-research/raid-au/pull/326) — RAID-537
- [PR #327](https://github.com/au-research/raid-au/pull/327) — RAID-538
- [PR #328](https://github.com/au-research/raid-au/pull/328) — RAID-539
- [PR #329](https://github.com/au-research/raid-au/pull/329) — RAID-540
- [PR #330](https://github.com/au-research/raid-au/pull/330) — RAID-541
