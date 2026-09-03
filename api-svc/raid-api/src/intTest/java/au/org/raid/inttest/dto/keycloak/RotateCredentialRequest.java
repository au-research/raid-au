package au.org.raid.inttest.dto.keycloak;

/**
 * Request body for {@code POST /realms/raid/client-credential/rotate} (RAID-846).
 */
public record RotateCredentialRequest(String clientId) {
}
