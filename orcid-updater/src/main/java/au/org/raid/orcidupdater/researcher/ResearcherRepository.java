package au.org.raid.orcidupdater.researcher;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ResearcherRepository {
    private final JdbcClient jdbcClient;

    /**
     * The iD of the researcher who ticked "Remember me" with this email fingerprint. Only rows
     * with consent carry a fingerprint (enforced by the researcher_consent_recorded constraint).
     */
    public Optional<String> findOrcidIdByConsentedEmailHash(final byte[] emailHash) {
        // a researcher who revoked access at ORCID must be invited again, not matched
        return jdbcClient.sql("SELECT orcid_id FROM researcher WHERE email_hash = :emailHash AND revoked_at IS NULL")
                .param("emailHash", emailHash)
                .query(String.class)
                .optional();
    }

    /** The researcher who consented with this email fingerprint, revoked or not (RAID-1006). */
    public Optional<String> findOrcidIdByEmailHash(final byte[] emailHash) {
        return jdbcClient.sql("SELECT orcid_id FROM researcher WHERE email_hash = :emailHash")
                .param("emailHash", emailHash)
                .query(String.class)
                .optional();
    }

    /** Deletes the tokens, consent and email fingerprint. @return whether there was a row */
    public boolean delete(final String orcidId) {
        return jdbcClient.sql("DELETE FROM researcher WHERE orcid_id = :orcidId")
                .param("orcidId", orcidId)
                .update() > 0;
    }

    /** The encrypted access token, when the researcher has authenticated and not revoked access. */
    public Optional<byte[]> findAccessToken(final String orcidId) {
        return jdbcClient.sql("""
                        SELECT access_token FROM researcher
                        WHERE orcid_id = :orcidId AND access_token IS NOT NULL AND revoked_at IS NULL
                        """)
                .param("orcidId", orcidId)
                .query(byte[].class)
                .optional();
    }

    /** Whether ORCID rejected this researcher's token and they haven't signed in since. */
    public boolean isRevoked(final String orcidId) {
        return jdbcClient.sql("SELECT revoked_at IS NOT NULL FROM researcher WHERE orcid_id = :orcidId")
                .param("orcidId", orcidId)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    /** ORCID rejected the token. No more writes for this iD until the researcher signs in again. */
    public void markRevoked(final String orcidId, final Instant revokedAt) {
        jdbcClient.sql("UPDATE researcher SET revoked_at = :revokedAt WHERE orcid_id = :orcidId AND revoked_at IS NULL")
                .param("revokedAt", revokedAt.atOffset(ZoneOffset.UTC))
                .param("orcidId", orcidId)
                .update();
    }

    /**
     * Stores the tokens from an ORCID sign-in, replacing any earlier ones for this iD and clearing
     * a revocation. An earlier "Remember me" consent is left as it is: signing in again without
     * ticking the box is not a request to delete it (that is {@link #delete}).
     */
    public void saveAuthentication(final String orcidId, final byte[] accessToken, final byte[] refreshToken,
                                   final String scope, final Instant authenticatedAt) {
        jdbcClient.sql("""
                        INSERT INTO researcher (orcid_id, access_token, refresh_token, scope, authenticated_at)
                        VALUES (:orcidId, :accessToken, :refreshToken, :scope, :authenticatedAt)
                        ON CONFLICT (orcid_id) DO UPDATE SET access_token = excluded.access_token,
                                                             refresh_token = excluded.refresh_token,
                                                             scope = excluded.scope,
                                                             authenticated_at = excluded.authenticated_at,
                                                             revoked_at = NULL
                        """)
                .param("orcidId", orcidId)
                .param("accessToken", accessToken)
                .param("refreshToken", refreshToken)
                .param("scope", scope)
                .param("authenticatedAt", authenticatedAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    /**
     * Records "Remember me" for an authenticated researcher. A fingerprint belongs to one person,
     * so if someone else consented with the same email earlier, the latest consent wins and the
     * earlier one is removed.
     */
    public void recordConsent(final String orcidId, final byte[] emailHash, final Instant consentedAt) {
        jdbcClient.sql("""
                        UPDATE researcher SET email_hash = NULL, consented_at = NULL
                        WHERE email_hash = :emailHash AND orcid_id <> :orcidId
                        """)
                .param("emailHash", emailHash)
                .param("orcidId", orcidId)
                .update();
        jdbcClient.sql("UPDATE researcher SET email_hash = :emailHash, consented_at = :consentedAt WHERE orcid_id = :orcidId")
                .param("emailHash", emailHash)
                .param("consentedAt", consentedAt.atOffset(ZoneOffset.UTC))
                .param("orcidId", orcidId)
                .update();
    }
}
