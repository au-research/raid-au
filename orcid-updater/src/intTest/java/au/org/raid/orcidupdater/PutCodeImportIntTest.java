package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidOrganisation;
import au.org.raid.orcidupdater.orcid.OrcidRaid;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import au.org.raid.orcidupdater.orcid.OrcidWriter;
import au.org.raid.orcidupdater.orcid.RorClient;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAID-1007 end to end against a real PostgreSQL: the admin endpoint, the insert-or-keep rule, and
 * the writer then updating the imported entry instead of creating another. ORCID and ROR are stubbed.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Put code import")
class PutCodeImportIntTest {
    private static final String BARE_ID = "0000-0002-1825-0097";
    private static final String ID = "https://sandbox.orcid.org/" + BARE_ID;
    private static final String OTHER_ID = "https://sandbox.orcid.org/0000-0001-5109-3700";
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String OTHER_HANDLE = "10.82841/5d3e2a10";
    private static final String OWNER = "https://ror.org/038sjwq14";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.8")
            .withDatabaseName("orcid_updater").withUsername("orcid_updater").withPassword("supersecret");

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
        registry.add("orcid-updater.email-hash-secret", () -> "int-test-email-hash-secret");
        registry.add("orcid-updater.token-encryption-key", () -> "int-test-token-encryption-key-32-chars");
        registry.add("orcid-updater.link-encryption-key", () -> "int-test-link-encryption-key-32-chars");
        registry.add("orcid-updater.api-key", () -> "int-test-api-key");
        registry.add("orcid-updater.app-base-url", () -> "https://app.example.org");
        registry.add("orcid-updater.contact-email", () -> "privacy@example.org");
        registry.add("orcid-updater.work.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OrcidEntryRepository entryRepository;
    @Autowired
    private ResearcherRepository researcherRepository;
    @Autowired
    private TokenCipher tokenCipher;
    @Autowired
    private OrcidWriter writer;

    @MockitoBean
    private OrcidMemberClient orcid;
    @MockitoBean
    private RorClient rorClient;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM orcid_updater.orcid_entry");
        jdbcTemplate.update("DELETE FROM orcid_updater.researcher");
        reset(orcid);
        when(jwtDecoder.decode(anyString())).thenAnswer(call -> Jwt.withTokenValue(call.getArgument(0))
                .header("alg", "none").subject("admin")
                .claim("realm_access", Map.of("roles", List.of("orcid-updater-admin")))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build());
        when(rorClient.find(OWNER)).thenReturn(Optional.of(new OrcidOrganisation("Australian Research Data Commons",
                "Melbourne", "Victoria", "AU", OWNER)));
    }

    private ResponseEntity<Map> importCsv(final String csv) {
        final var headers = new HttpHeaders();
        headers.setBearerAuth("admin-token");
        headers.setContentType(MediaType.valueOf("text/csv"));
        return restTemplate.exchange("/admin/put-code-imports", HttpMethod.POST, new HttpEntity<>(csv, headers), Map.class);
    }

    private Long putCode(final String orcidId, final String handle) {
        return entryRepository.find(orcidId, handle).map(OrcidEntryRepository.OrcidEntry::putCode).orElse(null);
    }

    @Test
    @DisplayName("Imports new rows, keeps put codes already held, fills a failed entry's, and is safe to run again")
    void importsOnce() {
        final var now = Instant.now();
        entryRepository.recordSuccess(OTHER_ID, HANDLE, 999L, now);
        entryRepository.recordFailure(OTHER_ID, OTHER_HANDLE, "ORCID returned 503", now);
        final var csv = """
                handle,orcidId,putCode
                %s,%s,1234
                %s,%s,555
                %s,%s,777
                not-a-handle,%s,1
                """.formatted(HANDLE, BARE_ID, HANDLE, OTHER_ID, OTHER_HANDLE, OTHER_ID, BARE_ID);

        final var first = importCsv(csv);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).isEqualTo(Map.of("imported", 2, "skipped", 1,
                "rejected", List.of(Map.of("line", 5, "reason", "handle must be a RAiD handle as prefix/suffix"))));
        assertThat(putCode(ID, HANDLE)).isEqualTo(1234L);
        assertThat(putCode(OTHER_ID, HANDLE)).isEqualTo(999L);
        assertThat(putCode(OTHER_ID, OTHER_HANDLE)).isEqualTo(777L);

        assertThat(importCsv(csv).getBody()).containsEntry("imported", 0).containsEntry("skipped", 3);
        assertThat(putCode(ID, HANDLE)).isEqualTo(1234L);
    }

    @Test
    @DisplayName("When the researcher authenticates, the imported entry is updated, not duplicated")
    void writerUpdatesImportedEntry() {
        importCsv("handle,orcidId,putCode\n%s,%s,1234\n".formatted(HANDLE, BARE_ID));
        researcherRepository.saveAuthentication(ID, tokenCipher.encrypt("access-" + ID, ID),
                tokenCipher.encrypt("refresh-" + ID, ID), "/read-limited /activities/update", Instant.now());

        writer.sync(new OrcidRaid(HANDLE, "https://raid.org/" + HANDLE, OWNER,
                List.of(new Title("Coral reef resilience", "https://vocabulary.raid.org/title.type.schema/5", "2025-03-01", null)),
                List.of(new Contributor(ID, List.of(new Position(
                        "https://vocabulary.raid.org/contributor.position.schema/307", "2025-03-01", null))))));

        verify(orcid).update(eq(ID), eq("access-" + ID), eq(1234L), argThat(m -> m.get("put-code").equals(1234L)));
        verify(orcid, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("A file without the header: 400 INVALID_IMPORT_FILE, nothing imported")
    void invalidFile() {
        final var response = importCsv("%s,%s,1234\n".formatted(HANDLE, BARE_ID));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "INVALID_IMPORT_FILE");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater.orcid_entry", Integer.class)).isZero();
    }
}
