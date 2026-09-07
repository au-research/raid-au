# RAID-876: GET /raid/ 500s a whole service point when any raid has NULL metadata

**Date:** 2026-09-08
**JIRA:** [RAID-876](https://ardc.atlassian.net/browse/RAID-876)
**PR:** raid-au [#PR](https://github.com/au-research/raid-au/pull/PR)

## What was wrong

`GET /raid/` returned 500 for an entire service point if any single raid row had a
`NULL` metadata column:

```
ERROR a.o.r.a.e.raidv2.RaidExceptionHandler : Unhandled exception
java.lang.NullPointerException: Cannot invoke "org.jooq.JSONB.data()" because "raidDto" is null
```

`RaidIngestService.findAllByServicePointIdOrHandleIn` and `RaidIngestService.findAll`
read the materialised `metadata` column directly, with no null guard:

```java
.stream().map(RaidRecord::getMetadata)
.map(raidDto -> objectMapper.readValue(raidDto.data(), RaidDto.class))
```

`RaidRecord.getMetadata()` returns `null` for such rows, so `JSONB.data()` throws. One
unresolvable row took down the whole list, not just that row.

## How it got there

Three changes, in sequence:

| Change | Effect | Safe? |
|---|---|---|
| `V27__normalise_raid_tables.sql` | Normalised the JSONB blob into relational tables; `alter column metadata drop not null` | Yes, nothing read it |
| RAID-518 (`59bc91bd`) | Reintroduced `metadata` as a materialised cache written on mint and update | Yes, reads still went via the guarded path |
| **RAID-507 (`acde8d82`)** | **List endpoints read the column directly for speed, dropping both fallbacks** | **No** |

The pre-RAID-507 implementation was null-safe by design:

```java
raidDtoReadService.toRaidDto(record)
        .orElseGet(() -> cacheableRaidService.build(record));
```

`RaidDtoReadService.toRaidDto` explicitly guards `record.getMetadata() != null` and falls
back to raid history; `CacheableRaidService.build` is a second fallback that rebuilds the
DTO from the relational tables. RAID-507 bypassed both.

Four sibling methods on the same class (`findAllByServicePointIdOrNotConfidential`,
`findAllByServicePointId`, `findAllByContributor`, `findAllByOrganisation`) never stopped
using the safe pattern, so the two affected methods were inconsistent with the rest of the
class.

## Why rows have NULL metadata

`RaidRecordFactory` sets `metadata_schema` but never `metadata`. Rows minted between V27
and RAID-518 that have not been updated since therefore have `NULL` metadata.

`RaidMetadataBackfillService` exists to populate them, but:

- it is only reachable through a manual admin endpoint, `POST /backfill-metadata`
  (`AdminController:17`), so it does not run on deploy; and
- it reconstructs through `raidHistoryService.findByHandle`, skipping any handle it cannot
  rebuild:

```java
log.warn("Could not reconstruct RaidDto for handle {}, skipping backfill", record.getHandle());
```

Observed in the `raid-sandbox` environment, where all three seeded raids have `NULL`
metadata and `raid_archive` is empty, so backfill could not have helped.

## What changed

Both methods now use the same guarded pattern as their four siblings:

```java
return raidRepository.findAllViewable(servicePointId, isServicePointUser, handles)
        .stream()
        .map(record -> raidDtoReadService.toRaidDto(record)
                .orElseGet(() -> cacheableRaidService.build(record)))
        .collect(Collectors.toList());
```

This preserves the RAID-507 performance intent. `toRaidDto` still tries the materialised
column first, so populated rows take the fast path unchanged; only `NULL` or unparseable
rows pay the assembly cost.

The now-unused `ObjectMapper` field and three imports were removed from
`RaidIngestService`.

## Tests

Five tests added to `RaidIngestServiceTest`:

- `findAllByServicePointIdOrHandleIn()` delegates resolution to `RaidDtoReadService`
- `findAllByServicePointIdOrHandleIn()` falls back to `cacheableRaidService` when metadata is null
- `findAllByServicePointIdOrHandleIn()` returns resolvable raids alongside one with null metadata
- `findAll()` delegates resolution to `RaidDtoReadService`
- `findAll()` falls back to `cacheableRaidService` when metadata is null

All five were verified to fail against the unfixed code with the exact production NPE, then
pass with the fix. Full `./gradlew build` and `./gradlew intTest` both green locally.

## Follow-up worth considering

Not addressed here, since they are beyond the scope of this bug:

- **Backfill is manual.** Nothing runs `POST /backfill-metadata` on deploy, so `NULL`
  metadata rows persist indefinitely in every environment.
- **Prod exposure is unquantified.** Prod raids generally do have history, so backfill would
  mostly succeed there, but no one has checked whether prod holds rows that would fail to
  reconstruct.
