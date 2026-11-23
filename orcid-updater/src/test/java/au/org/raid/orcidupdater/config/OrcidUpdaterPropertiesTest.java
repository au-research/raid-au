package au.org.raid.orcidupdater.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAID-975 scenario "Partial configuration": a partly configured updater must not start, and the
 * failure must name every missing setting so an RA can fix them in one pass.
 */
@DisplayName("OrcidUpdaterProperties startup validation")
class OrcidUpdaterPropertiesTest {

    static final List<String> COMPLETE = List.of(
            "orcid-updater.orcid.client-id=APP-TEST",
            "orcid-updater.orcid.client-secret=orcid-secret-value",
            "orcid-updater.orcid.client-name=ARDC RAiD Service",
            "orcid-updater.orcid.base-url=https://sandbox.orcid.org",
            "orcid-updater.orcid.api-url=https://api.sandbox.orcid.org/v3.0",
            "orcid-updater.raid-api.base-url=http://localhost:8080",
            "orcid-updater.keycloak.issuer-uri=http://localhost:8001/realms/raid",
            "orcid-updater.mail.host=localhost",
            "orcid-updater.mail.from=orcid-updater@example.org",
            "orcid-updater.email-hash-secret=hash-secret-value",
            "orcid-updater.token-encryption-key=encryption-key-value",
            "orcid-updater.link-encryption-key=link-key-value-of-at-least-32-chars",
            "orcid-updater.api-key=api-key-value",
            "orcid-updater.app-base-url=https://app.example.org",
            "orcid-updater.contact-email=privacy@example.org");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @Configuration
    @EnableConfigurationProperties(OrcidUpdaterProperties.class)
    static class PropertiesConfig {
    }

    private static String[] completeExcept(final String... prefixes) {
        return COMPLETE.stream()
                .filter(property -> Arrays.stream(prefixes).noneMatch(property::startsWith))
                .toArray(String[]::new);
    }

    private static String failureMessage(final Throwable failure) {
        final var message = new StringBuilder();
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            message.append(cause.getMessage()).append('\n');
        }
        return message.toString();
    }

    @Test
    @DisplayName("Starts when every required setting is present, with the default invitation expiry")
    void startsWhenComplete() {
        runner.withPropertyValues(COMPLETE.toArray(String[]::new)).run(context -> {
            assertThat(context).hasNotFailed();
            final var properties = context.getBean(OrcidUpdaterProperties.class);
            assertThat(properties.getInvitation().getExpiry()).isEqualTo(Duration.ofDays(14));
            assertThat(properties.getMail().getPort()).isEqualTo(587);
            assertThat(properties.getRaidApi().getClientId()).isEqualTo("orcid-updater");
            assertThat(properties.getWork().isEnabled()).isTrue();
            assertThat(properties.getWork().getMaxAttempts()).isEqualTo(5);
            assertThat(properties.getWork().getInitialBackoff()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    @DisplayName("Client ID set but client secret missing: startup fails and names the secret")
    void failsWhenClientSecretMissing() {
        runner.withPropertyValues(completeExcept("orcid-updater.orcid.client-secret")).run(context -> {
            assertThat(context).hasFailed();
            final var message = failureMessage(context.getStartupFailure());
            assertThat(message).contains("orcid.clientSecret");
            assertThat(message).doesNotContain("orcid.clientId'");
        });
    }

    @Test
    @DisplayName("Several settings missing: the failure names each one")
    void namesEveryMissingSetting() {
        runner.withPropertyValues(completeExcept(
                "orcid-updater.orcid.client-secret",
                "orcid-updater.mail.host",
                "orcid-updater.api-key",
                "orcid-updater.keycloak.issuer-uri")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(failureMessage(context.getStartupFailure()))
                    .contains("orcid.clientSecret", "mail.host", "apiKey", "keycloak.issuerUri");
        });
    }

    @Test
    @DisplayName("Nothing set: the failure names every required setting")
    void namesEveryRequiredSettingWhenNoneSet() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(failureMessage(context.getStartupFailure())).contains(
                    "orcid.clientId", "orcid.clientSecret", "orcid.clientName", "orcid.baseUrl",
                    "orcid.apiUrl", "raidApi.baseUrl", "keycloak.issuerUri", "mail.host", "mail.from",
                    "emailHashSecret", "tokenEncryptionKey", "linkEncryptionKey", "apiKey", "appBaseUrl", "contactEmail");
        });
    }

    @Test
    @DisplayName("A blank value counts as missing")
    void blankCountsAsMissing() {
        final var properties = new ArrayList<>(List.of(completeExcept("orcid-updater.api-key")));
        properties.add("orcid-updater.api-key=   ");
        runner.withPropertyValues(properties.toArray(String[]::new)).run(context -> {
            assertThat(context).hasFailed();
            assertThat(failureMessage(context.getStartupFailure())).contains("apiKey");
        });
    }

    @Test
    @DisplayName("The failure never echoes secrets that were set")
    void failureDoesNotEchoSecrets() {
        runner.withPropertyValues(completeExcept("orcid-updater.mail.host")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(failureMessage(context.getStartupFailure())).doesNotContain(
                    "orcid-secret-value", "hash-secret-value", "encryption-key-value", "link-key-value", "api-key-value");
        });
    }

    @Test
    @DisplayName("The environment variable names documented in the README bind")
    void readmeEnvironmentVariablesBind() {
        final var environment = new HashMap<String, Object>();
        COMPLETE.forEach(property -> {
            final var parts = property.split("=", 2);
            environment.put(parts[0].replace('.', '_').replace('-', '_').toUpperCase(), parts[1]);
        });
        environment.put("ORCID_UPDATER_INVITATION_EXPIRY", "7d");
        environment.put("ORCID_UPDATER_RAID_API_CLIENT_ID", "orcid-updater-raid-au");
        environment.put("ORCID_UPDATER_WORK_ENABLED", "false");
        environment.put("ORCID_UPDATER_WORK_POLL_INTERVAL", "10s");
        environment.put("ORCID_UPDATER_WORK_BATCH_SIZE", "50");
        environment.put("ORCID_UPDATER_WORK_MAX_ATTEMPTS", "8");
        environment.put("ORCID_UPDATER_WORK_INITIAL_BACKOFF", "1m");
        environment.put("ORCID_UPDATER_WORK_CLAIM_TIMEOUT", "10m");

        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("systemEnvironment", environment)))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final var properties = context.getBean(OrcidUpdaterProperties.class);
                    assertThat(properties.getOrcid().getClientId()).isEqualTo("APP-TEST");
                    assertThat(properties.getEmailHashSecret()).isEqualTo("hash-secret-value");
                    assertThat(properties.getInvitation().getExpiry()).isEqualTo(Duration.ofDays(7));
                    assertThat(properties.getRaidApi().getClientId()).isEqualTo("orcid-updater-raid-au");
                    final var work = properties.getWork();
                    assertThat(work.isEnabled()).isFalse();
                    assertThat(work.getPollInterval()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(work.getBatchSize()).isEqualTo(50);
                    assertThat(work.getMaxAttempts()).isEqualTo(8);
                    assertThat(work.getInitialBackoff()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(work.getClaimTimeout()).isEqualTo(Duration.ofMinutes(10));
                });
    }

    @Test
    @DisplayName("Properties have no toString that could print a secret")
    void toStringDoesNotExposeSecrets() {
        final var properties = new OrcidUpdaterProperties();
        properties.setApiKey("api-key-value");
        properties.getOrcid().setClientSecret("orcid-secret-value");
        properties.getMail().setPassword("mail-password-value");

        assertThat(properties.toString()).doesNotContain("api-key-value");
        assertThat(properties.getOrcid().toString()).doesNotContain("orcid-secret-value");
        assertThat(properties.getMail().toString()).doesNotContain("mail-password-value");
    }
}
