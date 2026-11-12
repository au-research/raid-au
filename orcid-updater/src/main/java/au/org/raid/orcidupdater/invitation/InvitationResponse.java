package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.status.ContributorStatus;

import java.time.Instant;

/**
 * Body of the POST /invitations response.
 *
 * @param contributorId the iD the updater holds for this researcher; differs from the requested
 *                      iD when a "Remember me" match returns the researcher's authenticated iD
 * @param expiresAt     when the pending invitation expires; null unless AWAITING_AUTHENTICATION
 */
public record InvitationResponse(String contributorId, ContributorStatus status, boolean emailSent,
                                 Instant expiresAt) {
}
