package au.org.raid.inttest.dto.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from create, rotate and get-secret (RAID-846). Carries the secret value.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialSecretResponse(
        String clientId,
        String label,
        String secret,
        String createdAt,
        String lastRotatedAt) {
}
