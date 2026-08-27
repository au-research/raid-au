package au.org.raid.iam.provider.credential.dto;

/**
 * A scoped client credential without its secret value. Timestamps are ISO 8601 UTC.
 * {@code lastRotatedAt} is null until the credential has been rotated at least once.
 */
public record CredentialResponse(
        String clientId,
        String label,
        String createdAt,
        String lastRotatedAt,
        boolean enabled) {
}
