package au.org.raid.orcidupdater.invitation;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InvitationRepository {
    private final JdbcClient jdbcClient;

    /**
     * Serialises invitation registration per ORCID iD until the transaction ends, so two saves at
     * the same moment can't both find nothing pending and send two emails.
     */
    public void lockOrcidId(final String orcidId) {
        jdbcClient.sql("SELECT pg_advisory_xact_lock(hashtextextended(:orcidId, 0))")
                .param("orcidId", orcidId)
                .query()
                .singleValue();
    }

    /** Expiry of the newest unused, unexpired invitation for this iD. */
    public Optional<Instant> findPendingExpiry(final String orcidId, final Instant now) {
        return jdbcClient.sql("""
                        SELECT expires_at FROM invitation
                        WHERE orcid_id = :orcidId AND used_at IS NULL AND expires_at > :now
                        ORDER BY expires_at DESC
                        LIMIT 1
                        """)
                .param("orcidId", orcidId)
                .param("now", utc(now))
                .query(OffsetDateTime.class)
                .optional()
                .map(OffsetDateTime::toInstant);
    }

    public void insert(final UUID id, final byte[] tokenHash, final String orcidId, final String raidHandle,
                       final Instant createdAt, final Instant expiresAt) {
        jdbcClient.sql("""
                        INSERT INTO invitation (id, token_hash, orcid_id, raid_handle, created_at, expires_at)
                        VALUES (:id, :tokenHash, :orcidId, :raidHandle, :createdAt, :expiresAt)
                        """)
                .param("id", id)
                .param("tokenHash", tokenHash)
                .param("orcidId", orcidId)
                .param("raidHandle", raidHandle)
                .param("createdAt", utc(createdAt))
                .param("expiresAt", utc(expiresAt))
                .update();
    }

    private static OffsetDateTime utc(final Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
