package au.org.raid.inttest.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An entry in the list response from {@code GET /realms/raid/client-credential} (RAID-846).
 * Deliberately carries no secret.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialSummary(
        String clientId,
        String label,
        String createdAt,
        String lastRotatedAt,
        boolean enabled) {
}
