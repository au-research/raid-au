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
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                        .id(LANGUAGE_ID))
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320))
                .text("stuff about the int test raid")
                .language(new Language()
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                        .id(LANGUAGE_ID)));


        return new RaidCreateRequest()
                .title(List.of(new Title()
                        .language(new Language()
                                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                                .id(LANGUAGE_ID)
                        )
                        .type(new TitleType()
                                .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5)
                                .schemaUri(TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376))
                        .text(initialTitle)
                        .startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .date(new Date().startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .description(descriptions)

                .contributor(List.of(contributor(
                        REAL_TEST_ORCID, ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307, ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SOFTWARE_, today, CONTRIBUTOR_EMAIL)))
                .organisation(List.of(organisation(
                        REAL_TEST_ROR, OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182, today)))
                .access(new Access()
                        .statement(new AccessStatement()
                                .text("Embargoed")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)))
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_)
                                .schemaUri(AccessTypeSchemaUriEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_))
                        .embargoExpiry(LocalDate.now().plusMonths(1)))
                .spatialCoverage(List.of(new SpatialCoverage()
                        .id(GEONAMES_MELBOURNE)
                        .place(List.of(new SpatialCoveragePlace()
                                .text("Melbourne")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML))))
                        .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)))
                .subject(List.of(
                        new Subject()
                                .id("https://linked.data.gov.au/def/anzsrc-for/2020/3702")
                                .schemaUri(SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316)
                                .keyword(List.of(new SubjectKeyword()
                                        .language(new Language()
                                                .id("eng")
                                                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML))
                                        .text("ENES")
                                ))));
    }
    public Contributor contributor(
            final String orcid,
            final ContributorPositionIdEnum position,
            final ContributorRoleIdEnum role,
            final LocalDate startDate,
            final String email
    ) {
        return new Contributor()
                .id(orcid)
                .contact(true)
                .leader(true)
//                .email(email)
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .position(List.of(new ContributorPosition()
                        .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                        .id(position)
                        .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .role(List.of(
                        new ContributorRole()
                                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                                .id(role)));
    }

    public Organisation organisation(
            String ror,
            OrganizationRoleIdEnum role,
            LocalDate today
    ) {
        return new Organisation()
                .id(ror)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(
                        new OrganisationRole()
                                .schemaUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359)
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