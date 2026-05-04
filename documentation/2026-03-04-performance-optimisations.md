# Performance Optimisations (RAID-507)

This document describes the performance optimisations delivered under
[RAID-507](https://ardc.atlassian.net/browse/RAID-507), merged via
[PR #303](https://github.com/au-research/raid-au/pull/303).

## Background

Two areas of the API were experiencing performance issues that scaled with
input size:

1. **Minting (creating) a RAiD** — validation and persistence time grew
   linearly with the number of contributors, organisations, related objects,
   and spatial coverages. A moderately complex RAiD could take over 2 seconds
   just for validation, leading to HTTP timeouts.

2. **Listing RAiDs** — the `findAllRaids` endpoint reconstructed every
   `RaidDto` from event-sourcing history individually. For 100 raids this
   meant 200+ database queries plus in-memory JSON patch replays, and
   additional per-raid database lookups for visibility filtering.

## Changes

### RAID-512: Parallelise I/O-bound validators

[RAID-512](https://ardc.atlassian.net/browse/RAID-512) |
[PR #298](https://github.com/au-research/raid-au/pull/298)

Four validators in `ValidationService` make external HTTP calls during
`validateForCreate()` and `validateForUpdate()`:

- `ContributorValidator` — ORCID and ISNI lookups
- `OrganisationValidator` — ROR lookups
- `RelatedObjectValidator` — DOI HEAD requests
- `SpatialCoverageValidator` — GeoNames and OpenStreetMap lookups

Previously these ran sequentially. A RAiD with 5 contributors, 3
organisations, 2 related objects, and 1 spatial coverage made ~11 sequential
HTTP calls at ~200ms each (~2.2 seconds).

The four I/O-bound validators now run concurrently using `CompletableFuture`
and the existing `applicationTaskExecutor`. In-memory validators remain
synchronous. A shared `runIoBoundValidators()` helper handles fan-out, join,
and `CompletionException` unwrapping.

**Impact**: Validation time reduced from ~2.2s to ~200ms (the duration of the
single slowest HTTP call).

### RAID-515: Cache reference data lookups

[RAID-515](https://ardc.atlassian.net/browse/RAID-515) |
[PR #299](https://github.com/au-research/raid-au/pull/299)

During `RaidIngestService.create()`, every title, contributor, organisation,
description, related object, and subject triggers lookups against reference
tables (schema URIs, type URIs, language codes). These are static data
populated exclusively by Flyway migrations — they never change at runtime.

`@Cacheable` annotations were added to 26 repository methods (15 schema
repositories and 11 type/value repositories). The existing
`ConcurrentMapCacheManager` (unbounded, non-expiring) is used, which is
appropriate since the data is immutable at runtime.

**Impact**: Eliminates 40+ redundant database round-trips per mint after the
first request populates the cache.

### RAID-518: Materialise current RaidDto state

[RAID-518](https://ardc.atlassian.net/browse/RAID-518) |
[PR #301](https://github.com/au-research/raid-au/pull/301)

The `findAllRaids` endpoint previously reconstructed each `RaidDto` from
event-sourcing history — loading JSON patch records from `raid_history` and
replaying them in memory. For N raids this required at least 2N database
queries plus N JSON patch replays.

The existing `raid.metadata` JSONB column (previously used only for legacy
raids) now stores the full current `RaidDto` JSON. This is written on every
create, update, and patch operation. List queries read directly from this
column — a single query returns pre-built DTOs with no reconstruction.

A `RaidDtoReadService` component handles deserialisation with fallback to
history reconstruction for legacy or un-backfilled raids.

For existing v2 raids, a backfill endpoint is available:

```
POST /admin/backfill-metadata
Authorization: Bearer <operator-token>
```

This returns a JSON response with counts: `{ "total": N, "backfilled": M, "skipped": K }`.

The `raid_history` table remains the source of truth for versioning and audit.
The materialised column is a read-optimised projection.

**Impact**: List queries reduced from 200+ database queries to a single query
for 100 raids.

### RAID-521: Push isViewable filtering into SQL

[RAID-521](https://ardc.atlassian.net/browse/RAID-521) |
[PR #302](https://github.com/au-research/raid-au/pull/302)

For non-operator users, the `findAllRaids` endpoint loaded all raids for a
service point into memory and then filtered with `isViewable()` in Java. This
filter checked access type, admin/user raid grants, and service point
membership. Worse, the service point membership check called
`getServicePointId()` per raid — a redundant database query each time.

The entire `isViewable` logic was pushed into the SQL WHERE clause:

```sql
WHERE (
    access_type_id IN (1, 4)           -- open access always visible
    OR handle IN (:adminRaids, :userRaids)  -- JWT-granted access
    OR service_point_id = :userSP      -- service point membership
)
```

The access type IDs and handle lists were already columns on the `raid` table,
so this required no schema changes. The four dead `isViewable`-related methods
were removed from `RaidController`.

**Impact**: Eliminates loading embargoed raids the user cannot see, and removes
per-raid database lookups for service point membership.
