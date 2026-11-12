package au.org.raid.orcidupdater.invitation;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored invitation. Title and service point name are null for invitations registered before
 * RAID-1003 stored them.
 */
public record Invitation(UUID id, String orcidId, String raidHandle, String raidTitle, String servicePointName,
                         Instant expiresAt, Instant usedAt) {

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpiredAt(final Instant now) {
        return !expiresAt.isAfter(now);
    }
}
