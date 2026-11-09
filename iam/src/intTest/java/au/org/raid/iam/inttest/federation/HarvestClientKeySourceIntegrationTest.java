package au.org.raid.iam.inttest.federation;

import au.org.raid.iam.inttest.EmbeddedKeysJar;
import au.org.raid.iam.inttest.KeycloakNode;
import au.org.raid.iam.inttest.SigningKey;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static au.org.raid.iam.inttest.KeycloakNode.HARVEST_CLIENT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.keycloak.util.JsonSerialization.mapper;

/**
 * Where the harvest client takes the Registration Authority's keys from, on a real Keycloak: the
 * compiled-in JWKS URL, a JWKS URL that cannot be reached, and the key set embedded in the jar.
 * Each test boots its own Keycloak, because the key source is read at boot.
 */
class HarvestClientKeySourceIntegrationTest {

    @Test
    void withNoConfigurationTheClientTrustsTheCompiledInJwksUrl() {
        try (var keycloak = KeycloakNode.builder().start()) {
            final var attributes = keycloak.harvestClient().get("attributes");

            assertThat(attributes.get("use.jwks.url").asText(), is("true"));
            assertThat(attributes.get("jwks.url").asText(), is("https://keys.raid.org/federation/jwks.json"));
        }
    }

    @Test
    void anUnreachableJwksDoesNotBlockBootAndIsReportedByUrl() throws InterruptedException {
        // .invalid never resolves (RFC 2606), so this stays unreachable on any network.
        final var unreachable = "https://jwks.raid-harvest-test.invalid/jwks.json";
        try (var keycloak = KeycloakNode.builder().env("RAID_FEDERATION_HARVEST_JWKS_URL", unreachable).start()) {
            assertThat(keycloak.harvestClient().get("attributes").get("jwks.url").asText(), is(unreachable));

            // The check runs in the background after boot; give it time to finish.
            final var deadline = Instant.now().plus(Duration.ofSeconds(30));
            while (!keycloak.logs().contains("may be unreachable") && Instant.now().isBefore(deadline)) {
                Thread.sleep(500);
            }
            final var warning = keycloak.logs().lines().filter(line -> line.contains("may be unreachable"))
                    .findFirst().orElse("");
            assertThat(warning, allOf(containsString("WARN"), containsString(unreachable)));
            // The shipped embedded key set is empty, so the warning must not suggest switching to it.
            assertThat(warning, not(containsString("RAID_FEDERATION_HARVEST_USE_JWKS_URL")));
        }
    }

    @Test
    void theEmbeddedKeySetIsUsedWhenTheJwksUrlIsDisabled() throws java.io.IOException {
        final var current = SigningKey.generate("embedded-current");
        final var next = SigningKey.generate("embedded-next");
        final var jar = EmbeddedKeysJar.write(Path.of(System.getProperty("raid.iam.jar")),
                Path.of(System.getProperty("raid.iam.workDir"), "raid-iam-embedded-test-keys.jar"), current, next);

        try (var keycloak = KeycloakNode.builder().jar(jar)
                .env("RAID_FEDERATION_HARVEST_USE_JWKS_URL", "false").start()) {
            final var attributes = keycloak.harvestClient().get("attributes");
            assertThat(attributes.get("use.jwks.url").asText(), is("false"));
            assertThat(attributes.has("jwks.url"), is(false));
            assertThat(attributes.get("use.jwks.string").asText(), is("true"));
            final var kids = new ArrayList<String>();
            mapper.readTree(attributes.get("jwks.string").asText())
                    .get("keys").forEach(key -> kids.add(key.get("kid").asText()));
            assertThat(kids, containsInAnyOrder("embedded-current", "embedded-next"));

            // A release carries the current and next keys, so either is accepted from one client.
            final var issuer = keycloak.issuer();
            assertThat(keycloak.harvestToken(current.assertion(HARVEST_CLIENT_ID, issuer)).status(), is(200));
            assertThat(keycloak.harvestToken(next.assertion(HARVEST_CLIENT_ID, issuer)).status(), is(200));
            final var stranger = keycloak.harvestToken(SigningKey.generate("stranger").assertion(HARVEST_CLIENT_ID, issuer));
            assertThat(stranger.status(), is(400));
            assertThat(stranger.error(), is("invalid_client"));
        }
    }
}
