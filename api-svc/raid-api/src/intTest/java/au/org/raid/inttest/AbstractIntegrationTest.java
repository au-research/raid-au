package au.org.raid.inttest;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static au.org.raid.inttest.service.TestConstants.*;
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
        createRequest = newCreateRequest();

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

    protected RaidCreateRequest newCreateRequest() {
        String initialTitle = UUID.randomUUID().toString();
        final var descriptions = new ArrayList<Description>();
        descriptions.add(new Description()
                .language(new Language()
                        .schemaUri(LANGUAGE_SCHEMA_URI)
                        .id(LANGUAGE_ID))
                .type(new DescriptionType()
                        .id(PRIMARY_DESCRIPTION_TYPE)
                        .schemaUri(DESCRIPTION_TYPE_SCHEMA_URI))
                .text("stuff about the int test raid")
                .language(new Language()
                        .schemaUri(LANGUAGE_SCHEMA_URI)
                        .id(LANGUAGE_ID)));


        return new RaidCreateRequest()
                .title(List.of(new Title()
                        .language(new Language()
                                .schemaUri(LANGUAGE_SCHEMA_URI)
                                .id(LANGUAGE_ID)
                        )
                        .type(new TitleType()
                                .id(PRIMARY_TITLE_TYPE)
                                .schemaUri(TITLE_TYPE_SCHEMA_URI))
                        .text(initialTitle)
                        .startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .date(new Date().startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .description(descriptions)

                .contributor(List.of(
                        orcidContributor(
                                REAL_TEST_ORCID, PRINCIPAL_INVESTIGATOR_POSITION, SOFTWARE_CONTRIBUTOR_ROLE, today, CONTRIBUTOR_EMAIL)
                ))
                .organisation(List.of(organisation(
                        REAL_TEST_ROR, LEAD_RESEARCH_ORGANISATION, today)))
                .access(new Access()
                        .statement(new AccessStatement()
                                .text("Embargoed")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LANGUAGE_SCHEMA_URI)))
                        .type(new AccessType()
                                .id(EMBARGOED_ACCESS_TYPE)
                                .schemaUri(ACCESS_TYPE_SCHEMA_URI))
                        .embargoExpiry(LocalDate.now().plusMonths(1)))
                .spatialCoverage(List.of(new SpatialCoverage()
                        .id(GEONAMES_MELBOURNE)
                        .place(List.of(new SpatialCoveragePlace()
                                .text("Melbourne")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LANGUAGE_SCHEMA_URI))))
                        .schemaUri(GEONAMES_SCHEMA_URI)))
                .subject(List.of(
                        new Subject()
                                .id("https://linked.data.gov.au/def/anzsrc-for/2020/3702")
                                .schemaUri("https://vocabs.ardc.edu.au/viewById/316")
                                .keyword(List.of(new SubjectKeyword()
                                        .language(new Language()
                                                .id("eng")
                                                .schemaUri("https://www.iso.org/standard/74575.html"))
                                        .text("ENES")
                                ))));
    }
    public Contributor orcidContributor(
            final String orcid,
            final String position,
            final String role,
            final LocalDate startDate,
            final String email
    ) {
        return new Contributor()
                .id(orcid)
                .contact(true)
                .leader(true)
                .schemaUri(ORCID_SCHEMA_URI)
                .position(List.of(new ContributorPosition()
                        .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                        .id(position)
                        .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .role(List.of(
                        new ContributorRole()
                                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                .id(role)));
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

    public Organisation organisation(
            String ror,
            String role,
            LocalDate today
    ) {
        return new Organisation()
                .id(ror)
                .schemaUri(ORGANISATION_IDENTIFIER_SCHEMA_URI)
                .role(List.of(
                        new OrganisationRole()
                                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                                .id(role)
                                .startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))));
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