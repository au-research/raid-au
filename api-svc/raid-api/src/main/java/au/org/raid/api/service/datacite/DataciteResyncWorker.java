package au.org.raid.api.service.datacite;

import au.org.raid.api.config.properties.DataciteResyncProperties;
import au.org.raid.api.repository.DataciteResyncRepository;
import au.org.raid.api.service.raid.RaidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.using;
import static org.jooq.impl.DSL.val;

/**
 * Reusable DataCite re-sync mechanism (RAID-832). Periodically drains raids flagged via
 * {@code raid.datacite_resync_required} and re-pushes each through the existing idempotent
 * DataCite full-document PUT ({@link RaidService#resyncWithDatacite(String)}), so a
 * correction to how RAiD metadata maps to DataCite (e.g. RAID-797) can be rolled out to
 * already-minted records with a one-line targeting migration rather than a bespoke,
 * hand-run backfill. See {@code doc/spike/RAID-797-datacite-backfill-automation.md}.
 *
 * <p>Runs on the scheduler thread, never blocking startup or request-serving traffic. Only
 * one application instance re-syncs at a time, coordinated by a Postgres session-level
 * advisory lock held on a single dedicated connection for the whole tick; an instance that
 * doesn't acquire the lock skips the tick as a no-op. Failures are handled per record: a
 * record that fails to re-push (including a DataCite 429) simply keeps its flag set for a
 * later tick, and processing continues with the remaining records. Self-terminating: once
 * nothing is flagged, a tick is a cheap no-op.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "raid.datacite.resync", name = "enabled", havingValue = "true")
public class DataciteResyncWorker {

    /**
     * A fixed advisory lock key, arbitrary but stable. Every instance uses the same key, so
     * the lock is mutually exclusive across the fleet regardless of which instance holds it.
     */
    private static final long LOCK_KEY = 0x2126_0832L;

    private final DSLContext db;
    private final DataciteResyncRepository repository;
    private final RaidService raidService;
    private final DataciteResyncProperties properties;

    @Scheduled(fixedDelayString = "${raid.datacite.resync.poll-delay-millis:60000}")
    public void tick() {
        try {
            db.connection(conn -> {
                final var dsl = using(conn);
                if (!tryAdvisoryLock(dsl)) {
                    // Another instance is already re-syncing this tick; skip, don't wait.
                    return;
                }
                try {
                    runBatch();
                } finally {
                    releaseAdvisoryLock(dsl);
                }
            });
        } catch (final Exception e) {
            // Never let a tick fail the scheduler thread; a later tick will retry.
            log.warn("DataCite re-sync: tick failed, will retry next poll", e);
        }
    }

    void runBatch() {
        final var pending = repository.findResyncRequired(properties.getBatchSize());

        if (pending.isEmpty()) {
            // Self-terminating: nothing flagged, so this tick is a cheap, silent no-op.
            return;
        }

        log.info("DataCite re-sync: re-pushing {} record(s) this tick", pending.size());

        int synced = 0;
        int deferred = 0;

        for (final var handle : pending) {
            if (resyncOne(handle)) {
                synced++;
            } else {
                deferred++;
            }
            sleepQuietly(properties.getThrottleMillis());
        }

        log.info("DataCite re-sync tick done: {} synced, {} deferred", synced, deferred);
    }

    private boolean resyncOne(final String handle) {
        try {
            raidService.resyncWithDatacite(handle);
            repository.clearResyncRequired(handle);
            return true;
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("DataCite re-sync: {} rate limited, deferring", handle, e);
            } else {
                log.warn("DataCite re-sync: {} deferred, will retry: {}", handle, e.getMessage());
            }
            return false;
        } catch (final Exception e) {
            // Leave the flag set so a later tick retries it. Never abort the run.
            log.warn("DataCite re-sync: {} deferred, will retry: {}", handle, e.getMessage());
            return false;
        }
    }

    private boolean tryAdvisoryLock(final DSLContext dsl) {
        return Boolean.TRUE.equals(
                dsl.select(field("pg_try_advisory_lock({0})", Boolean.class, val(LOCK_KEY)))
                        .fetchOne(0, Boolean.class));
    }

    private void releaseAdvisoryLock(final DSLContext dsl) {
        dsl.execute("select pg_advisory_unlock({0})", val(LOCK_KEY));
    }

    private static void sleepQuietly(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
