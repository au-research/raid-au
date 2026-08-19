# DataCite re-sync mechanism

Status: proposal for team discussion (not a decision).
Related: RAID-797 (the first user of this mechanism), parent RAID-796.

This document proposes a reusable way to re-push already-minted RAiD metadata to DataCite,
automatically and portably. It began as the RAID-797 backfill, but DataCite backfills have
recurred on several occasions, so this proposes a general mechanism rather than another
one-off. It is a starting point for discussion, not a decision.


## Summary

We periodically change how RAiD metadata maps to DataCite (RAID-797, which switches related
RAiDs to DataCite's native "RAiD" relatedIdentifierType, is the latest). Each change leaves
already-minted records stale until we re-push them. So far we have written a bespoke backfill
each time, and run it by hand.

This proposes a reusable, in-application "DataCite re-sync" mechanism:

* a flag marks records whose DataCite copy is stale,
* a background worker re-pushes the flagged records idempotently, and
* each future correction becomes a one-line migration that flags its affected rows.

Because the worker runs inside the application, it works in any environment and under any
pipeline, and it contains no manual step. Because it is generic, a future backfill costs
almost nothing.


## Background and problem

A re-push sends a record's full current metadata to DataCite. The `post-to-datacite` path
already does this as a full-document PUT, so re-pushing is idempotent and creates no duplicate
identifiers.

The important insight for a reusable design: a record only ever needs to be flagged as "its
DataCite copy is stale, re-push it", never as "it needs correction X". The worker does not
need to know the reason. When it re-pushes, it sends the complete current representation,
which already includes whatever the latest change altered. Several pending corrections on the
same record simply coalesce into one re-push.

Two requirements shape the mechanism:

* Deployments must contain no manual steps.
* It must work in environments other than AWS, and under pipelines other than CodeBuild. This
  is the decisive constraint: it rules out anything that lives in the AWS pipeline, because
  the automation would then not run elsewhere. The only artifact that runs identically in
  every environment is the application, so the mechanism lives in the application.

The design must also:

* run once across instances, not once per instance per restart,
* not block startup or fail the deployment if DataCite is slow or unavailable,
* be self-terminating, so it stops working once nothing is stale, and
* be safe in production, so it can never mass-rewrite records unintentionally.


## Options considered

* A bespoke backfill per change (the pattern so far), run by hand. Fails the no-manual-step
  requirement, and repeats effort every time.
* An AWS pipeline deploy action (CodeBuild) or a one-shot ECS task. Not portable, because they
  are specific to AWS and CodeBuild.
* A Flyway migration that calls DataCite. Migrations here are plain SQL. A Java migration could
  make HTTP calls, but that would block startup and fail the application if DataCite were
  unavailable. The only suitable use of Flyway is deterministic SQL that flags the records.
* A reusable in-application re-sync mechanism. Recommended.


## Recommended approach

Build a reusable "DataCite re-sync queue" once, then drive each backfill with a small
migration.

Written once:

* A Flyway migration adds a flag column, `datacite_resync_required` (default false), to the
  raid table.
* The write path (mint, update, and `post-to-datacite`) clears the flag on every successful
  post, so new and edited records are never stale.
* A background worker drains the flagged records: on a schedule, it takes a Postgres advisory
  lock so only one instance runs, processes flagged records on a background thread, re-pushes
  each through the existing `post-to-datacite` path, clears the flag on success, rate-limits,
  retries, and logs counts. It is self-terminating: once nothing is flagged, every tick is a
  cheap no-op.
* A configuration property enables or disables the worker per environment.

Per backfill (including RAID-797), the entire change is a targeting migration:

```sql
-- RAID-797
update api_svc.raid r set datacite_resync_required = true
where exists (select 1 from api_svc.related_raid rr where rr.raid_name = r.handle);
```

A future correction ships its own one-line migration with its own predicate, and the existing
worker drains it. No new application code, no manual step, and it runs the same in every
environment.

Keep `scripts/backfill-datacite-related-raids.sh` as an operator fallback only.


## Consequences

Benefits:

* No manual deployment step, and the same behaviour in any environment and under any pipeline.
* Reusable. A future backfill is a one-line migration, not another bespoke job.
* Decoupled from startup and serving. A DataCite outage does not fail the deployment or the
  application; the worker makes no progress that run and retries later.
* Self-terminating and idempotent.
* Reuses existing application code and the application's own database for coordination.

Costs and constraints:

* Adds application-lifecycle complexity: a background executor, a Postgres advisory lock, and
  the flag column plus the write-path change that clears it.
* Introduces a pattern (application-driven data re-sync) that the team must maintain.
* The flag is the safety guard. Each backfill's predicate must be correct, especially in
  production, and the worker must log counts so a run is observable.


## Decisions

These are the decisions this proposal puts to the team to confirm.

* Flag shape and row selection. Use a boolean targeting flag, `datacite_resync_required`. Each
  backfill flags only its affected rows through its own migration predicate, so scope is
  declared per backfill and DataCite traffic stays minimal. We rejected the global generation
  counter (which re-pushes every record on each bump) and the `datacite_synced_at` timestamp
  (whose NULL-means-pending semantics are less clear).
* Write-path integration. The write path clears the flag on every successful DataCite post in
  `RaidService` (the mint, update, and `post-to-datacite` paths), so new and edited records are
  never stale. New mints default the column to false, so this matters most on user edits and on
  the worker's own re-push.
* Pacing. The worker drains in capped batches per tick: it processes up to `batchSize` records,
  releases the advisory lock between ticks, and continues on the schedule until drained. This
  bounds each tick's duration and DataCite load, and avoids holding the lock through a large
  backlog.
* Rate limit. DataCite documents a general limit of 3,000 requests per 5 minutes per IP, but a
  backfill is DataCite's "large-scale updates" case, for which they recommend 300 to 500
  requests per 5 minutes, and their test system caps at 750 per 5 minutes. So the default
  targets roughly 1 request per second (`throttleMillis` around 1000, about 300 per 5 minutes),
  which sits inside that recommendation and leaves headroom for normal minting traffic sharing
  the same per-IP budget. The worker handles a 429 by deferring the record to a later tick. The
  values stay configurable per environment.
* Status. This remains a spike proposal for now rather than an ADR. We can promote it to an ADR
  after the team agrees and the mechanism is implemented.

Still to tune (not blockers for the discussion): the exact `batchSize`, `throttleMillis`, and
`pollDelayMillis` defaults, within the DataCite guidance above.


## Links

* RAID-797, DataCite native RAiD relatedIdentifierType, and PR au-research/raid-au#617.
* RAID-796, parent story.
* Existing operator-fallback script: `scripts/backfill-datacite-related-raids.sh`.
* Related ADR: `doc/adr/2026-08-10_resolver-unavailable-503.md`, which follows the same
  principle of decoupling the application from external-service availability.
* DataCite API rate limits: https://support.datacite.org/docs/rate-limit and
  https://support.datacite.org/docs/best-practices-for-integrators.


## Appendix: mechanism sketch

This is an illustrative sketch for discussion, using Spring Boot and JOOQ to match the
api-svc conventions. Class and column names are indicative rather than the exact signatures.

The worker is a scheduled poller rather than a one-shot startup hook, so one mechanism covers
every case: the first tick fires at startup, later ticks retry failures and catch an instance
that was already running during a rolling deployment, and once nothing is flagged every tick
is a cheap no-op.

```java
@ConfigurationProperties(prefix = "raid.datacite.resync")
public record DataciteResyncProperties(
        boolean enabled,
        int batchSize,          // records per tick, e.g. 50
        long throttleMillis,    // pause between DataCite calls, e.g. 1000 (~1 req/s, see below)
        long pollDelayMillis) { // gap between ticks, e.g. 60000
}
```

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "raid.datacite.resync", name = "enabled", havingValue = "true")
public class DataciteResyncWorker {

    private static final Logger log = LoggerFactory.getLogger(DataciteResyncWorker.class);
    // A fixed key. Every instance uses the same one, so the lock is mutually exclusive.
    private static final long LOCK_KEY = 0x2126_0797L;

    private final DSLContext db;
    private final RaidResyncRepository repository;   // JOOQ queries over the flag column
    private final RaidService raidService;           // reuses the existing post-to-datacite path
    private final DataciteResyncProperties props;

    // Runs on a scheduler thread, so it never blocks startup or serving traffic.
    @Scheduled(fixedDelayString = "${raid.datacite.resync.poll-delay-millis:60000}")
    public void tick() {
        // Hold ONE connection for the whole tick, so the session advisory lock stays held.
        db.connection(conn -> {
            var dsl = DSL.using(conn);
            if (!tryAdvisoryLock(dsl)) {
                return; // another instance is already re-syncing; skip this tick
            }
            try {
                runBatch(dsl);
            } finally {
                releaseAdvisoryLock(dsl);
            }
        });
    }

    private void runBatch(DSLContext dsl) {
        var pending = repository.findResyncRequired(dsl, props.batchSize());
        if (pending.isEmpty()) {
            return; // self-terminating: nothing flagged, so the tick is a no-op
        }
        log.info("DataCite re-sync: re-pushing {} record(s) this tick", pending.size());

        int synced = 0, deferred = 0;
        for (var handle : pending) {
            try {
                raidService.postToDatacite(handle);        // idempotent full-document PUT
                repository.clearResyncRequired(dsl, handle);// own statement, no long transaction
                synced++;
            } catch (Exception e) {
                deferred++;
                // Leave the flag set so a later tick retries it. Do not abort the run.
                log.warn("DataCite re-sync: {} deferred, will retry: {}", handle, e.getMessage());
            }
            sleepQuietly(props.throttleMillis());          // rate-limit against the DataCite API
        }
        log.info("DataCite re-sync tick done: {} synced, {} deferred", synced, deferred);
    }

    private boolean tryAdvisoryLock(DSLContext dsl) {
        return Boolean.TRUE.equals(
            dsl.select(field("pg_try_advisory_lock({0})", Boolean.class, val(LOCK_KEY)))
               .fetchOne(0, Boolean.class));
    }

    private void releaseAdvisoryLock(DSLContext dsl) {
        dsl.execute("select pg_advisory_unlock({0})", val(LOCK_KEY));
    }

    private static void sleepQuietly(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

The worker is fully generic, because the "which records" decision lives in each backfill's
migration, not in the worker.

```java
// JOOQ repository (names indicative)
List<String> findResyncRequired(DSLContext dsl, int limit) {
    return dsl.select(RAID.HANDLE).from(RAID)
        .where(RAID.DATACITE_RESYNC_REQUIRED.isTrue())
        .orderBy(RAID.HANDLE).limit(limit).fetch(RAID.HANDLE);
}

void clearResyncRequired(DSLContext dsl, String handle) {
    dsl.update(RAID).set(RAID.DATACITE_RESYNC_REQUIRED, false)
       .where(RAID.HANDLE.eq(handle)).execute();
}
```

The mechanism is created once by a Flyway migration, and the write path clears the flag on
every successful post so new and edited records are never stale:

```sql
-- Vxx__datacite_resync_queue.sql  (the reusable mechanism, added once)
alter table api_svc.raid add column datacite_resync_required boolean not null default false;
```

Each backfill is then a targeting migration:

```sql
-- Vyy__resync_datacite_for_raid_797.sql  (the first user)
update api_svc.raid r set datacite_resync_required = true
where exists (select 1 from api_svc.related_raid rr where rr.raid_name = r.handle);

-- A future correction ships its own one-line migration with its own predicate:
-- update api_svc.raid set datacite_resync_required = true where <predicate>;
```

Design points worth noting for the discussion:

* The advisory lock on a single held connection (`db.connection(...)`) makes the worker
  single-runner across instances without any external coordinator. `pg_try_advisory_lock` is
  non-blocking, so a losing instance skips the tick.
* DataCite calls sit outside any database transaction. `clearResyncRequired` is its own
  statement per record, so there is no long-running transaction, and a crash mid-run leaves
  the remainder flagged for the next tick.
* Failure is handled per record, never fatal to the run or the application. A record whose
  flag is still set is retried on a later tick.
* The write path clears the flag on every successful post, so a record edited by a user before
  the worker reaches it is not re-pushed redundantly.
* The toggle (`@ConditionalOnProperty`) keeps the worker off in tests and local runs, and lets
  us disable it per environment. Scheduling requires `@EnableScheduling` on the application.
