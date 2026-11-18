package au.org.raid.api.service;

import au.org.raid.api.config.properties.ContributorStatusRefreshProperties;
import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorStatusRefreshRepository;
import au.org.raid.api.repository.ContributorStatusResetRepository;
import au.org.raid.api.repository.DataciteResyncRepository;
import au.org.raid.api.repository.PostgresAdvisoryLock;
import au.org.raid.api.service.raid.RaidService;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * A one-off catch-up that runs after the release introducing per-agency ORCID updaters. Normal
 * status changes don't need it: {@link ContributorStatusService} keeps each contributor's current
 * status and the status recorded in every RAiD naming them in step.
 *
 * <p>It works in two stages, finishing the first before starting the second:
 * <ol>
 *   <li><b>Reset.</b> Each contributor listed in {@code contributor_status_reset} (authenticated
 *   through the central ORCID integration) is set to UNAUTHENTICATED through
 *   {@link ContributorStatusService}, so every RAiD naming them gets a new version and a DataCite
 *   update in the same step. A contributor the ORCID updater has since given another status is left
 *   with it.</li>
 *   <li><b>DOIs.</b> Each RAiD flagged in {@code raid.contributor_status_refresh_required} is brought
 *   in step: a new version where the status recorded in the RAiD differs from the contributor's
 *   current status, and a DataCite update alone where it doesn't, so its DOI lists only the
 *   creators the current rule allows. If that DataCite update fails, the RAiD is flagged for the
 *   DataCite resync worker.</li>
 * </ol>
 *
 * <p>Runs on the scheduler thread. One instance at a time does the work, coordinated by a Postgres
 * advisory lock. Anything that fails is retried on a later tick. Once both stages are done, a tick
 * is a cheap no-op. Repeating any step changes nothing the second time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "raid.contributor-status-refresh", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class ContributorStatusRefreshWorker {
    /** Arbitrary but stable, and distinct from the DataCite resync worker's key. */
    static final long LOCK_KEY = 0x2126_1064L;
    static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    /**
     * Statuses a recorded contributor can hold before their reset is finished: the original, or the
     * reset status itself after a reset that only partly succeeded. Any other status was set by the
     * ORCID updater since, and stands.
     */
    private static final Set<String> RESETTABLE = Set.of("AUTHENTICATED", UNAUTHENTICATED);

    private final PostgresAdvisoryLock advisoryLock;
    private final ContributorStatusResetRepository resetRepository;
    private final ContributorRepository contributorRepository;
    private final ContributorStatusService contributorStatusService;
    private final ContributorStatusRefreshRepository repository;
    private final DataciteResyncRepository dataciteResyncRepository;
    private final RaidService raidService;
    private final ContributorStatusRefreshProperties properties;

    @Scheduled(fixedDelayString = "${raid.contributor-status-refresh.poll-delay-millis:60000}")
    public void tick() {
        try {
            advisoryLock.runExclusively(LOCK_KEY, this::runBatch);
        } catch (final Exception e) {
            log.warn("Contributor status refresh: tick failed, will retry next poll", e);
        }
    }

    void runBatch() {
        final var resets = resetRepository.findPending(properties.getBatchSize());
        if (!resets.isEmpty()) {
            resetContributors(resets);
            // DOIs wait until every reset is done, so none is updated from a status about to change.
            return;
        }

        refreshRaids();
    }

    private void resetContributors(final List<String> pids) {
        int reset = 0;
        int kept = 0;
        int deferred = 0;

        for (final var pid : pids) {
            try {
                final var status = contributorRepository.findByPid(pid)
                        .map(ContributorRecord::getStatus)
                        .orElse(null);
                if (status != null && RESETTABLE.contains(status)) {
                    contributorStatusService.updateStatus(pid, UNAUTHENTICATED);
                    reset++;
                } else {
                    kept++;
                }
                resetRepository.markReset(pid);
            } catch (final Exception e) {
                log.warn("Contributor status reset: a contributor was deferred, will retry: {}", e.getMessage());
                deferred++;
            }
            sleepQuietly(properties.getThrottleMillis());
        }

        log.info("Contributor status reset: {} reset, {} kept a status the updater set, {} deferred",
                reset, kept, deferred);
    }

    private void refreshRaids() {
        final var pending = repository.findRefreshRequired(properties.getBatchSize());
        if (pending.isEmpty()) {
            return;
        }

        int versioned = 0;
        int dataciteOnly = 0;
        int deferred = 0;
        int skipped = 0;

        for (final var handle : pending) {
            try {
                if (raidService.refreshContributorStatus(handle)) {
                    versioned++;
                } else {
                    resyncDatacite(handle);
                    dataciteOnly++;
                }
                repository.clearRefreshRequired(handle);
            } catch (final ResourceNotFoundException e) {
                // No saved version to bring up to date, so retrying can't help.
                log.warn("Contributor status refresh: {} has no saved version; skipped", handle);
                repository.clearRefreshRequired(handle);
                skipped++;
            } catch (final Exception e) {
                log.warn("Contributor status refresh: {} deferred, will retry: {}", handle, e.getMessage());
                deferred++;
            }
            sleepQuietly(properties.getThrottleMillis());
        }

        log.info("Contributor status refresh: {} new versions, {} DataCite only, {} skipped, {} deferred",
                versioned, dataciteOnly, skipped, deferred);
    }

    private void resyncDatacite(final String handle) {
        try {
            raidService.resyncWithDatacite(handle);
            dataciteResyncRepository.clearResyncRequired(handle);
        } catch (final RuntimeException e) {
            log.warn("Contributor status refresh: DataCite update failed for {}; flagged for resync: {}",
                    handle, e.getMessage());
            dataciteResyncRepository.markResyncRequired(handle);
        }
    }

    private static void sleepQuietly(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
