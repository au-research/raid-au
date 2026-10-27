package au.org.raid.orcidupdater.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Settings every registration agency must supply (RAID-975). Validation runs at startup, so a
 * partly configured updater fails to start and Spring's failure report names every missing
 * setting at once.
 *
 * <p>No {@code @Data} or {@code @ToString}: several fields are secrets and must never be logged.
 * Only presence is validated on secrets, because a failed format check would echo the value.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "orcid-updater")
public class OrcidUpdaterProperties {

    @Valid
    @NotNull
    private Orcid orcid = new Orcid();

    @Valid
    @NotNull
    private RaidApi raidApi = new RaidApi();

    @Valid
    @NotNull
    private Keycloak keycloak = new Keycloak();

    @Valid
    @NotNull
    private Mail mail = new Mail();

    @Valid
    @NotNull
    private Invitation invitation = new Invitation();

    @Valid
    @NotNull
    private OrcidWrite orcidWrite = new OrcidWrite();

    @Valid
    @NotNull
    private Ror ror = new Ror();

    @Valid
    @NotNull
    private Work work = new Work();

    /** HMAC-SHA-256 key for the email fingerprint kept when a researcher ticks "Remember me". */
    @NotBlank
    private String emailHashSecret;

    /** Key for encrypting researchers' ORCID tokens at rest. Length is checked by TokenCipher. */
    @NotBlank
    private String tokenEncryptionKey;

    /**
     * Key for encrypting the email fingerprint carried in an invitation link, so the fingerprint
     * is stored only if the researcher consents (RAID-1002). Length is checked by LinkCipher,
     * which can report it without echoing the value.
     */
    @NotBlank
    private String linkEncryptionKey;

    /** Shared secret the RAiD API presents in the X-Api-Key header. */
    @NotBlank
    private String apiKey;

    /**
     * The RAiD app that shows researchers their pages, e.g. https://app.example.org (RAID-977).
     * Invitation links and the ORCID redirect URI point into it, and it is the only origin
     * allowed to call the updater from a browser.
     */
    @NotBlank
    private String appBaseUrl;

    /**
     * Where researchers ask the RA to delete their stored data, returned with the invitation for
     * the app's landing page. The RA's own contact, never a service point's.
     */
    @NotBlank
    private String contactEmail;

    /** A URL in the RAiD app; a trailing slash on the base URL is ignored. */
    public String appUrl(final String path) {
        return appBaseUrl.replaceAll("/+$", "") + path;
    }

    @Getter
    @Setter
    public static class Orcid {
        @NotBlank
        private String clientId;
        @NotBlank
        private String clientSecret;
        /** Shown to researchers on the landing page and in the email, e.g. "ARDC RAiD Service". */
        @NotBlank
        private String clientName;
        /** https://orcid.org or https://sandbox.orcid.org: sign-in and iD URLs. */
        @NotBlank
        private String baseUrl;
        /** https://api.orcid.org/v3.0 or https://api.sandbox.orcid.org/v3.0: member API. */
        @NotBlank
        private String apiUrl;
    }

    @Getter
    @Setter
    public static class RaidApi {
        @NotBlank
        private String baseUrl;
        /**
         * The updater's own Keycloak client, which holds contributor-writer. It authenticates with
         * private_key_jwt, so there is no secret to configure (RAID-1011 provisions it).
         */
        @NotBlank
        private String clientId = "orcid-updater";
    }

    @Getter
    @Setter
    public static class Keycloak {
        /** Issuer of minter and administrator tokens, e.g. https://iam.example.org/realms/raid. */
        @NotBlank
        private String issuerUri;
    }

    @Getter
    @Setter
    public static class Mail {
        @NotBlank
        private String host;
        private int port = 587;
        /** Optional: some RA relays accept mail without authentication. */
        private String username;
        private String password;
        private boolean startTls = true;
        /** Sender on the RA's own domain. */
        @NotBlank
        private String from;
    }

    @Getter
    @Setter
    public static class Invitation {
        @NotNull
        private Duration expiry = Duration.ofDays(14);
    }

    /**
     * Retries for a write ORCID couldn't take (5xx, 429, no response). After the last attempt the
     * failure is recorded and the next save of the RAiD tries again (Decision 2026-10-23, Q6).
     */
    @Getter
    @Setter
    public static class OrcidWrite {
        @Min(1)
        private int maxAttempts = 3;
        /** Wait before the second attempt, doubled before each later one. */
        @NotNull
        private Duration initialBackoff = Duration.ofSeconds(2);
    }

    @Getter
    @Setter
    public static class Ror {
        /** ROR's API, for the RAiD owner's organisation named on every ORCID entry. */
        @NotBlank
        private String apiUrl = "https://api.ror.org/v2";
    }

    /**
     * The background worker that pushes status changes and syncs RAiDs (RAID-1005). Only reaching
     * the RAiD API is retried here; ORCID write failures are recorded per entry (OrcidWrite).
     */
    @Getter
    @Setter
    public static class Work {
        /** Off only in tests that drive the worker themselves. */
        private boolean enabled = true;
        @NotNull
        private Duration pollInterval = Duration.ofSeconds(5);
        @Min(1)
        private int batchSize = 20;
        /** Attempts to reach the RAiD API for one item before it is recorded as failed. */
        @Min(1)
        private int maxAttempts = 5;
        /** Wait before the second attempt, doubled before each later one. */
        @NotNull
        private Duration initialBackoff = Duration.ofSeconds(30);
        /** How long an instance may hold an item before another may take it over. */
        @NotNull
        private Duration claimTimeout = Duration.ofMinutes(5);
    }
}
