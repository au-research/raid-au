package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.work.WorkRepository.Kind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Asks the background worker to do something (RAID-1005). Called inside the caller's transaction
 * where there is one, so the request is stored only if the change that prompted it is.
 */
@Component
@RequiredArgsConstructor
public class WorkQueue {
    private final WorkRepository workRepository;
    private final Clock clock;

    /**
     * Something about this researcher's status may have changed. The worker works out the current
     * status itself, so a stale or repeated request never pushes a wrong status.
     */
    public void requestStatus(final String orcidId) {
        workRepository.request(Kind.STATUS, orcidId, clock.instant());
    }

    /** Bring this RAiD's ORCID entries in step with the RAiD. */
    public void requestRaidSync(final String handle) {
        workRepository.request(Kind.RAID_SYNC, handle, clock.instant());
    }
}
