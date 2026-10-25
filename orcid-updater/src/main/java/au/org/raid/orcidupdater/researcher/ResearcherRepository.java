package au.org.raid.orcidupdater.researcher;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
}
