package au.org.raid.api.service;

import au.org.raid.api.config.properties.ContributorStatusResetProperties;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorStatusResetRepository;
import au.org.raid.api.repository.PostgresAdvisoryLock;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * A one-off reset that runs after the release introducing per-agency ORCID updaters. Each
 * contributor listed in {@code contributor_status_reset} (authenticated through the central ORCID
 * integration) is set to UNAUTHENTICATED through {@link ContributorStatusService}, the same path as a
 * status change from the ORCID updater, so every RAiD naming them gets a new version in the same
 * step. A contributor the ORCID updater has since given another status is left with it.
 *
 * <p>Runs on the scheduler thread. One instance at a time does the work, coordinated by a Postgres
 * advisory lock. A reset that fails is retried on a later tick. Once every listed contributor is
 * done, a tick is a cheap no-op. Repeating a reset changes nothing the second time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "raid.contributor-status-reset", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class ContributorStatusResetWorker {
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
    private final ContributorStatusResetProperties properties;

    @Scheduled(fixedDelayString = "${raid.contributor-status-reset.poll-delay-millis:60000}")
    public void tick() {
        try {
            advisoryLock.runExclusively(LOCK_KEY, this::runBatch);
        } catch (final Exception e) {
            log.warn("Contributor status reset: tick failed, will retry next poll", e);
        }
    }

    void runBatch() {
        final var pids = resetRepository.findPending(properties.getBatchSize());
        if (pids.isEmpty()) {
            return;
        }

        resetContributors(pids);
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

    private static void sleepQuietly(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
