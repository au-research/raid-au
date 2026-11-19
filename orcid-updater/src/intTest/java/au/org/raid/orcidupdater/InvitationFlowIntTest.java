package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.InvitationLinkToken;
import au.org.raid.orcidupdater.crypto.LinkCipher;
import au.org.raid.orcidupdater.raidapi.RaidApiClient;
import au.org.raid.orcidupdater.raidapi.RaidSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAID-1002 invitations against a real PostgreSQL. The RAiD API, Keycloak and the mail server are
 * stubbed; the database, transactions, advisory lock and crypto are real.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Invitation flow")
class InvitationFlowIntTest {
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String ORCID_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String EMAIL = "researcher@example.edu";

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
        // these tests drive the work queue themselves, or not at all
        registry.add("orcid-updater.work.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EmailHasher emailHasher;
    @Autowired
    private LinkCipher linkCipher;

    @MockitoBean
    private RaidApiClient raidApiClient;
    @MockitoBean
    private JavaMailSender mailSender;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM orcid_updater.invitation");
        jdbcTemplate.update("DELETE FROM orcid_updater.work_item");
        jdbcTemplate.update("DELETE FROM orcid_updater.researcher");
        reset(mailSender);
        when(jwtDecoder.decode(anyString())).thenAnswer(call -> Jwt.withTokenValue(call.getArgument(0))
                .header("alg", "none").subject("minter").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).build());
        when(raidApiClient.findEditableRaid(HANDLE, "minter-token")).thenReturn(
                new RaidSummary(HANDLE, "Coral reef resilience", "Reef Research Institute", List.of(ORCID_ID)));
    }

    private ResponseEntity<Map> invite(final String email) {
        final var headers = new HttpHeaders();
        headers.setBearerAuth("minter-token");
        headers.setContentType(MediaType.APPLICATION_JSON);
        final var body = """
                {"handle": "%s", "contributorId": "%s", "email": "%s"}
                """.formatted(HANDLE, ORCID_ID, email);
        return restTemplate.exchange("/invitations", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    private int invitationCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater.invitation", Integer.class);
    }

    private String sentLink() {
        final var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        final var text = captor.getValue().getText();
        final var start = text.indexOf("https://app.example.org/invitations/");
        return text.substring(start, text.indexOf('\n', start));
    }

    @Test
    @DisplayName("New invitation: one row, one email, and the link opens against the stored token hash")
    void newInvitation() {
        final var response = invite("Researcher@Example.EDU");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "AWAITING_AUTHENTICATION").containsEntry("emailSent", true);
        assertThat(invitationCount()).isEqualTo(1);

        final var link = sentLink();
        final var token = InvitationLinkToken.parse(link.substring(link.lastIndexOf('/') + 1)).orElseThrow();
        final var stored = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM orcid_updater.invitation WHERE orcid_id = ?", byte[].class, ORCID_ID);
        assertThat(stored).isEqualTo(token.lookupHash());
        assertThat(linkCipher.decrypt(token.sealedEmailHash(), token.random())).contains(emailHasher.hash(EMAIL));
        // the worker pushes AWAITING_AUTHENTICATION to the RAiD API
        assertThat(jdbcTemplate.queryForList("SELECT kind || ':' || item_key FROM orcid_updater.work_item", String.class))
                .containsExactly("STATUS:" + ORCID_ID);
    }

    @Test
    @DisplayName("Neither the email nor its fingerprint is stored anywhere for a pending invitation")
    void nothingAboutTheEmailStored() {
        invite(EMAIL);

        final var dump = jdbcTemplate.queryForList("SELECT row_to_json(i)::text FROM orcid_updater.invitation i", String.class);
        assertThat(dump).hasSize(1);
        assertThat(dump.get(0)).doesNotContainIgnoringCase("researcher").doesNotContain("example.edu");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater.researcher", Integer.class)).isZero();

        // and no column could hold the fingerprint: the hex of it appears nowhere
        final var fingerprintHex = jdbcTemplate.queryForObject("SELECT encode(?::bytea, 'hex')", String.class,
                (Object) emailHasher.hash(EMAIL));
        assertThat(dump.get(0)).doesNotContain(fingerprintHex);
    }

    @Test
    @DisplayName("Second save while the link is valid: no second email or row")
    void onePerPerson() {
        invite(EMAIL);
        clearInvocations(mailSender);

        final var second = invite(EMAIL);

        assertThat(second.getBody()).containsEntry("status", "AWAITING_AUTHENTICATION").containsEntry("emailSent", false);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(invitationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Expired invitation: the next save sends a new one")
    void expiredGetsANewOne() {
        invite(EMAIL);
        jdbcTemplate.update("UPDATE orcid_updater.invitation SET expires_at = now() - interval '1 minute'");
        clearInvocations(mailSender);

        final var response = invite(EMAIL);

        assertThat(response.getBody()).containsEntry("emailSent", true);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        assertThat(invitationCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Email cannot be sent: 502 EMAIL_DELIVERY_FAILED and no pending invitation remains")
    void emailFailureRollsBack() {
        doThrow(new MailSendException("550 no such user " + EMAIL)).when(mailSender).send(any(SimpleMailMessage.class));

        final var response = invite(EMAIL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("code", "EMAIL_DELIVERY_FAILED");
        assertThat(response.getBody().toString()).doesNotContain(EMAIL);
        assertThat(invitationCount()).isZero();
    }

    @Test
    @DisplayName("Researcher who chose Remember me: their iD comes back, no email, nothing recorded")
    void consentedMatch() {
        final var authenticatedId = "https://orcid.org/0000-0001-5109-3700";
        jdbcTemplate.update("INSERT INTO orcid_updater.researcher (orcid_id, email_hash, consented_at) VALUES (?, ?, now())",
                authenticatedId, emailHasher.hash(EMAIL));

        final var response = invite("RESEARCHER@example.edu");

        assertThat(response.getBody()).containsEntry("contributorId", authenticatedId)
                .containsEntry("status", "AUTHENTICATED").containsEntry("emailSent", false);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(invitationCount()).isZero();
    }

    @Test
    @DisplayName("Two saves at the same moment: one email and one row")
    void concurrentSaves() throws Exception {
        final var start = new CountDownLatch(1);
        final Callable<ResponseEntity<Map>> save = () -> {
            start.await();
            return invite(EMAIL);
        };
        final var executor = Executors.newFixedThreadPool(2);
        try {
            final var first = executor.submit(save);
            final var second = executor.submit(save);
            start.countDown();

            assertThat(List.of(first.get().getStatusCode(), second.get().getStatusCode()))
                    .containsOnly(HttpStatus.OK);
        } finally {
            executor.shutdownNow();
        }

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        assertThat(invitationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("The consent constraint stops a fingerprint being stored without a consent time")
    void consentConstraint() {
        final var thrown = org.assertj.core.api.Assertions.catchThrowable(() -> jdbcTemplate.update(
                "INSERT INTO orcid_updater.researcher (orcid_id, email_hash) VALUES (?, ?)", ORCID_ID, new byte[]{1}));

        assertThat(thrown).hasMessageContaining("researcher_consent_recorded");
    }
}
