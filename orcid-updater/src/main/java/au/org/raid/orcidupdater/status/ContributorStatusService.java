package au.org.raid.orcidupdater.status;

import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * A researcher's status, worked out from what the updater holds (RAID-1005). The updater is the
 * authority; the RAiD API keeps a display copy that the worker pushes to it.
 */
@Service
@RequiredArgsConstructor
public class ContributorStatusService {
    private final ResearcherRepository researcherRepository;
    private final InvitationRepository invitationRepository;
    private final Clock clock;

    /**
     * Contract schema ContributorStatusResponse. {@code authenticatedId} is set when the status is
     * AUTHENTICATED: the iD itself, or the iD the researcher signed in as when it differs from the
     * one the minter typed.
     */
    public record Status(String id, ContributorStatus status, String authenticatedId) {
    }

    /**
     * The status of this iD itself, as pushed to the RAiD API. An authenticated token comes
     * first; then a pending invitation, so someone who revoked access and was invited again is
     * waiting, not revoked.
     */
    public ContributorStatus statusOf(final String orcidId) {
        if (researcherRepository.findAccessToken(orcidId).isPresent()) {
            return ContributorStatus.AUTHENTICATED;
        }
        if (invitationRepository.findPendingExpiry(orcidId, clock.instant()).isPresent()) {
            return ContributorStatus.AWAITING_AUTHENTICATION;
        }
        if (researcherRepository.isRevoked(orcidId)) {
            return ContributorStatus.AUTHENTICATION_REVOKED;
        }
        return ContributorStatus.UNAUTHENTICATED;
    }

    /**
     * For GET /contributors/status. Unlike {@link #statusOf}, an iD whose invitation was used to
     * sign in as a different, still authenticated iD reports AUTHENTICATED with that iD, so the
     * RAiD API can replace the typed one (RAID-976).
     */
    public Status lookup(final String orcidId) {
        final var own = statusOf(orcidId);
        if (own == ContributorStatus.AUTHENTICATED) {
            return new Status(orcidId, own, orcidId);
        }
        return invitationRepository.findOtherAuthenticatedOrcidId(orcidId)
                .filter(other -> researcherRepository.findAccessToken(other).isPresent())
                .map(other -> new Status(orcidId, ContributorStatus.AUTHENTICATED, other))
                .orElse(new Status(orcidId, own, null));
    }
}
