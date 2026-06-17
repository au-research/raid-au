# RAID-690: Restore original date_created for corrupted RAiDs

## What changed and why

A bug in `RaidRepository.update()` overwrote `raid.date_created` with `LocalDateTime.now()` on every update, corrupting the original mint timestamp. At least 59 RAiDs had their creation date overwritten. This was fixed in two parts:

1. **Write-path fix** (commit `6b5787dc`): `RaidHistoryService.save(RaidUpdateRequest)` and `save(RaidDto)` now fetch `dateCreated` from the database instead of using `LocalDateTime.now()`, making `metadata.created` immutable on update.

2. **Data restoration** (V42 Flyway migration): Restores corrupted data across three stores for two categories of raids:
   - **Raids with revision 1 history** (664 affected): Sources true creation time from `raid_history.created` on the revision 1 PATCH record.
   - **V1-imported raids** (132 affected, no revision 1 record): Sources true creation time from the `/metadata` add operation in the revision 2 PATCH diff. This is more reliable than `metadata.created` which could also be corrupted by subsequent updates.

   Stores fixed per category:
   - `raid.date_created` column
   - `raid.metadata` materialised JSONB (`metadata.created` field)
   - `raid_history.diff` patch and baseline diffs containing wrong `/metadata/created` values

## Testing

Tested against a fresh production database dump (2026-06-17). Results:
- Step 1: 664 raids fixed (date_created + metadata JSONB)
- Step 2: 391 patch diffs fixed
- Step 3: 1 baseline diff fixed
- Step 4: 132 V1-imported raids fixed (date_created + metadata JSONB)
- Step 5: 6 V1-import patch diffs fixed
- Step 6: 0 V1-import baseline diffs (none exist)

Post-migration verification: 0 remaining inconsistencies across all 796 raids. 178 raids have no `metadata` key in their JSONB (pre-metadata-feature raids, never updated since) -- this is expected and outside scope.

## JIRA

- Parent: [RAID-690](https://ardc.atlassian.net/browse/RAID-690)

## PR

- [PR #525](https://github.com/au-research/raid-au/pull/525)
