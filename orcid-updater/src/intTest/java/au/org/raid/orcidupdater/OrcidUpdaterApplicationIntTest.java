package au.org.raid.orcidupdater;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole updater against a throwaway PostgreSQL (RAID-1001): Flyway runs on startup with
 * no manual step, the health check is public, and caller authentication applies end to end.
 * Keycloak and SMTP are configured but not contacted at startup.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ORCID updater application")
class OrcidUpdaterApplicationIntTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.8")
            .withDatabaseName("orcid_updater")
            .withUsername("orcid_updater")
            .withPassword("supersecret");

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
        registry.add("orcid-updater.keycloak.issuer-uri", () -> "http://localhost:1/realms/raid");
        registry.add("orcid-updater.mail.host", () -> "localhost");
        registry.add("orcid-updater.mail.from", () -> "orcid-updater@example.org");
        registry.add("orcid-updater.email-hash-secret", () -> "test");
        registry.add("orcid-updater.token-encryption-key", () -> "test");
        registry.add("orcid-updater.link-encryption-key", () -> "int-test-link-encryption-key-32-chars");
        registry.add("orcid-updater.api-key", () -> "int-test-api-key");
        registry.add("orcid-updater.public-base-url", () -> "http://localhost:8090");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway applies the migrations into the orcid_updater schema on startup")
    void migrationsRunOnStartup() {
        final var applied = jdbcTemplate.queryForList(
                "select version from orcid_updater.flyway_schema_history where success order by installed_rank",
                String.class);
        assertThat(applied).contains("1");

        final var comment = jdbcTemplate.queryForObject(
                "select obj_description('orcid_updater'::regnamespace, 'pg_namespace')", String.class);
        assertThat(comment).contains("RAID-975");
    }

    @Test
    @DisplayName("Health is public and UP")
    void healthIsUp() {
        final var response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Other actuator endpoints are not exposed")
    void otherActuatorEndpointsHidden() {
        final var response = restTemplate.getForEntity("/actuator/env", String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("RAiD API endpoint without the key: 401 problem details")
    void raidChangesWithoutKey() {
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var response = restTemplate.exchange("/raid-changes", HttpMethod.POST,
                new HttpEntity<>("{\"handle\":\"10.1/abc\"}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("RAiD API endpoint with the key: not rewritten to 401 by the error forward (404 until RAID-1005)")
    void raidChangesWithKeyKeepsItsStatus() {
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", "int-test-api-key");

        final var response = restTemplate.exchange("/raid-changes", HttpMethod.POST,
                new HttpEntity<>("{\"handle\":\"10.1/abc\"}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Register invitation without a token: 401, and Keycloak is never needed to refuse it")
    void invitationWithoutToken() {
        final var response = restTemplate.postForEntity("/invitations", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
