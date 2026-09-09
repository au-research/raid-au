package au.org.raid.inttest;

import au.org.raid.fixtures.APIFixtures;
import au.org.raid.idl.raidv2.api.RaidApi;
import au.org.raid.idl.raidv2.model.*;
import au.org.raid.idl.raidv2.model.ContributorPositionSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ContributorRoleSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ContributorSchemaUriEnum;
import au.org.raid.inttest.client.keycloak.KeycloakClient;
import au.org.raid.inttest.config.IntegrationTestConfig;
import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.factory.RaidUpdateRequestFactory;
import au.org.raid.inttest.service.RaidApiValidationException;
import au.org.raid.inttest.service.TestClient;
import au.org.raid.inttest.service.TokenService;
import au.org.raid.inttest.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(classes = IntegrationTestConfig.class)
public class AbstractIntegrationTest {
    /**
     * RAID-877: the "raid-au" Keycloak group backing the default fixture user's service point.
     * Stable across every environment - it comes from the committed realm export
     * ({@code iam/realms/raid-realm.json}) - unlike the service_point.id it backs, which is a
     * Postgres-sequence-allocated value that differs per environment (see
     * {@link #resolveServicePointId(String)}).
     */
    protected static final String RAID_AU_GROUP_ID = "169bd3f3-dd42-4ac0-b89a-fb49648e5eff";

    /**
     * RAID-877: the "RAiD AU Test Registry 2" Keycloak group, a genuinely different, real
     * service point used to test cross-tenant isolation. Also from the committed realm export,
     * so stable across environments.
     */
    protected static final String RAID_AU_REGISTRY_2_GROUP_ID = "ba0b01a6-726f-464f-b501-454a10096826";

    /**
     * RAID-877: {@code service_point.id} is allocated from a Postgres sequence
     * ({@code B25__baseline.sql}) and assigned per environment by the Deploy stage's
     * Configure-ServicePoints action, so it is NOT safe to hardcode (a branch test deployment
     * can - and did - allocate a different id to the same group than the local dev database).
     * The Keycloak group id, by contrast, comes from the committed realm export and is stable
     * everywhere, so resolve the real id at runtime by looking it up via {@code GET /service-point/}
     * and matching on {@code groupId}. Cached per groupId for the life of the JVM, since the
     * mapping cannot change during a test run.
     */
    private static final Map<String, Long> SERVICE_POINT_ID_BY_GROUP_ID = new ConcurrentHashMap<>();

    protected LocalDate today = LocalDate.now();
    protected RaidCreateRequest createRequest;

    protected RaidApi raidApi;

    @Autowired
    protected UserService userService;

    @Autowired
    protected KeycloakClient keycloakClient;

    protected UserContext userContext;

    @Autowired
    protected TestClient testClient;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected Contract feignContract;
    @Autowired
    protected RaidUpdateRequestFactory raidUpdateRequestFactory;

    @Autowired
    protected TokenService tokenService;
    private TestInfo testInfo;

    @BeforeEach
    public void setupTestToken() {
        createRequest = APIFixtures.newCreateRequest();

        userContext = userService.createUser("raid-au", "service-point-user");
        raidApi = testClient.raidApi(userContext.getToken());
    }

    @AfterEach
    void tearDown() {
        userService.deleteUser(userContext.getId());
    }

    @BeforeEach
    public void init(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    protected String getName() {
        return testInfo.getDisplayName();
    }

    /**
     * RAID-877: resolves the real {@code service_point.id} for a Keycloak groupId at runtime,
     * rather than relying on a hardcoded literal that only happens to be correct locally. See
     * {@link #SERVICE_POINT_ID_BY_GROUP_ID} for why this is necessary.
     */
    protected Long resolveServicePointId(final String groupId) {
        return SERVICE_POINT_ID_BY_GROUP_ID.computeIfAbsent(groupId, id -> {
            final var servicePointApi = testClient.servicePointApi(userContext.getToken());
            final var servicePoints = servicePointApi.findAllServicePoints().getBody();
            assertThat(servicePoints)
                    .describedAs("GET /service-point/ should return the fixture service points")
                    .isNotNull();

            return servicePoints.stream()
                    .filter(sp -> id.equals(sp.getGroupId()))
                    .map(ServicePoint::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No service point found for Keycloak group " + id));
        });
    }

    protected Long raidAuServicePointId() {
        return resolveServicePointId(RAID_AU_GROUP_ID);
    }

    protected Long raidAuRegistry2ServicePointId() {
        return resolveServicePointId(RAID_AU_REGISTRY_2_GROUP_ID);
    }

    public Contributor isniContributor(
            final String isni,
            final String position,
            final String role,
            final LocalDate startDate,
            final String status
    ) {

        final var contributor = new Contributor()
                .id(isni)
                .contact(true)
                .leader(true)
                .schemaUri(ContributorSchemaUriEnum.fromValue(ISNI_SCHEMA_URI))
                .position(List.of(new ContributorPosition()
                        .schemaUri(ContributorPositionSchemaUriEnum.fromValue(CONTRIBUTOR_POSITION_SCHEMA_URI))
                        .id(ContributorPositionIdEnum.fromValue(position))
                        .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .role(List.of(
                        new ContributorRole()
                                .schemaUri(ContributorRoleSchemaUriEnum.fromValue(CONTRIBUTOR_ROLE_SCHEMA_URI))
                                .id(ContributorRoleIdEnum.fromValue(role))));

        if (status != null) {
            contributor.setStatus(status);
        }
        return contributor;
    }


    protected void failOnError(final Exception e) {
        if (e instanceof RaidApiValidationException) {
            final var responseBody = ((RaidApiValidationException) e).getBadRequest().responseBody()
                    .map(byteBuffer -> {
                        if (byteBuffer.hasArray()) {
                            return new String(byteBuffer.array());
                        }
                        return "";
                    }).orElse("");

            fail(responseBody);
        } else if (e instanceof RetryableException) {
            final var status = ((RetryableException) e).status();

            final var responseBody = ((RetryableException) e).responseBody()
                    .map(byteBuffer -> {
                        if (byteBuffer.hasArray()) {
                            return new String(byteBuffer.array());
                        }
                        return "";
                    }).orElse("");

            fail("status: %s: %s".formatted(status, responseBody));
        } else {
            fail(e.getMessage());
        }
    }
}