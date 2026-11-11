package au.org.raid.iam.inttest.federation;

import au.org.raid.iam.inttest.JwksServer;
import au.org.raid.iam.inttest.KeycloakNode;
import au.org.raid.iam.inttest.SigningKey;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static au.org.raid.iam.inttest.KeycloakNode.HARVEST_CLIENT_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The federation harvest client that raid-iam.jar creates at boot, checked against a real Keycloak
 * and real issued tokens: its configuration, the token it yields, the assertions it refuses, and
 * what survives a restart.
 *
 * <p>Keycloak runs on Postgres so that "restart" can be a fresh container on the same database.
 * Tests run in order because the last ones restart Keycloak and change the client.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HarvestClientIntegrationTest {

    /** Realm roles that grant RAiD API access beyond public harvesting. */
    static final Set<String> RAID_API_ROLES = Set.of("operator", "service-point-user", "group-admin",
            "raid-admin", "raid-access-handler", "raid-permissions-admin", "raid-upgrader", "contributor-writer");

    private static Network network;
    private static PostgreSQLContainer<?> postgres;
    private static JwksServer jwks;
    private static SigningKey registrationAuthorityKey;
    private static KeycloakNode keycloak;

    @BeforeAll
    static void startKeycloak() {
        registrationAuthorityKey = SigningKey.generate("ra-current");
        jwks = JwksServer.start();
        jwks.publish(registrationAuthorityKey);
        network = Network.newNetwork();
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withNetwork(network).withNetworkAliases("postgres")
                .withDatabaseName("keycloak").withUsername("keycloak").withPassword("keycloak");
        postgres.start();
        keycloak = startNode();
    }

    private static KeycloakNode startNode() {
        return KeycloakNode.builder()
                .postgres(network, null, "jdbc:postgresql://postgres:5432/keycloak", "keycloak", "keycloak")
                .jwksServer(jwks)
                .start();
    }

    private static KeycloakNode restart() {
        keycloak.stop();
        keycloak = startNode();
        return keycloak;
    }

    @AfterAll
    static void stopAll() {
        if (keycloak != null) keycloak.stop();
        if (postgres != null) postgres.stop();
        if (jwks != null) jwks.close();
        if (network != null) network.close();
    }

    private static List<String> names(final JsonNode array, final String field) {
        final var names = new ArrayList<String>();
        array.forEach(node -> names.add(node.get(field).asText()));
        return names;
    }

    private static List<String> texts(final JsonNode array) {
        final var values = new ArrayList<String>();
        if (array != null) {
            array.forEach(node -> values.add(node.asText()));
        }
        return values;
    }

    private static String serviceAccountUserId(final JsonNode client) {
        return keycloak.adminGet("/raid/clients/" + client.get("id").asText() + "/service-account-user")
                .body().get("id").asText();
    }

    // --- created on first boot ----------------------------------------------------------------

    @Test
    @Order(1)
    void createdOnFirstBootWithSignedJwtAuthentication() {
        final var client = keycloak.harvestClient();
        final var attributes = client.get("attributes");

        assertThat(client.get("clientAuthenticatorType").asText(), is("client-jwt"));
        assertThat(attributes.get("use.jwks.url").asText(), is("true"));
        assertThat(attributes.get("jwks.url").asText(), is(jwks.urlForContainers()));
        assertThat(attributes.get("token.endpoint.auth.signing.alg").asText(), is("RS256"));
        assertThat(attributes.get("access.token.lifespan").asText(), is("300"));
        assertThat(client.get("enabled").asBoolean(), is(true));
        assertThat(client.get("publicClient").asBoolean(), is(false));
        assertThat(client.get("serviceAccountsEnabled").asBoolean(), is(true));
        assertThat(client.get("standardFlowEnabled").asBoolean(), is(false));
        assertThat(client.get("implicitFlowEnabled").asBoolean(), is(false));
        assertThat(client.get("directAccessGrantsEnabled").asBoolean(), is(false));
    }

    @Test
    @Order(2)
    void hasFullScopeAndTheRealmDefaultClientScopes() {
        // A raw model-layer create leaves both off, which silently strips roles from the token.
        final var client = keycloak.harvestClient();
        // The realm defaults include SAML scopes (role_list), which never attach to an OIDC client.
        final var realmDefaults = new ArrayList<String>();
        keycloak.adminGet("/raid/default-default-client-scopes").body().forEach(scope -> {
            if ("openid-connect".equals(scope.path("protocol").asText())) {
                realmDefaults.add(scope.get("name").asText());
            }
        });

        assertThat(client.get("fullScopeAllowed").asBoolean(), is(true));
        assertThat(realmDefaults, hasItem("roles"));
        assertThat(texts(client.get("defaultClientScopes")), hasItems(realmDefaults.toArray(String[]::new)));
    }

    @Test
    @Order(3)
    void hasNoClientSecret() {
        final var client = keycloak.harvestClient();
        final var secret = keycloak.adminGet("/raid/clients/" + client.get("id").asText() + "/client-secret").body();

        assertThat(client.has("secret") ? client.get("secret").asText() : null, is(nullValue()));
        assertThat(secret.has("value") ? secret.get("value").asText() : null, is(nullValue()));
    }

    @Test
    @Order(4)
    void serviceAccountHoldsOnlyRaidDumperAndTheDefaultRole() {
        final var userId = serviceAccountUserId(keycloak.harvestClient());
        final var mappings = keycloak.adminGet("/raid/users/" + userId + "/role-mappings").body();

        assertThat(names(mappings.get("realmMappings"), "name"),
                containsInAnyOrder("raid-dumper", "default-roles-raid"));
        assertThat(mappings.has("clientMappings") ? mappings.get("clientMappings").size() : 0, is(0));
        assertThat(keycloak.adminGet("/raid/users/" + userId + "/groups").body().size(), is(0));
    }

    @Test
    @Order(5)
    void defaultRoleCompositeHoldsNoApiRoles() {
        // Every service account carries the default composite, so an API role in it would leak to
        // the harvester. The shipped realm must keep it to Keycloak's own defaults.
        final var composites = names(keycloak.adminGet("/raid/roles/default-roles-raid/composites").body(), "name");

        assertThat(composites, everyItem(not(is(in(RAID_API_ROLES)))));
    }

    @Test
    @Order(6)
    void masterRealmIsLeftAlone() {
        assertThat(keycloak.harvestClients("master").size(), is(0));
    }

    // --- tokens ---------------------------------------------------------------------------------

    @Test
    @Order(10)
    void validAssertionYieldsAHarvestOnlyToken() {
        final var response = keycloak.harvestToken(registrationAuthorityKey.assertion(HARVEST_CLIENT_ID, keycloak.issuer()));

        assertThat(response.toString(), response.status(), is(200));
        final var claims = KeycloakNode.claims(response.body().get("access_token").asText());
        final var roles = texts(claims.path("realm_access").path("roles"));
        assertThat(claims.get("azp").asText(), is(HARVEST_CLIENT_ID));
        assertThat(roles, hasItem("raid-dumper"));
        assertThat(roles, everyItem(not(is(in(RAID_API_ROLES)))));
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong(), lessThanOrEqualTo(300L));
    }

    @Test
    @Order(11)
    void replayedAssertionIsRejected() {
        final var assertion = registrationAuthorityKey.assertion(HARVEST_CLIENT_ID, keycloak.issuer());
        assertThat(keycloak.harvestToken(assertion).status(), is(200));

        final var replay = keycloak.harvestToken(assertion);

        assertThat(replay.status(), is(400));
        assertThat(replay.error(), is("invalid_client"));
    }

    @Test
    @Order(12)
    void assertionSignedByAnUnknownKeyIsRejected() {
        final var stranger = SigningKey.generate("stranger");

        final var response = keycloak.harvestToken(stranger.assertion(HARVEST_CLIENT_ID, keycloak.issuer()));

        assertThat(response.status(), is(400));
        assertThat(response.error(), is("invalid_client"));
    }

    @Test
    @Order(13)
    void assertionForgingATrustedKeyIdIsRejected() {
        final var forger = SigningKey.generate("forger");

        final var response = keycloak.harvestToken(forger.assertion(HARVEST_CLIENT_ID, keycloak.issuer(),
                registrationAuthorityKey.kid(), 60, UUID.randomUUID().toString()));

        assertThat(response.status(), is(400));
        assertThat(response.error(), is("invalid_client"));
    }

    @Test
    @Order(14)
    void expiredAssertionIsRejected() {
        final var response = keycloak.harvestToken(registrationAuthorityKey.assertion(HARVEST_CLIENT_ID,
                keycloak.issuer(), registrationAuthorityKey.kid(), -120, UUID.randomUUID().toString()));

        assertThat(response.status(), is(400));
        assertThat(response.error(), is("invalid_client"));
    }

    @Test
    @Order(15)
    void assertionForTheWrongAudienceIsRejected() {
        final var response = keycloak.harvestToken(registrationAuthorityKey.assertion(HARVEST_CLIENT_ID,
                keycloak.baseUrl() + "/realms/master"));

        assertThat(response.status(), is(400));
        assertThat(response.error(), is("invalid_client"));
    }

    @Test
    @Order(16)
    void clientSecretIsRejected() {
        final var response = keycloak.clientCredentials(Map.of(
                "client_id", HARVEST_CLIENT_ID, "client_secret", "anything"));

        assertThat(response.status(), is(400));
        assertThat(response.error(), is("invalid_client"));
    }

    @Test
    @Order(17)
    void requestWithNoClientAuthenticationIsRejected() {
        final var response = keycloak.clientCredentials(Map.of("client_id", HARVEST_CLIENT_ID));

        assertThat(response.status(), is(400));
        assertThat(response.error(), is("invalid_client"));
    }

    // --- restarts -------------------------------------------------------------------------------

    @Test
    @Order(20)
    void restartIsIdempotent() {
        final var before = keycloak.harvestClient();

        restart();

        assertThat(keycloak.harvestClients("raid").size(), is(1));
        assertThat(keycloak.harvestClient(), is(before));
        assertThat(keycloak.logs(), not(containsString("client '" + HARVEST_CLIENT_ID + "' created")));
        assertThat(keycloak.logs(), not(containsString("client '" + HARVEST_CLIENT_ID + "' updated")));
    }

    @Test
    @Order(21)
    void restartPutsDriftBackButLeavesTheClientDisabled() {
        // An agency, or an attacker with admin access, edits the client in the admin console:
        // disables it, lengthens the token, switches to a client secret and grants operator.
        final var client = keycloak.harvestClient();
        final var id = client.get("id").asText();
        final var edited = (com.fasterxml.jackson.databind.node.ObjectNode) client.deepCopy();
        edited.put("enabled", false);
        edited.put("clientAuthenticatorType", "client-secret");
        ((com.fasterxml.jackson.databind.node.ObjectNode) edited.get("attributes")).put("access.token.lifespan", "86400");
        assertThat(keycloak.adminPut("/raid/clients/" + id, edited).status(), is(204));
        final var operator = keycloak.adminGet("/raid/roles/operator").body();
        assertThat(keycloak.adminPost("/raid/users/" + serviceAccountUserId(client) + "/role-mappings/realm",
                List.of(operator)).status(), is(204));
        // The admin console path stores a client secret even on a client-jwt client.
        assertThat(keycloak.adminGet("/raid/clients/" + id + "/client-secret").body().path("value").asText(""),
                not(emptyString()));

        restart();

        final var after = keycloak.harvestClient();
        assertThat(after.get("enabled").asBoolean(), is(false));
        assertThat(after.get("clientAuthenticatorType").asText(), is("client-jwt"));
        assertThat(after.get("attributes").get("access.token.lifespan").asText(), is("300"));
        assertThat(keycloak.adminGet("/raid/clients/" + id + "/client-secret").body().path("value").asText(""),
                is(emptyString()));
        final var roles = names(keycloak.adminGet("/raid/users/" + serviceAccountUserId(after)
                + "/role-mappings/realm").body(), "name");
        assertThat(new HashSet<>(roles), is(Set.of("raid-dumper", "default-roles-raid")));
    }

    @Test
    @Order(22)
    void disabledClientCannotGetATokenEvenWithAValidAssertion() {
        final var response = keycloak.harvestToken(registrationAuthorityKey.assertion(HARVEST_CLIENT_ID, keycloak.issuer()));

        assertThat(response.status(), is(401));
        assertThat(response.error(), is("invalid_client"));
    }
}
