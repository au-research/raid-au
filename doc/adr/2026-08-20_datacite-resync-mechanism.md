# Reusable in-application DataCite re-sync mechanism

- Status: accepted
- Date: 2026-08-20
- Ticket: RAID-832 (parent RAID-796)

## Context

RAiD periodically changes how it maps RAiD metadata to DataCite. RAID-797, which
emits related RAiDs using DataCite's native `RAiD` relatedIdentifierType instead of
the generic `DOI` type, is the most recent example. Each such change leaves
already-minted records stale in DataCite until we re-push them, and so far we have
written a bespoke backfill script for each change and run it by hand.

That pattern has two problems. It repeats effort every time, and a hand-run script is
a manual deployment step. Two constraints shaped the alternative:

1. Deployments must contain no manual step.
2. The mechanism must run identically in every environment and under any pipeline, not
   only AWS or CodeBuild. The only artifact that runs the same everywhere is the
   application itself, so the mechanism has to live in the application.

The key insight is that a record only ever needs to be marked "its DataCite copy is
stale, re-push it", never "it needs correction X". A re-push sends the record's full
current metadata through the existing idempotent `post-to-datacite` full-document PUT,
which already carries whatever the latest change altered. Several pending corrections
on one record therefore coalesce into a single re-push, and the worker never needs to
know the reason.

This decision promotes the spike proposal
`doc/spike/RAID-797-datacite-backfill-automation.md` to an accepted decision. The team
agreed to proceed with the design on 2026-08-20.

## Decision

Build a reusable "DataCite re-sync" mechanism once, then drive each future backfill
with a one-line targeting migration.

### Written once (RAID-832)

- A Flyway migration (`V44__datacite_resync_queue.sql`) adds a boolean column
  `datacite_resync_required` (default `false`) to the `raid` table. The column is
  additive and defaulted, so it is safe under rolling deployment.
- The write path clears the flag on every successful DataCite post. In `RaidService`
  that is the mint, update, patch-contributors, and post-to-datacite paths, so new and
  edited records are never stale. New mints default the column to `false`, so the clear
  matters most on user edits and on the worker's own re-push.
- A scheduled background worker (`DataciteResyncWorker`) drains the flagged records. On
  each tick it takes a Postgres session-level advisory lock on a single held connection
  so only one application instance re-syncs at a time, processes up to `batchSize`
  flagged records, re-pushes each through the existing idempotent path, clears the flag
  on success with its own per-record statement, throttles between calls, and logs
  counts. The worker runs on the scheduler thread, so it never blocks startup or
  request serving. It is self-terminating: once nothing is flagged, every tick is a
  cheap no-op.
- Configuration properties under `raid.datacite.resync` (`enabled`, `batchSize`,
  `throttleMillis`, `pollDelayMillis`) make the worker toggleable per environment. It is
  disabled by default.

### Per backfill

Each future correction ships its own one-line targeting migration that flags only its
affected rows, and the existing worker drains it. No new application code and no manual
step. For example, RAID-797's backfill is:

```sql
update api_svc.raid r set datacite_resync_required = true
where exists (select 1 from api_svc.related_raid rr where rr.raid_name = r.handle);
```

That targeting migration is owned by RAID-797, not by RAID-832. This ADR and PR deliver
only the reusable mechanism.

### Flag shape, pacing, and rate limiting

- A boolean targeting flag was chosen over a global generation counter (which would
  re-push every record on each bump) and over a `datacite_synced_at` timestamp (whose
  NULL-means-pending semantics are less clear). Each backfill declares its own scope
  through its migration predicate, so DataCite traffic stays minimal.
- The worker drains in capped batches per tick and releases the advisory lock between
  ticks, which bounds each tick's duration and avoids holding the lock through a large
  backlog.
- DataCite documents a general limit of 3,000 requests per 5 minutes per IP, but treats
  a backfill as its "large-scale updates" case and recommends 300 to 500 requests per 5
  minutes, with the test system capped at 750 per 5 minutes. The default targets roughly
  one request per second (`throttleMillis` around 1000, about 300 per 5 minutes), which
  sits inside that guidance and leaves headroom for normal minting traffic on the same
  per-IP budget. A DataCite 429 defers the record to a later tick. The values stay
  configurable per environment.

## Consequences

- No manual deployment step, and the same behaviour in every environment and under any
  pipeline.
- Reusable. A future backfill is a one-line migration, not another bespoke job.
- Decoupled from startup and serving. A DataCite outage does not fail the deployment or
  the application; the worker makes no progress that run and retries later. This follows
  the same decouple-from-external-availability principle as
  `doc/adr/2026-08-10_resolver-unavailable-503.md`.
- Self-terminating and idempotent. Once nothing is flagged, the worker is a silent
  no-op.
- The existing hand-run backfill script becomes an operator fallback only, not the
  primary path.
- The flag is the safety guard. Each backfill's predicate must be correct, especially in
  production, and the worker logs synced and deferred counts so a run is observable.
- New costs to maintain: a background executor (this is the first `@Scheduled` component
  in the codebase, so `@EnableScheduling` is now enabled on the application), the first
  use of a Postgres advisory lock (key `0x2126_0832L`, which future components must not
  collide with), and the flag column plus the write-path change that clears it.
