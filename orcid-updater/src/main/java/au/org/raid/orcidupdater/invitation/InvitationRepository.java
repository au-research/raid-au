package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.raidapi.RaidSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InvitationRepository {
    private static final String SELECT_INVITATION = """
            SELECT id, orcid_id, raid_handle, raid_title, service_point_name, expires_at, used_at FROM invitation""";
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

    public void insert(final UUID id, final byte[] tokenHash, final String orcidId, final RaidSummary raid,
                       final Instant createdAt, final Instant expiresAt) {
        jdbcClient.sql("""
                        INSERT INTO invitation (id, token_hash, orcid_id, raid_handle, raid_title, service_point_name,
                                                created_at, expires_at)
                        VALUES (:id, :tokenHash, :orcidId, :raidHandle, :raidTitle, :servicePointName,
                                :createdAt, :expiresAt)
                        """)
                .param("id", id)
                .param("tokenHash", tokenHash)
                .param("orcidId", orcidId)
                .param("raidHandle", raid.handle())
                .param("raidTitle", raid.title())
                .param("servicePointName", raid.servicePointName())
                .param("createdAt", utc(createdAt))
                .param("expiresAt", utc(expiresAt))
                .update();
    }

    public Optional<Invitation> findByTokenHash(final byte[] tokenHash) {
        return jdbcClient.sql(SELECT_INVITATION + " WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .query(InvitationRepository::invitation)
                .optional();
    }

    public Optional<Invitation> findById(final UUID id) {
        return jdbcClient.sql(SELECT_INVITATION + " WHERE id = :id")
                .param("id", id)
                .query(InvitationRepository::invitation)
                .optional();
    }

    /**
     * Uses up the invitation and records the iD ORCID returned. False if it was already used, so
     * one link can't authenticate twice even if two callbacks race.
     */
    public boolean markUsed(final UUID id, final String authenticatedOrcidId, final Instant usedAt) {
        return jdbcClient.sql("""
                        UPDATE invitation SET used_at = :usedAt, authenticated_orcid_id = :authenticatedOrcidId
                        WHERE id = :id AND used_at IS NULL
                        """)
                .param("id", id)
                .param("authenticatedOrcidId", authenticatedOrcidId)
                .param("usedAt", utc(usedAt))
                .update() == 1;
    }

    private static Invitation invitation(final ResultSet rs, final int row) throws SQLException {
        final var usedAt = rs.getObject("used_at", OffsetDateTime.class);
        return new Invitation(rs.getObject("id", UUID.class), rs.getString("orcid_id"), rs.getString("raid_handle"),
                rs.getString("raid_title"), rs.getString("service_point_name"),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                usedAt == null ? null : usedAt.toInstant());
    }

    private static OffsetDateTime utc(final Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
