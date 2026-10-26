package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.InvitationLinkToken;
import au.org.raid.orcidupdater.crypto.LinkCipher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.raidapi.RaidSummary;
import au.org.raid.orcidupdater.signin.OrcidOAuthClient;
import au.org.raid.orcidupdater.signin.OrcidOAuthClient.OrcidSignIn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RAID-1003 researcher pages and ORCID sign-in against a real PostgreSQL. Only ORCID's token
 * exchange is stubbed; the pages, OAuth state, transactions and encryption are real.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ORCID sign-in flow")
class SignInFlowIntTest {
    private static final String TYPED_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://sandbox.orcid.org/0000-0001-5109-3700";
    private static final String EMAIL = "researcher@example.edu";
    private static final RaidSummary RAID = new RaidSummary("10.82841/86bf1b6c", "Coral reef resilience",
            "Reef Research Institute", List.of(TYPED_ID));

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
        registry.add("orcid-updater.public-base-url", () -> "https://orcid-updater.example.org");
        registry.add("orcid-updater.contact-email", () -> "privacy@example.org");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private EmailHasher emailHasher;
    @Autowired
    private LinkCipher linkCipher;
    @Autowired
    private TokenCipher tokenCipher;

    @MockitoSpyBean
    private OrcidOAuthClient orcidOAuthClient;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private String link;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM orcid_updater.oauth_state");
        jdbcTemplate.update("DELETE FROM orcid_updater.invitation");
        jdbcTemplate.update("DELETE FROM orcid_updater.researcher");
        link = invite(TYPED_ID, EMAIL);
        signsInAs(TYPED_ID);
    }

    /** Registers an invitation the way RAID-1002 does and returns the link token. */
    private String invite(final String orcidId, final String email) {
        final var token = InvitationLinkToken.issue(emailHasher.hash(email), linkCipher);
        final var now = Instant.now();
        invitationRepository.insert(UUID.randomUUID(), token.lookupHash(), orcidId, RAID, now, now.plus(Duration.ofDays(14)));
        return token.encode();
    }

    private void signsInAs(final String orcidId) {
        doReturn(new OrcidSignIn(orcidId, "access-token-" + orcidId, "refresh-token-" + orcidId,
                "/read-limited /activities/update")).when(orcidOAuthClient).exchange(anyString());
    }

    /** Clicks the button and returns the state ORCID would send back. */
    private String startSignIn(final String token, final boolean rememberMe) throws Exception {
        final var location = mockMvc.perform(post("/invitations/" + token).param("rememberMe", String.valueOf(rememberMe)))
                .andExpect(status().isSeeOther())
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).startsWith("https://sandbox.orcid.org/oauth/authorize?");
        return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state");
    }

    private String callback(final String state, final String code, final int expectedStatus) throws Exception {
        final var request = get("/orcid/callback").param("state", state);
        if (code != null) {
            request.param("code", code);
        }
        return mockMvc.perform(request).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private int count(final String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater." + table, Integer.class);
    }

    @Test
    @DisplayName("Landing page shows the stored title and service point, without anything being sent to ORCID")
    void landingPage() throws Exception {
        mockMvc.perform(get("/invitations/" + link)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Coral reef resilience"),
                        org.hamcrest.Matchers.containsString("Reef Research Institute"))));
        assertThat(count("oauth_state")).isZero();
    }

    @Test
    @DisplayName("Signs in with Remember me: token encrypted, fingerprint and consent stored, link used up, state gone")
    void rememberMe() throws Exception {
        final var state = startSignIn(link, true);
        assertThat(count("oauth_state")).isEqualTo(1);

        assertThat(callback(state, "code-1", 200)).contains("Your ORCID iD is confirmed", TYPED_ID);

        final var row = jdbcTemplate.queryForMap(
                "SELECT access_token, refresh_token, scope, email_hash, consented_at FROM orcid_updater.researcher WHERE orcid_id = ?",
                TYPED_ID);
        assertThat(tokenCipher.decrypt((byte[]) row.get("access_token"), TYPED_ID)).isEqualTo("access-token-" + TYPED_ID);
        assertThat(tokenCipher.decrypt((byte[]) row.get("refresh_token"), TYPED_ID)).isEqualTo("refresh-token-" + TYPED_ID);
        assertThat(row.get("scope")).isEqualTo("/read-limited /activities/update");
        assertThat((byte[]) row.get("email_hash")).isEqualTo(emailHasher.hash(EMAIL));
        assertThat(row.get("consented_at")).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT authenticated_orcid_id FROM orcid_updater.invitation WHERE used_at IS NOT NULL", String.class))
                .isEqualTo(TYPED_ID);
        assertThat(count("oauth_state")).isZero();

        // the link is used up, and the callback can't be replayed
        mockMvc.perform(get("/invitations/" + link)).andExpect(status().isGone());
        callback(state, "code-1", 400);
    }

    @Test
    @DisplayName("Signs in without Remember me: no fingerprint stored")
    void withoutRememberMe() throws Exception {
        callback(startSignIn(link, false), "code-1", 200);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT email_hash IS NULL AND consented_at IS NULL FROM orcid_updater.researcher WHERE orcid_id = ?",
                Boolean.class, TYPED_ID)).isTrue();
    }

    @Test
    @DisplayName("ORCID returns a different iD: that iD is authenticated and recorded on the invitation")
    void differentId() throws Exception {
        signsInAs(OTHER_ID);

        assertThat(callback(startSignIn(link, true), "code-1", 200)).contains(OTHER_ID, "different iD");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT authenticated_orcid_id FROM orcid_updater.invitation WHERE orcid_id = ?", String.class, TYPED_ID))
                .isEqualTo(OTHER_ID);
        assertThat(jdbcTemplate.queryForList("SELECT orcid_id FROM orcid_updater.researcher", String.class))
                .containsExactly(OTHER_ID);
    }

    @Test
    @DisplayName("Declines at ORCID: nothing stored, the link still works")
    void declined() throws Exception {
        final var state = startSignIn(link, true);

        mockMvc.perform(get("/orcid/callback").param("state", state).param("error", "access_denied"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nothing was authorised")));

        verify(orcidOAuthClient, never()).exchange(anyString());
        assertThat(count("researcher")).isZero();
        assertThat(count("oauth_state")).isZero();
        mockMvc.perform(get("/invitations/" + link)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("State that matches no sign-in: 400 and nothing stored")
    void forgedState() throws Exception {
        startSignIn(link, true);

        callback("forged-state", "code-1", 400);

        verify(orcidOAuthClient, never()).exchange(anyString());
        assertThat(count("researcher")).isZero();
    }

    @Test
    @DisplayName("Sign-in older than the timeout: 400 and nothing stored")
    void expiredState() throws Exception {
        final var state = startSignIn(link, false);
        jdbcTemplate.update("UPDATE orcid_updater.oauth_state SET expires_at = now() - interval '1 second'");

        callback(state, "code-1", 400);

        assertThat(count("researcher")).isZero();
    }

    @Test
    @DisplayName("Two clicks then two callbacks: the first authenticates, the second finds the link used")
    void doubleSubmit() throws Exception {
        final var first = startSignIn(link, false);
        final var second = startSignIn(link, false);

        callback(first, "code-1", 200);
        assertThat(callback(second, "code-2", 410)).contains("already been used");
        assertThat(count("researcher")).isEqualTo(1);
    }

    @Test
    @DisplayName("Latest consent wins: one email fingerprint belongs to one researcher")
    void latestConsentWins() throws Exception {
        callback(startSignIn(link, true), "code-1", 200);

        signsInAs(OTHER_ID);
        callback(startSignIn(invite(OTHER_ID, EMAIL), true), "code-2", 200);

        assertThat(jdbcTemplate.queryForList("SELECT orcid_id FROM orcid_updater.researcher WHERE email_hash IS NOT NULL",
                String.class)).containsExactly(OTHER_ID);
        assertThat(count("researcher")).isEqualTo(2);
    }

    @Test
    @DisplayName("Signing in again without Remember me keeps an earlier consent")
    void earlierConsentKept() throws Exception {
        callback(startSignIn(link, true), "code-1", 200);

        callback(startSignIn(invite(TYPED_ID, EMAIL), false), "code-2", 200);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT email_hash IS NOT NULL FROM orcid_updater.researcher WHERE orcid_id = ?", Boolean.class, TYPED_ID))
                .isTrue();
    }
}
