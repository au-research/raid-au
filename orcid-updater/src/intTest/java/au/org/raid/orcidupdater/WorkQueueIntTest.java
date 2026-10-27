package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.crypto.ServiceKeyCipher;
import au.org.raid.orcidupdater.crypto.TokenCipher;
import au.org.raid.orcidupdater.invitation.InvitationRepository;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidOrganisation;
import au.org.raid.orcidupdater.orcid.RorClient;
import au.org.raid.orcidupdater.raidapi.RaidApiUnavailableException;
import au.org.raid.orcidupdater.raidapi.RaidServiceClient;
import au.org.raid.orcidupdater.raidapi.RaidSummary;
import au.org.raid.orcidupdater.researcher.ResearcherRepository;
import au.org.raid.orcidupdater.serviceauth.ServiceKeyRepository;
import au.org.raid.orcidupdater.serviceauth.ServiceKeyStore;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.work.WorkProcessor;
import au.org.raid.orcidupdater.work.WorkQueue;
import au.org.raid.orcidupdater.work.WorkRepository;
import au.org.raid.orcidupdater.work.WorkRepository.Kind;
import au.org.raid.orcidupdater.work.WorkRepository.WorkItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RAID-1005 against a real PostgreSQL: the V5 work queue, the expiry sweep, the processor's
 * status and RAiD flows, and the stored service key. The RAiD API, ORCID and ROR are stubbed.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Work queue")
class WorkQueueIntTest {
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OWNER = "https://ror.org/038sjwq14";
    private static final String OPEN = "https://vocabularies.coar-repositories.org/access_rights/c_abf2/";
    private static final String EMBARGOED = "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/";

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
        // the tests call the processor themselves
        registry.add("orcid-updater.work.enabled", () -> "false");
    }

    @Autowired
    private WorkRepository workRepository;
    @Autowired
    private WorkQueue workQueue;
    @Autowired
    private WorkProcessor processor;
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private ResearcherRepository researcherRepository;
    @Autowired
    private TokenCipher tokenCipher;
    @Autowired
    private ServiceKeyStore serviceKeyStore;
    @Autowired
    private ServiceKeyRepository serviceKeyRepository;
    @Autowired
    private ServiceKeyCipher serviceKeyCipher;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private RaidServiceClient raidServiceClient;
    @MockitoBean
    private OrcidMemberClient orcid;
    @MockitoBean
    private RorClient rorClient;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM orcid_updater.work_item");
        jdbcTemplate.update("DELETE FROM orcid_updater.orcid_entry");
        jdbcTemplate.update("DELETE FROM orcid_updater.invitation");
        jdbcTemplate.update("DELETE FROM orcid_updater.researcher");
        when(raidServiceClient.updateContributorStatus(any(), any())).thenReturn(true);
        when(rorClient.find(OWNER)).thenReturn(Optional.of(new OrcidOrganisation("Australian Research Data Commons",
                "Melbourne", "Victoria", "AU", OWNER)));
    }

    private Map<String, Object> row(final Kind kind, final String key) {
        return jdbcTemplate.queryForMap("SELECT * FROM orcid_updater.work_item WHERE kind = ? AND item_key = ?",
                kind.name(), key);
    }

    private int count() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater.work_item", Integer.class);
    }

    private void authenticate(final String orcidId) {
        researcherRepository.saveAuthentication(orcidId, tokenCipher.encrypt("access-" + orcidId, orcidId),
                null, "/read-limited /activities/update", Instant.now());
    }

    private void invite(final String orcidId, final Instant expiresAt) {
        invitationRepository.insert(UUID.randomUUID(), UUID.randomUUID().toString().getBytes(), orcidId,
                new RaidSummary(HANDLE, "Coral reef resilience", "Reef Research Institute", List.of(orcidId)),
                expiresAt.minus(Duration.ofDays(14)), expiresAt);
    }

    private static JsonNode raid(final String accessType) throws Exception {
        return new ObjectMapper().readTree("""
                {"identifier": {"id": "https://raid.org/%s", "owner": {"id": "%s"}},
                 "access": {"type": {"id": "%s"}},
                 "title": [{"text": "Coral reef resilience",
                            "type": {"id": "https://vocabulary.raid.org/title.type.schema/5"}, "startDate": "2025-03-01"}],
                 "contributor": [{"id": "%s", "position": [{"id": "https://vocabulary.raid.org/contributor.position.schema/308",
                                                             "startDate": "2025-03-01"}]}]}
                """.formatted(HANDLE, OWNER, accessType, ID));
    }

    @Nested
    @DisplayName("Queue")
    class Queue {
        @Test
        @DisplayName("A second request for the same item is merged into it")
        void merged() {
            workQueue.requestRaidSync(HANDLE);
            workQueue.requestRaidSync(HANDLE);

            assertThat(count()).isEqualTo(1);
            assertThat(row(Kind.RAID_SYNC, HANDLE).get("generation")).isEqualTo(1);
        }

        @Test
        @DisplayName("Claimed items aren't claimed again until the claim times out")
        void claimed() {
            workQueue.requestStatus(ID);
            final var now = Instant.now().plusSeconds(1);

            assertThat(workRepository.claimDue(now, now.plusSeconds(300), 10)).hasSize(1);
            assertThat(workRepository.claimDue(now.plusSeconds(299), now.plusSeconds(600), 10)).isEmpty();
            assertThat(workRepository.claimDue(now.plusSeconds(301), now.plusSeconds(600), 10)).hasSize(1);
        }

        @Test
        @DisplayName("Requested again while in progress: finishing the earlier request keeps it for another pass")
        void requestedMeanwhile() {
            workQueue.requestRaidSync(HANDLE);
            final var now = Instant.now().plusSeconds(1);
            final var item = workRepository.claimDue(now, now.plusSeconds(300), 10).get(0);
            workQueue.requestRaidSync(HANDLE);

            workRepository.complete(item);

            assertThat(count()).isEqualTo(1);
            assertThat(workRepository.claimDue(now.plusSeconds(1), now.plusSeconds(300), 10))
                    .containsExactly(new WorkItem(Kind.RAID_SYNC, HANDLE, 1, 0));
        }

        @Test
        @DisplayName("Finished: removed")
        void completed() {
            workQueue.requestRaidSync(HANDLE);
            final var now = Instant.now().plusSeconds(1);

            workRepository.complete(workRepository.claimDue(now, now.plusSeconds(300), 10).get(0));

            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("Failed for good: not claimed again until requested again")
        void failedForGood() {
            workQueue.requestRaidSync(HANDLE);
            final var now = Instant.now().plusSeconds(1);
            final var item = workRepository.claimDue(now, now.plusSeconds(300), 10).get(0);

            workRepository.recordFailure(item, "RAiD API returned 503", now, null);

            assertThat(row(Kind.RAID_SYNC, HANDLE).get("failed_at")).isNotNull();
            assertThat(workRepository.claimDue(now.plusSeconds(3600), now.plusSeconds(4000), 10)).isEmpty();

            workQueue.requestRaidSync(HANDLE);

            final var again = row(Kind.RAID_SYNC, HANDLE);
            assertThat(again.get("failed_at")).isNull();
            assertThat(again.get("failure")).isNull();
            assertThat(again.get("attempts")).isEqualTo(0);
        }

        @Test
        @DisplayName("Expired, unused invitations are queued as status changes once each")
        void expirySweep() {
            final var now = Instant.now();
            invite(ID, now.minusSeconds(60));
            invite("https://orcid.org/0000-0001-5109-3700", now.plus(Duration.ofDays(1)));

            assertThat(workRepository.requestStatusForExpiredInvitations(now)).isEqualTo(1);
            assertThat(workRepository.requestStatusForExpiredInvitations(now.plusSeconds(1))).isZero();
            assertThat(row(Kind.STATUS, ID)).isNotNull();
            assertThat(count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Processing")
    class Processing {
        @Test
        @DisplayName("Sign-in to ORCID: AUTHENTICATED pushed, then the RAiD naming them written to ORCID")
        void authenticatedThenWritten() throws Exception {
            authenticate(ID);
            when(raidServiceClient.findHandlesByContributor(ID)).thenReturn(List.of(HANDLE));
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.of(raid(OPEN)));
            when(orcid.create(eq(ID), any(), any())).thenReturn(4242L);
            workQueue.requestStatus(ID);

            // first pass: the status; the RAiD sync it queues is due straight away
            processor.processDue();
            verify(raidServiceClient).updateContributorStatus(ID, ContributorStatus.AUTHENTICATED);
            assertThat(row(Kind.RAID_SYNC, HANDLE)).isNotNull();

            processor.processDue();

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT put_code FROM orcid_updater.orcid_entry WHERE orcid_id = ? AND raid_handle = ?",
                    Long.class, ID, HANDLE)).isEqualTo(4242L);
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("Embargoed RAiD: held back, nothing written, nothing left queued")
        void embargoed() throws Exception {
            authenticate(ID);
            when(raidServiceClient.findRaid(HANDLE)).thenReturn(Optional.of(raid(EMBARGOED)));
            workQueue.requestRaidSync(HANDLE);

            processor.processDue();

            verify(orcid, never()).create(any(), any(), any());
            assertThat(count()).isZero();
        }

        @Test
        @DisplayName("Invitation expired: the status is pushed as UNAUTHENTICATED")
        void expired() {
            invite(ID, Instant.now().minusSeconds(60));

            processor.processDue();

            verify(raidServiceClient).updateContributorStatus(ID, ContributorStatus.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("RAiD API down: the attempt and its failure are kept, and it's tried again after the backoff")
        void apiDown() {
            when(raidServiceClient.findRaid(HANDLE)).thenThrow(new RaidApiUnavailableException("RAiD API unreachable reading " + HANDLE));
            workQueue.requestRaidSync(HANDLE);
            final var before = Instant.now();

            processor.processDue();

            final var row = row(Kind.RAID_SYNC, HANDLE);
            assertThat(row.get("attempts")).isEqualTo(1);
            assertThat(row.get("failure")).isEqualTo("RAiD API unreachable reading " + HANDLE);
            assertThat(row.get("claimed_until")).isNull();
            assertThat(((java.sql.Timestamp) row.get("next_attempt_at")).toInstant())
                    .isCloseTo(before.plusSeconds(30), within(5, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("Service key")
    class ServiceKey {
        @Test
        @DisplayName("Created once and stored encrypted; another instance signs with the same key")
        void sharedAndEncrypted() throws Exception {
            jdbcTemplate.update("DELETE FROM orcid_updater.service_key");
            final var key = instance(serviceKeyCipher).signingKey();
            final var stored = serviceKeyRepository.findCurrent().orElseThrow();

            assertThat(stored.keyId()).isEqualTo(key.getKeyID());
            assertThat(stored.privateKey()).isNotEqualTo(key.toRSAPrivateKey().getEncoded());
            assertThat(instance(serviceKeyCipher).signingKey().getKeyID()).isEqualTo(key.getKeyID());
        }

        /** A store as another updater instance would have it, with nothing cached. */
        private ServiceKeyStore instance(final ServiceKeyCipher cipher) {
            return new ServiceKeyStore(serviceKeyRepository, cipher, transactionTemplate, Clock.systemUTC());
        }

        @Test
        @DisplayName("Stored key can't be decrypted (encryption key changed): replaced by a new one")
        void replacedWhenUnreadable() {
            final var original = instance(serviceKeyCipher).signingKey().getKeyID();
            final var properties = new OrcidUpdaterProperties();
            properties.setTokenEncryptionKey("a-different-token-encryption-key-of-32-chars");

            final var replaced = instance(new ServiceKeyCipher(properties)).signingKey();

            assertThat(replaced.getKeyID()).isNotEqualTo(original);
            assertThat(serviceKeyRepository.findCurrent().orElseThrow().keyId()).isEqualTo(replaced.getKeyID());
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM orcid_updater.service_key", Integer.class))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("JWKS endpoint publishes the public key only, without credentials")
        void jwks() {
            final var response = restTemplate.getForEntity("/.well-known/jwks.json", JsonNode.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            final var jwk = response.getBody().path("keys").get(0);
            assertThat(jwk.path("kty").asText()).isEqualTo("RSA");
            assertThat(jwk.path("use").asText()).isEqualTo("sig");
            assertThat(jwk.path("alg").asText()).isEqualTo("RS256");
            assertThat(jwk.path("kid").asText()).isEqualTo(serviceKeyStore.publicJwk().getKeyID());
            assertThat(jwk.has("d")).isFalse();
            assertThat(jwk.has("p")).isFalse();
        }
    }
}
