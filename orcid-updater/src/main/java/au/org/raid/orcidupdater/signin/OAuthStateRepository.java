package au.org.raid.orcidupdater.signin;

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
public class OAuthStateRepository {
    private final JdbcClient jdbcClient;

    /** Also clears abandoned sign-ins, so the table only holds ones still in progress. */
    public void insert(final byte[] stateHash, final UUID invitationId, final byte[] emailHash,
                       final Instant createdAt, final Instant expiresAt) {
        jdbcClient.sql("DELETE FROM oauth_state WHERE expires_at <= :now")
                .param("now", utc(createdAt))
                .update();
        jdbcClient.sql("""
                        INSERT INTO oauth_state (state_hash, invitation_id, email_hash, created_at, expires_at)
                        VALUES (:stateHash, :invitationId, :emailHash, :createdAt, :expiresAt)
                        """)
                .param("stateHash", stateHash)
                .param("invitationId", invitationId)
                .param("emailHash", emailHash)
                .param("createdAt", utc(createdAt))
                .param("expiresAt", utc(expiresAt))
                .update();
    }

    /**
     * Deletes and returns the sign-in for this state in one statement, so a callback can't be
     * replayed and two callbacks with the same state can't both proceed. Empty if unknown.
     */
    public Optional<OAuthState> consume(final byte[] stateHash) {
        return jdbcClient.sql("""
                        DELETE FROM oauth_state WHERE state_hash = :stateHash
                        RETURNING invitation_id, email_hash, expires_at
                        """)
                .param("stateHash", stateHash)
                .query((rs, row) -> new OAuthState(rs.getObject("invitation_id", UUID.class),
                        rs.getBytes("email_hash"), rs.getObject("expires_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    /** Drops sign-ins in progress that carry this email fingerprint (RAID-1006). @return how many */
    public int deleteByEmailHash(final byte[] emailHash) {
        return jdbcClient.sql("DELETE FROM oauth_state WHERE email_hash = :emailHash")
                .param("emailHash", emailHash)
                .update();
    }

    private static OffsetDateTime utc(final Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    /** emailHash is null unless the researcher ticked "Remember me". */
    public record OAuthState(UUID invitationId, byte[] emailHash, Instant expiresAt) {
    }
}
