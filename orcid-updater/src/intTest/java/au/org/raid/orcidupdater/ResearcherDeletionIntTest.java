package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidEntryRepository;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidWriteException;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import au.org.raid.orcidupdater.raidapi.RaidSummary;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.signin.OAuthStateRepository;
import au.org.raid.orcidupdater.signin.OrcidOAuthClient;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAID-1006 end to end against a real PostgreSQL: the admin endpoint, the lock, the rollback when
 * ORCID fails, and every table that holds something about the researcher. ORCID is stubbed.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Researcher data deletion")
class ResearcherDeletionIntTest {
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String TYPED_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final String BYSTANDER = "https://orcid.org/0000-0003-1419-2405";
    private static final String EMAIL = "researcher@example.edu";
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String OTHER_HANDLE = "10.82841/5d3e2a10";

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
    private ResearcherRepository researcherRepository;
    @Autowired
    private OrcidEntryRepository entryRepository;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private OAuthStateRepository oauthStateRepository;
    @Autowired
    private TokenCipher tokenCipher;
    @Autowired
    private EmailHasher emailHasher;

    @MockitoBean
    private OrcidMemberClient orcid;
    @MockitoBean
    private OrcidOAuthClient oauth;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        for (final var table : List.of("oauth_state", "invitation", "orcid_entry", "researcher", "work_item")) {
            jdbcTemplate.update("DELETE FROM orcid_updater." + table);
        }
        reset(orcid, oauth);
        when(jwtDecoder.decode(anyString())).thenAnswer(call -> Jwt.withTokenValue(call.getArgument(0))
                .header("alg", "none").subject("admin")
                .claim("realm_access", Map.of("roles", List.of("orcid-updater-admin")))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build());

        held(ID, TYPED_ID);
        held(BYSTANDER, BYSTANDER);
    }

    /**
     * Everything the updater can hold about a researcher: an invitation sent to the typed iD and
     * used to sign in as this one, a pending invitation with a sign-in in progress, tokens,
     * "Remember me" consent and two ORCID entries.
     */
    private void held(final String orcidId, final String typedId) {
        final var now = Instant.now();
        final var used = UUID.randomUUID();
        invitationRepository.insert(used, randomBytes(), typedId, raid(HANDLE), now, now.plus(Duration.ofDays(14)));
        invitationRepository.markUsed(used, orcidId, now);
        final var pending = UUID.randomUUID();
        invitationRepository.insert(pending, randomBytes(), orcidId, raid(OTHER_HANDLE), now, now.plus(Duration.ofDays(14)));
        oauthStateRepository.insert(randomBytes(), pending, emailHasher.hash(email(orcidId)), now, now.plusSeconds(600));

        researcherRepository.saveAuthentication(orcidId, tokenCipher.encrypt("access-" + orcidId, orcidId),
                tokenCipher.encrypt("refresh-" + orcidId, orcidId), "/read-limited /activities/update", now);
        researcherRepository.recordConsent(orcidId, emailHasher.hash(email(orcidId)), now);
        entryRepository.recordSuccess(orcidId, HANDLE, putCode(orcidId, 1), now);
        entryRepository.recordSuccess(orcidId, OTHER_HANDLE, putCode(orcidId, 2), now);
    }

    private static String email(final String orcidId) {
        return orcidId.equals(ID) ? EMAIL : "bystander@example.edu";
    }

    private static long putCode(final String orcidId, final int n) {
        return (orcidId.equals(ID) ? 100 : 200) + n;
    }

    private static RaidSummary raid(final String handle) {
        return new RaidSummary(handle, "Coral reef resilience", "Reef Research Institute", List.of());
    }

    private static byte[] randomBytes() {
        return UUID.randomUUID().toString().getBytes();
    }

    private ResponseEntity<Map> delete(final String json) {
        final var headers = new HttpHeaders();
        headers.setBearerAuth("admin-token");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange("/admin/researcher-deletions", HttpMethod.POST, new HttpEntity<>(json, headers), Map.class);
    }

    /** Rows anywhere that still mention the iD, or the email's fingerprint. */
    private int heldAbout(final String orcidId) {
        final var hash = emailHasher.hash(email(orcidId));
        return jdbcTemplate.queryForObject("""
                SELECT (SELECT count(*) FROM orcid_updater.researcher WHERE orcid_id = ? OR email_hash = ?)
                     + (SELECT count(*) FROM orcid_updater.orcid_entry WHERE orcid_id = ?)
                     + (SELECT count(*) FROM orcid_updater.invitation WHERE orcid_id = ? OR authenticated_orcid_id = ?)
                     + (SELECT count(*) FROM orcid_updater.oauth_state WHERE email_hash = ?)""",
                Integer.class, orcidId, hash, orcidId, orcidId, orcidId, hash);
    }

    private List<String> queued() {
        return jdbcTemplate.queryForList("SELECT kind || ':' || item_key FROM orcid_updater.work_item ORDER BY item_key", String.class);
    }

    @Test
    @DisplayName("By email: entries removed at ORCID, token revoked, nothing left here, statuses queued; others untouched")
    void byEmail() {
        final var bystanderBefore = heldAbout(BYSTANDER);

        final var response = delete("{\"email\": \"Researcher@Example.EDU\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("found", true, "orcidEntriesDeleted", 2,
                "orcidEntriesLeft", 0, "tokenRevoked", true));
        verify(orcid).delete(ID, "access-" + ID, 101L);
        verify(orcid).delete(ID, "access-" + ID, 102L);
        verify(oauth).revoke("access-" + ID);
        assertThat(heldAbout(ID)).isZero();
        assertThat(heldAbout(BYSTANDER)).isEqualTo(bystanderBefore);
        assertThat(queued()).containsExactly("STATUS:" + TYPED_ID, "STATUS:" + ID);
    }

    @Test
    @DisplayName("ORCID unavailable part-way: 502 and nothing here deleted; trying again finishes the job")
    void orcidUnavailableThenRetry() {
        doThrow(new OrcidWriteException(Kind.RETRYABLE, "ORCID returned 503")).when(orcid).delete(ID, "access-" + ID, 102L);
        final var before = heldAbout(ID);

        final var failed = delete("{\"orcidId\": \"%s\"}".formatted(ID));

        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(failed.getBody()).containsEntry("code", "ORCID_UNAVAILABLE");
        assertThat(heldAbout(ID)).isEqualTo(before);
        assertThat(queued()).isEmpty();

        // the first entry went at ORCID last time, so it now answers 404
        reset(orcid);
        doThrow(new OrcidWriteException(Kind.NOT_FOUND, "ORCID returned 404")).when(orcid).delete(ID, "access-" + ID, 101L);

        final var retried = delete("{\"orcidId\": \"%s\"}".formatted(ID));

        assertThat(retried.getBody()).isEqualTo(Map.of("found", true, "orcidEntriesDeleted", 2,
                "orcidEntriesLeft", 0, "tokenRevoked", true));
        assertThat(heldAbout(ID)).isZero();
    }

    @Test
    @DisplayName("Researcher revoked access at ORCID earlier: no ORCID calls, entries reported left, data deleted")
    void revokedEarlier() {
        researcherRepository.markRevoked(ID, Instant.now());

        final var response = delete("{\"orcidId\": \"%s\", \"email\": \"%s\"}".formatted(ID, EMAIL));

        assertThat(response.getBody()).isEqualTo(Map.of("found", true, "orcidEntriesDeleted", 0,
                "orcidEntriesLeft", 2, "tokenRevoked", false));
        verify(orcid, org.mockito.Mockito.never()).delete(anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong());
        assertThat(heldAbout(ID)).isZero();
    }

    @Test
    @DisplayName("Email of one researcher with the iD of another: 400 and nothing deleted")
    void mismatch() {
        final var before = heldAbout(ID) + heldAbout(BYSTANDER);

        final var response = delete("{\"orcidId\": \"%s\", \"email\": \"%s\"}".formatted(BYSTANDER, EMAIL));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "RESEARCHER_MISMATCH");
        assertThat(heldAbout(ID) + heldAbout(BYSTANDER)).isEqualTo(before);
    }

    @Test
    @DisplayName("Nobody held under that email or iD: found false")
    void nothingHeld() {
        assertThat(delete("{\"email\": \"stranger@example.edu\"}").getBody()).containsEntry("found", false);
        assertThat(delete("{\"orcidId\": \"https://orcid.org/0000-0002-9079-593X\"}").getBody()).containsEntry("found", false);
        assertThat(queued()).isEmpty();
    }
}
