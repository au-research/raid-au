package au.org.raid.inttest;

import au.org.raid.fixtures.APIFixtures;
import au.org.raid.idl.raidv2.api.RaidApi;
import au.org.raid.idl.raidv2.model.*;
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

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(classes = IntegrationTestConfig.class)
public class AbstractIntegrationTest {
    protected static final Long UQ_SERVICE_POINT_ID = 20000002L;
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

    public Contributor isniContributor(
            final String isni,
            final String position,
            final String role,
            final LocalDate startDate,
            final String email
    ) {
        return new Contributor()
                .id(isni)
                .contact(true)
                .leader(true)
                .schemaUri(ISNI_SCHEMA_URI)
                .position(List.of(new ContributorPosition()
                        .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                        .id(position)
                        .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .role(List.of(
                        new ContributorRole()
                                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                .id(role)));
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