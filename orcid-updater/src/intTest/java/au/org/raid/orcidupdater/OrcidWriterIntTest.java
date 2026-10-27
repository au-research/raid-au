package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.crypto.EmailHasher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidOrganisation;
import au.org.raid.orcidupdater.orcid.OrcidRaid;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import au.org.raid.orcidupdater.orcid.OrcidWriteException;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import au.org.raid.orcidupdater.orcid.OrcidWriter;
import au.org.raid.orcidupdater.orcid.OrcidWriter.EntryOutcome;
import au.org.raid.orcidupdater.orcid.OrcidWriter.Outcome;
import au.org.raid.orcidupdater.orcid.RorClient;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAID-1004 writes against a real PostgreSQL. ORCID's member API and ROR are stubbed; the V4
 * migration, put codes, failure records, revocation, locks and token decryption are real.
 */
@Testcontainers
@SpringBootTest
@DisplayName("ORCID writes")
class OrcidWriterIntTest {
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final String OWNER = "https://ror.org/038sjwq14";
    private static final String PRIMARY_TITLE = "https://vocabulary.raid.org/title.type.schema/5";
    private static final String PRINCIPAL_INVESTIGATOR = "https://vocabulary.raid.org/contributor.position.schema/307";
    private static final String CO_INVESTIGATOR = "https://vocabulary.raid.org/contributor.position.schema/308";

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
        registry.add("orcid-updater.orcid-write.initial-backoff", () -> "0ms");
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
    private OrcidWriter writer;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ResearcherRepository researcherRepository;
    @Autowired
    private TokenCipher tokenCipher;
    @Autowired
    private EmailHasher emailHasher;

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
        authenticate(ID);
        authenticate(OTHER_ID);
        when(rorClient.find(OWNER)).thenReturn(Optional.of(new OrcidOrganisation("Australian Research Data Commons",
                "Melbourne", "Victoria", "AU", OWNER)));
    }

    /** Stores tokens the way RAID-1003's sign-in does. */
    private void authenticate(final String orcidId) {
        researcherRepository.saveAuthentication(orcidId, tokenCipher.encrypt("access-" + orcidId, orcidId),
                tokenCipher.encrypt("refresh-" + orcidId, orcidId), "/read-limited /activities/update", Instant.now());
    }

    private static OrcidRaid raid(final Contributor... contributors) {
        return new OrcidRaid(HANDLE, "https://raid.org/" + HANDLE, OWNER,
                List.of(new Title("Coral reef resilience", PRIMARY_TITLE, "2025-03-01", null)),
                List.of(contributors));
    }

    private static Contributor contributor(final String id, final String position) {
        return new Contributor(id, List.of(new Position(position, "2025-03-01", null)));
    }

    private Map<String, Object> entry(final String orcidId) {
        return jdbcTemplate.queryForMap("""
                SELECT put_code, failure, last_attempt_at, last_success_at
                FROM orcid_updater.orcid_entry WHERE orcid_id = ? AND raid_handle = ?""", orcidId, HANDLE);
    }

    private int entries() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater.orcid_entry", Integer.class);
    }

    @Test
    @DisplayName("First save creates the entry and keeps its put code; the next save updates it by put code")
    void createThenUpdate() {
        when(orcid.create(eq(ID), eq("access-" + ID), any())).thenReturn(1234567L);

        assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                .containsExactly(new EntryOutcome(ID, Outcome.WRITTEN));
        assertThat(entry(ID)).containsEntry("put_code", 1234567L).containsEntry("failure", null);
        assertThat(entry(ID).get("last_success_at")).isNotNull();

        writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));

        verify(orcid, times(1)).create(any(), any(), any());
        verify(orcid).update(eq(ID), eq("access-" + ID), eq(1234567L), argThat(m -> m.get("put-code").equals(1234567L)));
        assertThat(entries()).isEqualTo(1);
    }

    @Test
    @DisplayName("Entry deleted at ORCID: created again under a new put code")
    void recreated() {
        when(orcid.create(eq(ID), any(), any())).thenReturn(1L, 2L);
        writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));
        doThrow(new OrcidWriteException(Kind.NOT_FOUND, "ORCID returned 404")).when(orcid).update(any(), any(), anyLong(), any());

        writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));

        assertThat(entry(ID)).containsEntry("put_code", 2L);
    }

    @Test
    @DisplayName("Every contributor's entry names the RAiD owner's organisation")
    void organisation() {
        when(orcid.create(any(), any(), any())).thenReturn(1L, 2L);

        writer.sync(raid(contributor(ID, PRINCIPAL_INVESTIGATOR), contributor(OTHER_ID, CO_INVESTIGATOR)));

        verify(orcid).create(eq(ID), any(), argThat(m -> m.get("organization").toString().contains(OWNER)));
        verify(orcid).create(eq(OTHER_ID), any(), argThat(m -> m.get("organization").toString().contains(OWNER)));
    }

    @Test
    @DisplayName("Token revoked: researcher marked revoked, no longer matched by email or written, until signing in again")
    void revoked() {
        researcherRepository.recordConsent(ID, emailHasher.hash("researcher@example.edu"), Instant.now());
        when(orcid.create(eq(ID), any(), any())).thenThrow(new OrcidWriteException(Kind.TOKEN_REJECTED, "ORCID returned 401"));

        assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                .containsExactly(new EntryOutcome(ID, Outcome.TOKEN_REJECTED));
        assertThat(jdbcTemplate.queryForObject("SELECT revoked_at IS NOT NULL FROM orcid_updater.researcher WHERE orcid_id = ?",
                Boolean.class, ID)).isTrue();
        assertThat(entry(ID)).containsEntry("put_code", null);
        assertThat((String) entry(ID).get("failure")).contains("access revoked");
        assertThat(researcherRepository.findOrcidIdByConsentedEmailHash(emailHasher.hash("researcher@example.edu"))).isEmpty();

        assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                .containsExactly(new EntryOutcome(ID, Outcome.NOT_AUTHENTICATED));
        verify(orcid, times(1)).create(any(), any(), any());

        authenticate(ID);
        assertThat(researcherRepository.findAccessToken(ID)).isPresent();
    }

    @Test
    @DisplayName("ORCID unavailable: retried, then the failure is recorded and the put code kept")
    void unavailable() {
        when(orcid.create(eq(ID), any(), any())).thenReturn(1234567L);
        writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));
        doThrow(new OrcidWriteException(Kind.RETRYABLE, "ORCID returned 503")).when(orcid).update(any(), any(), anyLong(), any());

        assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                .containsExactly(new EntryOutcome(ID, Outcome.FAILED));

        verify(orcid, times(3)).update(any(), any(), anyLong(), any());
        assertThat(entry(ID)).containsEntry("put_code", 1234567L)
                .containsEntry("failure", "ORCID returned 503 (after 3 attempts)");

        // the next successful save clears the failure
        reset(orcid);
        writer.sync(raid(contributor(ID, CO_INVESTIGATOR)));
        assertThat(entry(ID)).containsEntry("failure", null);
    }

    @Test
    @DisplayName("Contributor removed from the RAiD: their entry is deleted at ORCID and forgotten")
    void removed() {
        when(orcid.create(eq(ID), any(), any())).thenReturn(1L);
        when(orcid.create(eq(OTHER_ID), any(), any())).thenReturn(2L);
        writer.sync(raid(contributor(ID, CO_INVESTIGATOR), contributor(OTHER_ID, CO_INVESTIGATOR)));

        assertThat(writer.sync(raid(contributor(ID, CO_INVESTIGATOR))))
                .containsExactly(new EntryOutcome(ID, Outcome.WRITTEN), new EntryOutcome(OTHER_ID, Outcome.REMOVED));

        verify(orcid).delete(OTHER_ID, "access-" + OTHER_ID, 2L);
        assertThat(jdbcTemplate.queryForList("SELECT orcid_id FROM orcid_updater.orcid_entry", String.class))
                .containsExactly(ID);
    }
}
