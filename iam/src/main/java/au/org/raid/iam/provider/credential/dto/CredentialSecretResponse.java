package au.org.raid.iam.provider.credential.dto;

/**
 * A scoped client credential including its secret value. Returned only by create, rotate and
 * get-secret.
 *
 * <p>Never log an instance of this type. Timestamps are ISO 8601 UTC.
 */
public record CredentialSecretResponse(
        String clientId,
        String label,
        String secret,
        String createdAt,
        String lastRotatedAt) {
}
