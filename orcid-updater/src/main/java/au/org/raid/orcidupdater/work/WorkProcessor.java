package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.orcid.OrcidRaid;
import au.org.raid.orcidupdater.orcid.OrcidWriter;
import au.org.raid.orcidupdater.raidapi.RaidApiUnavailableException;
import au.org.raid.orcidupdater.raidapi.RaidServiceClient;
import au.org.raid.orcidupdater.serviceauth.ServiceTokenException;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.status.ContributorStatusService;
import au.org.raid.orcidupdater.work.WorkRepository.WorkItem;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Works through the queue (RAID-1005): pushes statuses to the RAiD API and brings RAiDs' ORCID
 * entries in step. When the RAiD API or Keycloak can't be reached, the item is tried again later,
 * waiting longer each time. A failed ORCID write is not retried here: the writer records it on the
 * entry, and the next change to the RAiD tries again (Decision Q6 on RAID-975).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkProcessor {
    static final String OPEN_ACCESS = "https://vocabularies.coar-repositories.org/access_rights/c_abf2/";
    /** Bounds the doubling, so a large max-attempts setting can't overflow the wait. */
    private static final int MAX_BACKOFF_DOUBLINGS = 16;

    private final WorkRepository workRepository;
    private final WorkQueue workQueue;
    private final RaidServiceClient raidServiceClient;
    private final ContributorStatusService statusService;
    private final OrcidWriter orcidWriter;
    private final OrcidUpdaterProperties properties;
    private final Clock clock;

    /** @return how many items were taken, so the caller can carry on while the queue is full */
    public int processDue() {
        final var work = properties.getWork();
        final var now = clock.instant();
        final var expired = workRepository.requestStatusForExpiredInvitations(now);
        if (expired > 0) {
            log.info("{} expired invitation(s) queued as status changes", expired);
        }
        final var items = workRepository.claimDue(now, now.plus(work.getClaimTimeout()), work.getBatchSize());
        items.forEach(this::process);
        return items.size();
    }

    void process(final WorkItem item) {
        try {
            switch (item.kind()) {
                case STATUS -> pushStatus(item.key());
                case RAID_SYNC -> syncRaid(item.key());
            }
            workRepository.complete(item);
        } catch (final RaidApiUnavailableException | ServiceTokenException e) {
            retryLater(item, e.getMessage());
        } catch (final RuntimeException e) {
            // unexpected: the message could hold anything, so only the type is stored
            log.error("{} {}: unexpected failure", item.kind(), item.key(), e);
            retryLater(item, "Unexpected " + e.getClass().getSimpleName() + "; see the updater's log");
        }
    }

    private void pushStatus(final String orcidId) {
        final var status = statusService.statusOf(orcidId);
        if (!raidServiceClient.updateContributorStatus(orcidId, status)) {
            log.info("Status {} for {} not set: the RAiD API has no contributor with this iD", status, orcidId);
        } else {
            log.info("Status {} set for {}", status, orcidId);
        }
        if (status == ContributorStatus.AUTHENTICATED) {
            // newly authenticated: write every RAiD that already names them
            final var handles = raidServiceClient.findHandlesByContributor(orcidId);
            handles.forEach(workQueue::requestRaidSync);
            log.info("{} is authenticated; {} RAiD(s) queued for ORCID", orcidId, handles.size());
        }
    }

    private void syncRaid(final String handle) {
        final var raid = raidServiceClient.findRaid(handle);
        if (raid.isEmpty()) {
            log.warn("RAiD {} not found in the RAiD API; nothing written to ORCID", handle);
            return;
        }
        if (!isOpen(raid.get())) {
            // written when the embargo ends: the RAiD API's access handler saves the RAiD as open,
            // and that save notifies the updater again
            log.info("RAiD {} is not open access; held back from ORCID", handle);
            return;
        }
        final var outcomes = orcidWriter.sync(OrcidRaid.fromApi(handle, raid.get()));
        log.info("RAiD {} synced to ORCID: {}", handle, outcomes);
    }

    private static boolean isOpen(final JsonNode raid) {
        return OPEN_ACCESS.equals(raid.path("access").path("type").path("id").asText(null));
    }

    private void retryLater(final WorkItem item, final String failure) {
        final var work = properties.getWork();
        final var now = clock.instant();
        final Instant nextAttemptAt;
        if (item.attempts() + 1 >= work.getMaxAttempts()) {
            nextAttemptAt = null;
            log.error("{} {}: gave up after {} attempts: {}", item.kind(), item.key(), item.attempts() + 1, failure);
        } else {
            nextAttemptAt = now.plus(work.getInitialBackoff()
                    .multipliedBy(1L << Math.min(item.attempts(), MAX_BACKOFF_DOUBLINGS)));
            log.warn("{} {}: attempt {} failed, trying again at {}: {}",
                    item.kind(), item.key(), item.attempts() + 1, nextAttemptAt, failure);
        }
        workRepository.recordFailure(item, failure, now, nextAttemptAt);
    }
}
