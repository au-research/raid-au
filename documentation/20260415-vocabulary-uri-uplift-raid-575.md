# RAID-575: Vocabulary URI Uplift & Operator Service Point Fix

**Date**: 2026-04-15

## What Changed

### Operator Service Point Fix
Operators can now update raids across service points without requiring a Keycloak group mapping. The `PUT /raid/{prefix}/{suffix}` endpoint now uses the raid's own service point from the request payload when the caller has the `operator` role, rather than resolving via `getServicePointId()` which requires a Keycloak group mapping.

### Vocabulary URI Uplift Script
Added `scripts/uplift-vocabulary-uris.sh` to migrate legacy `github.com/au-research/raid-metadata` URIs to `vocabulary.raid.org` and COAR equivalents. The script processes raids via the API's PATCH endpoint, fixing:
- Title types
- Description types
- Contributor roles and positions
- Organisation roles
- Related object types

### Legacy Raid Read Fix
Fixed HTTP 500 errors when reading legacy raids (handles `102.100.100/*`, `10378.1/*`) that were never registered with DataCite. The update path now skips DataCite API calls for non-DOI handles.

### Cleanup
Removed the unused `/raid/all` endpoint and associated `findAllIncludingWithoutHistory()` methods in `RaidRepository` and `RaidIngestService`.

## Why

Legacy raids stored vocabulary URIs pointing to `github.com/au-research/raid-metadata` which is being deprecated in favour of `vocabulary.raid.org`. Investigation revealed the normalised database tables already had correct URIs, but the denormalised `metadata` column on the `raid` table still contained stale github.com URIs for 9 records.

The operator fix was needed because updating raids from other service points required Keycloak group mappings that don't exist for operators working across the system.

## Deployment Plan

1. Deploy code changes
2. Fix stale metadata column: `UPDATE api_svc.raid SET metadata = NULL WHERE metadata::text LIKE '%github.com/au-research/raid-metadata%';`
3. Call `POST /admin/backfill-metadata` to re-materialise from clean normalised data
4. Optionally run uplift script to confirm 0 changes needed

## JIRA Tickets
- Parent: [RAID-575](https://ardc.atlassian.net/browse/RAID-575)

## Pull Request
- [PR #433](https://github.com/au-research/raid-au/pull/433)
