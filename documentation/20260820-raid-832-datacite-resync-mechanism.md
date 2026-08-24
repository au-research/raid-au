# RAID-832: Reusable in-application DataCite re-sync mechanism

- Ticket: [RAID-832](https://ardc.atlassian.net/browse/RAID-832) (parent [RAID-796](https://ardc.atlassian.net/browse/RAID-796))
- PR: [au-research/raid-au#619](https://github.com/au-research/raid-au/pull/619)
- ADR: `doc/adr/2026-08-20_datacite-resync-mechanism.md`

## What changed and why

RAiD periodically changes how it maps RAiD metadata to DataCite (RAID-797 is the latest).
Each change leaves already-minted records stale in DataCite until they are re-pushed, and so
far each backfill has been a bespoke script run by hand, which is both repeated effort and a
manual deployment step.

This adds a reusable, in-application DataCite re-sync mechanism, built once. A future
correction becomes a one-line targeting migration that flags its affected rows, and an
existing background worker drains them by re-pushing the full current metadata through the
idempotent DataCite PUT. Because the worker runs inside the application, it works in every
environment and under any pipeline, with no manual step.

### Code

- `V44__datacite_resync_queue.sql`: adds `datacite_resync_required boolean not null default
  false` to the `raid` table (additive and defaulted, so safe under rolling deployment).
  JOOQ regenerated for the new column.
- `RaidService`: clears the flag after every successful DataCite post (update,
  patch-contributors, post-to-datacite), so an edited record is never left stale and the
  worker never double-processes a record a user already refreshed. Adds
  `resyncWithDatacite(handle)`, which loads a raid by handle and re-pushes it through the
  existing idempotent `DataciteService.update(RaidDto, ...)` PUT, resolving service-point
  credentials the same way the other paths do.
- `DataciteResyncWorker` (`@Scheduled`, `@ConditionalOnProperty` on
  `raid.datacite.resync.enabled`): drains flagged rows in capped batches, throttles between
  DataCite calls, defers a record on failure or a 429 (leaves the flag set for a later tick),
  logs synced/deferred counts, and is a cheap no-op once nothing is flagged.
- `PostgresAdvisoryLock`: reusable collaborator that runs a unit of work under a Postgres
  session-level advisory lock held on a single connection (`pg_try_advisory_lock`, key
  `0x2126_0832L`). First advisory-lock user in the codebase; future users must not reuse this
  key.
- `DataciteResyncRepository`: JOOQ queries over the flag column.
- `DataciteResyncProperties` (`raid.datacite.resync.*`): `enabled` (default false),
  `batchSize` (50), `throttleMillis` (1000, about one request per second), `pollDelayMillis`
  (60000). Toggleable per environment.
- `@EnableScheduling` on `Api.java` (first `@Scheduled` component in the codebase).

### Tests

- Unit: `DataciteResyncWorkerTest` (drain, defer-on-failure, defer-on-429, no-op-when-empty,
  and the tick lock-orchestration: work skipped when the lock is not granted, runs when it
  is, and a throwing tick never propagates). `PostgresAdvisoryLockTest` (try/finally release
  contract, including release when the work throws).
- Integration (real Postgres): `DataciteResyncLockIntegrationTest` (advisory-lock mutual
  exclusion and unblock-on-release across two connections, AC2); `DataciteResyncFlagIntegrationTest`
  (write path clears a forcibly-set flag end-to-end, AC4).
- Full `./gradlew :api-svc:raid-api:intTest` green locally: 184 passed, 0 failed.

## Follow-ups and coordination

- The targeting migration that actually flags RAID-797's already-minted rows
  (`update api_svc.raid r set datacite_resync_required = true where exists (select 1 from
  api_svc.related_raid rr where rr.raid_name = r.handle)`) is owned by RAID-797, not this
  ticket. RAID-797 / PR #617 currently ships no targeting migration; that is the follow-up
  once this mechanism lands.
- The operator documentation change that makes the hand-run backfill script a fallback only
  should be coordinated with RAID-797, whose branch holds the RAID-797-specific backfill
  script.

## Verification performed

- Full unit test suite and full integration test suite green locally (see Tests above).
- A one-off manual verification against DataCite's real test API is still required for
  end-to-end confirmation, per the ticket's non-functional requirements (one-time, not a new
  scheduled suite). Not yet performed.
