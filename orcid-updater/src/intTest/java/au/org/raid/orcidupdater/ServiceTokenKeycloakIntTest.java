package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.serviceauth.ServiceTokenClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The updater's private_key_jwt sign-in against a real Keycloak (RAID-1005): Keycloak fetches the
 * updater's JWKS from its running endpoint and checks the signed client assertion. The client is
 * created here the way RAID-1011 will create it, with no secret.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Service token from Keycloak")
class ServiceTokenKeycloakIntTest {
    private static final String REALM = "raid";
    private static final String CLIENT_ID = "orcid-updater";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.8")
            .withDatabaseName("orcid_updater").withUsername("orcid_updater").withPassword("supersecret");

    @Container
    static final GenericContainer<?> KEYCLOAK = new GenericContainer<>("quay.io/keycloak/keycloak:26.6.2")
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withCommand("start-dev")
            .withExposedPorts(8080)
            // so Keycloak can read the updater's JWKS from the test's web server
            .withAccessToHost(true)
            .waitingFor(Wait.forHttp("/realms/master").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));

    private static boolean clientCreated;

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("orcid-updater.orcid.client-id", () -> "APP-TEST");
        registry.add("orcid-updater.orcid.client-secret", () -> "test");
        registry.add("orcid-updater.orcid.client-name", () -> "ARDC RAiD Service");
        registry.add("orcid-updater.orcid.base-url", () -> "https://sandbox.orcid.org");
        registry.add("orcid-updater.orcid.api-url", () -> "https://api.sandbox.orcid.org/v3.0");
        registry.add("orcid-updater.raid-api.base-url", () -> "http://localhost:1");
        registry.add("orcid-updater.keycloak.issuer-uri", () -> keycloakUrl() + "/realms/" + REALM);
        registry.add("orcid-updater.mail.host", () -> "localhost");
        registry.add("orcid-updater.mail.from", () -> "orcid-updater@example.org");
        registry.add("orcid-updater.email-hash-secret", () -> "int-test-email-hash-secret");
        registry.add("orcid-updater.token-encryption-key", () -> "int-test-token-encryption-key-32-chars");
        registry.add("orcid-updater.link-encryption-key", () -> "int-test-link-encryption-key-32-chars");
        registry.add("orcid-updater.api-key", () -> "int-test-api-key");
        registry.add("orcid-updater.app-base-url", () -> "https://app.example.org");
        registry.add("orcid-updater.contact-email", () -> "privacy@example.org");
        registry.add("orcid-updater.work.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ServiceTokenClient serviceTokenClient;

    // the resource server would otherwise read the realm's keys at startup, before the realm exists
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static String keycloakUrl() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
    }

    @BeforeEach
    void setUp() {
        if (clientCreated) {
            return;
        }
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        final var keycloak = RestClient.create(keycloakUrl());
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", "admin");
        form.add("password", "admin");
        final var adminToken = keycloak.post().uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(JsonNode.class).path("access_token").asText();

        keycloak.post().uri("/admin/realms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("realm", REALM, "enabled", true))
                .retrieve().toBodilessEntity();
        keycloak.post().uri("/admin/realms/{realm}/clients", REALM)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "clientId", CLIENT_ID,
                        "enabled", true,
                        "publicClient", false,
                        "standardFlowEnabled", false,
                        "serviceAccountsEnabled", true,
                        "clientAuthenticatorType", "client-jwt",
                        "attributes", Map.of(
                                "use.jwks.url", "true",
                                "jwks.url", "http://host.testcontainers.internal:" + port + "/.well-known/jwks.json",
                                "token.endpoint.auth.signing.alg", "RS256")))
                .retrieve().toBodilessEntity();
        clientCreated = true;
    }

    @Test
    @DisplayName("Keycloak accepts the signed assertion and issues a token for the updater's client")
    void issuesToken() throws Exception {
        serviceTokenClient.reset();

        final var token = serviceTokenClient.accessToken();

        final var payload = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(payload.path("azp").asText()).isEqualTo(CLIENT_ID);
        assertThat(payload.path("iss").asText()).endsWith("/realms/" + REALM);
    }

    @Test
    @DisplayName("An assertion signed with a key Keycloak doesn't know is refused with invalid_client")
    void unknownKeyRefused() throws Exception {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", CLIENT_ID);
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", forgedAssertion());

        assertThatThrownBy(() -> RestClient.create(keycloakUrl()).post()
                .uri("/realms/{realm}/protocol/openid-connect/token", REALM)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().toBodilessEntity())
                .hasMessageContaining("invalid_client");
    }

    /** Right claims, wrong key: as someone without the updater's private key would have to sign. */
    private String forgedAssertion() throws Exception {
        final var key = new RSAKeyGenerator(2048).keyID("forged").generate();
        final var now = Instant.now();
        final var claims = new JWTClaimsSet.Builder()
                .issuer(CLIENT_ID).subject(CLIENT_ID).audience(keycloakUrl() + "/realms/" + REALM)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))
                .build();
        final var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("forged").build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
