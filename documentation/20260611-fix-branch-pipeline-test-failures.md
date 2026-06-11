# Fix Branch-Build-Deploy test failures (RAID-461)

**Date:** 2026-06-11
**JIRA:** [RAID-461](https://ardc.atlassian.net/browse/RAID-461) (parent: [RAID-292](https://ardc.atlassian.net/browse/RAID-292))
**PRs:** [#508 — Keycloak SPI null guard (to main)](https://github.com/au-research/raid-au/pull/508)
**Branch:** `feature/RAID-461` (commits `aefcce32`, `7f24b64c`, `5a8534dc`)

## Background

The `feature/RAID-461` branch deployment (RDF content negotiation for RAID-292) failed both
test actions in the Branch-Build-Deploy pipeline: `Api-Integration-Test` (1 of 154 tests) and
`E2e-Test` (3 of 27 tests). RDF content negotiation itself was verified working on
`api-raid-461.test.raid.org.au` (Turtle, RDF/XML, N-Triples and JSON-LD all return 200 with
correct content types; default JSON unchanged).

## Root causes and fixes

### 1. E2E: hardcoded embargo dates rejected by the API

Tests 02, 03 and 05 mint a RAiD with embargoed access and a hardcoded expiry of 2030.
`AccessValidator` rejects embargo expiry more than 18 months in the future, so every save
returned a silent 400 (validation failures are not logged server side) and the tests timed
out waiting for navigation.

**Fix:** new `e2e/utils/date-helpers.ts` with `validEmbargoExpiry()` (today + 12 months);
specs 02–05 now use it. Commit `5a8534dc`.

### 2. E2E: create form sends `identifier: {}` which the branch API rejects

This branch generates API request models from the strict 3.1 spec with bean validation
enabled (`useBeanValidation: true`). The create form payload included an empty
`identifier: {}`, which now fails nested `@NotNull` validation with six
`identifier.* field must be set` failures. Main's API silently ignored it.

**Fix:** new `raidCreateRequest()` helper omits the identifier from the mint payload (the
API generates it); `raidService.create` now takes `RaidCreateRequest`. Verified by direct
POST to the branch API returning 201. Commit `7f24b64c`.

### 3. Integration test: organisation insert race on fresh schema

`AccessIntegrationTest > Mint with valid embargoed access type` got a 500. The first mints
against the freshly created `raid_461` schema were slow (cold start), the test client
retried, and concurrent requests raced through the non-atomic select-then-insert in
`OrganisationRepository.findOrCreate()`, producing a `DuplicateKeyException` on
`organisation_pid_schema_id_key`.

**Fix:** `findOrCreate` now falls back to `INSERT .. ON CONFLICT DO NOTHING` and re-reads
the row when the insert loses the race. Commit `aefcce32`.

### 4. Keycloak SPI NPE masking failures (separate branch, PR to main)

In-flight mint retries referencing a deleted test user hit an NPE in
`RaidPermissionsController.addAdminRaid` (`getUserById` result dereferenced without a null
check), producing opaque `unknown_error` 500s. Fixed on
`feature/RAID-461-keycloak-user-not-found` ([PR #508](https://github.com/au-research/raid-au/pull/508))
targeting `main`, because branch deployments use the shared test Keycloak which deploys
from `main`.

### 5. Branch UI deployment bricked by s3 sync skipping index.html

After the fixes above, E2E failed at auth setup: the deployed app never booted.
`aws s3 sync --delete` skips files whose size is unchanged, and `index.html` changed only
by equal-length content hash references, so the sync deleted the old hashed JS chunk while
leaving the old `index.html` pointing at it ("Failed to load module script ... MIME type
text/html"). This never bit before because no prior run had changed the UI bundle.

**Fix:** the `S3-Sync-UI` buildspec now force-copies `index.html` after the sync
(`raido-v2-aws-private` commit `893757a` on main, deployed to the AdminStack). New
`branch-s3-sync-project.test.ts` covers the buildspec commands.

## Outcome

Pipeline execution `1dcb3f30` (2026-06-11 11:32): **all stages succeeded**, including
`Api-Integration-Test` and `E2e-Test`. The deployed branch app serves a consistent
bundle and RDF content negotiation is verified working on the branch API.

## Tests

- `OrganisationRepositoryTest` (new, 3 tests) — found/insert/race paths
- `RaidPermissionsControllerTest.addAdminRaid_throwsUserNotFoundWhenUserDoesNotExist` (new)
- `data-utils.test.ts` (new, 5 tests) — `raidRequest` and `raidCreateRequest`
- Full frontend vitest suite: 112 passed
- Manual verification against the branch environment with the e2e user's credentials:
  mint without identifier and a valid embargo date returns 201
