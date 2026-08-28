package au.org.raid.inttest.dto.keycloak;

/**
 * Request body for {@code POST /realms/raid/client-credential} (RAID-846).
 */
public record CreateCredentialRequest(String groupId, String label) {
}
