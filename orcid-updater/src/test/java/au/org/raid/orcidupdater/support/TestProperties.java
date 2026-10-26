package au.org.raid.orcidupdater.support;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;

/** A complete, valid set of settings for unit tests that build components by hand. */
public final class TestProperties {
    private TestProperties() {
    }

    public static OrcidUpdaterProperties complete() {
        final var properties = new OrcidUpdaterProperties();
        properties.getOrcid().setClientId("APP-TEST");
        properties.getOrcid().setClientSecret("test");
        properties.getOrcid().setClientName("ARDC RAiD Service");
        properties.getOrcid().setBaseUrl("https://sandbox.orcid.org");
        properties.getOrcid().setApiUrl("https://api.sandbox.orcid.org/v3.0");
        properties.getRaidApi().setBaseUrl("http://raid-api.test");
        properties.getKeycloak().setIssuerUri("http://iam.test/realms/raid");
        properties.getMail().setHost("localhost");
        properties.getMail().setFrom("orcid-updater@example.org");
        properties.setEmailHashSecret("email-hash-secret");
        properties.setTokenEncryptionKey("token-encryption-key-of-at-least-32-characters");
        properties.setLinkEncryptionKey("link-encryption-key-of-at-least-32-characters");
        properties.setApiKey("test-api-key");
        properties.setAppBaseUrl("https://app.example.org/");
        properties.setContactEmail("privacy@example.org");
        return properties;
    }
}
