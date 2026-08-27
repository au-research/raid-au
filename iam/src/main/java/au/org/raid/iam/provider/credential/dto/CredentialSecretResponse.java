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

    /**
     * Redacts the secret. A record's generated {@code toString} includes every component, so
     * without this any accidental log statement or exception message mentioning this object would
     * write the secret into the log (RAID-847).
     */
    @Override
    public String toString() {
        return "CredentialSecretResponse[clientId=" + clientId
                + ", label=" + label
                + ", secret=***REDACTED***"
                + ", createdAt=" + createdAt
                + ", lastRotatedAt=" + lastRotatedAt + "]";
    }
}
