package au.org.raid.api.service;

import au.org.raid.api.repository.RaidContributorRepository;
import au.org.raid.api.service.raid.RaidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Applies a contributor status set by the registration agency's ORCID updater. Status belongs to the
 * person, so the stored contributor row changes first, then every RAiD naming the person is brought up
 * to date in its own transaction: one RAiD that fails doesn't hold back the others.
 *
 * <p>Deliberately not transactional, so each RAiD commits on its own. Safe to repeat: RAiDs that already
 * hold the status are left alone, which is what lets the updater retry after a failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorStatusService {
    private final ContributorService contributorService;
    private final RaidContributorRepository raidContributorRepository;
    private final RaidService raidService;

    /**
     * @return false when no contributor has this id
     * @throws ContributorStatusRefreshException when one or more RAiDs couldn't be brought up to date;
     *                                           the others are saved, and a retry finishes the rest
     */
    public boolean updateStatus(final String pid, final String status) {
        if (!contributorService.updateStatus(pid, status)) {
            return false;
        }

        final var handles = raidContributorRepository.findHandlesByContributorPid(pid);
        var versioned = 0;
        var failed = 0;

        for (final var handle : handles) {
            try {
                if (raidService.refreshContributorStatus(handle)) {
                    versioned++;
                }
            } catch (final RuntimeException e) {
                failed++;
                log.error("Could not apply a contributor status change to RAiD {}", handle, e);
            }
        }

        log.info("Contributor status set to {}: {} RAiD(s) named the contributor, {} given a new version, {} failed",
                status, handles.size(), versioned, failed);

        if (failed > 0) {
            throw new ContributorStatusRefreshException(failed, handles.size());
        }

        return true;
    }

    public static class ContributorStatusRefreshException extends RuntimeException {
        public ContributorStatusRefreshException(final int failed, final int total) {
            super("%d of %d RAiD(s) could not be updated with the contributor's new status".formatted(failed, total));
        }
    }
}
