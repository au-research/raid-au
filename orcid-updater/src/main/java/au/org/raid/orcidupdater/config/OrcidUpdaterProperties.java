package au.org.raid.orcidupdater.config;

import jakarta.validation.Valid;
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

    /** HMAC-SHA-256 key for the email fingerprint kept when a researcher ticks "Remember me". */
    @NotBlank
    private String emailHashSecret;

    /** Key for encrypting researchers' ORCID access tokens at rest. */
    @NotBlank
    private String tokenEncryptionKey;

    /** Shared secret the RAiD API presents in the X-Api-Key header. */
    @NotBlank
    private String apiKey;

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
}
