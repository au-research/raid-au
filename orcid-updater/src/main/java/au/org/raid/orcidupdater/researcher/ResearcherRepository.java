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
        // IMPROVE: RAID-1004 must also skip researchers whose access ORCID has revoked
        return jdbcClient.sql("SELECT orcid_id FROM researcher WHERE email_hash = :emailHash")
                .param("emailHash", emailHash)
                .query(String.class)
                .optional();
    }

    /**
     * Stores the tokens from an ORCID sign-in, replacing any earlier ones for this iD. An earlier
     * "Remember me" consent is left as it is: signing in again without ticking the box is not a
     * request to delete it (that is RAID-1006).
     */
    public void saveAuthentication(final String orcidId, final byte[] accessToken, final byte[] refreshToken,
                                   final String scope, final Instant authenticatedAt) {
        jdbcClient.sql("""
                        INSERT INTO researcher (orcid_id, access_token, refresh_token, scope, authenticated_at)
                        VALUES (:orcidId, :accessToken, :refreshToken, :scope, :authenticatedAt)
                        ON CONFLICT (orcid_id) DO UPDATE SET access_token = excluded.access_token,
                                                             refresh_token = excluded.refresh_token,
                                                             scope = excluded.scope,
                                                             authenticated_at = excluded.authenticated_at
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
