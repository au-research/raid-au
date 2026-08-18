package au.org.raid.api.datacite;

import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.HttpHeadersFactory;
import au.org.raid.api.factory.datacite.DataciteAlternateIdentifierFactory;
import au.org.raid.api.factory.datacite.DataciteDateFactory;
import au.org.raid.api.factory.datacite.DataciteIdentifierFactory;
import au.org.raid.api.factory.datacite.DataciteRelatedIdentifierFactory;
import au.org.raid.api.factory.datacite.DataciteTitleFactory;
import au.org.raid.api.factory.datacite.DataciteTypesFactory;
import au.org.raid.api.model.datacite.doi.DataciteAttributesDto;
import au.org.raid.api.model.datacite.doi.DataciteCreator;
import au.org.raid.api.model.datacite.doi.DataciteDto;
import au.org.raid.api.model.datacite.doi.DataciteRelatedIdentifier;
import au.org.raid.api.model.datacite.doi.DataciteRequest;
import au.org.raid.idl.raidv2.model.Date;
import au.org.raid.idl.raidv2.model.RelatedRaid;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeSchemaUriEnum;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
import au.org.raid.idl.raidv2.model.TitleTypeIdEnum;
import au.org.raid.idl.raidv2.model.TitleTypeSchemaURIEnum;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Permanent regression test guarding {@code DataciteRelatedIdentifierFactory.create(RelatedRaid)}
 * (see RAID-797): DataCite's own {@code relatedIdentifierType} vocabulary (Metadata Schema
 * 4.6/4.7) includes a native {@code "RAiD"} value, and the production factory now emits that
 * value (with {@code resourceTypeGeneral = "Project"}) for every {@code relatedRaid} entry on a
 * minted RAiD, instead of a generic/incorrect type. This test proves the REAL DataCite test API
 * (https://api.test.datacite.org/dois) accepts that exact payload shape - it is not a stub or a
 * MockServer expectation.
 *
 * <p>It builds the {@code relatedIdentifier} block via the actual, unmodified production classes
 * ({@link DataciteRelatedIdentifierFactory}), and the surrounding DOI payload via the other
 * zero-external-dependency Datacite factories/model classes from raid-api's main source set (title,
 * date, types, identifier). Fields that come from ROR/ORCID/ISNI lookups (publisher, creators,
 * contributors) are out of scope for this regression - those factories require live external HTTP
 * clients wired through the full Spring application context - so they are populated directly with
 * minimal valid values instead of going through {@code DataciteAttributesDtoFactory}'s ROR-backed
 * collaborators. Only a DRAFT DOI is minted (no {@code event}, so it need not resolve), and it is
 * deleted again immediately after assertions, leaving no residue in the DataCite test service.
 *
 * <p>This lives in the plain unit-test source set (not intTest) because it doesn't extend
 * {@code AbstractIntegrationTest}, doesn't use Spring, and only needs raid-api's main classes plus
 * a plain {@link RestTemplate} - the unit-test source set already has {@code src/main} on its
 * classpath, so no build.gradle classpath changes are needed to exercise the real production
 * mapping classes.
 *
 * <p><b>This test is INERT by default</b> so that {@code ./gradlew test} (and CI) stays green
 * without DataCite credentials present. To run it against the real DataCite test API, set:
 * <ul>
 *   <li>{@code DATACITE_LIVE_TEST=true} - required to enable the test at all</li>
 *   <li>{@code DATACITE_TEST_REPOSITORY_ID} - the DataCite test repository/client id (HTTP Basic username)</li>
 *   <li>{@code DATACITE_TEST_PASSWORD} - the repository's password (HTTP Basic password)</li>
 *   <li>{@code DATACITE_TEST_PREFIX} - the DOI prefix the repository may mint under, e.g. {@code 10.82841}</li>
 *   <li>{@code DATACITE_TEST_ENDPOINT} - optional, defaults to {@code https://api.test.datacite.org/dois}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * DATACITE_LIVE_TEST=true \
 * DATACITE_TEST_REPOSITORY_ID=XXXX.YYYY \
 * DATACITE_TEST_PASSWORD=secret \
 * DATACITE_TEST_PREFIX=10.82841 \
 * ./gradlew :api-svc:raid-api:test --tests '*DataciteLive*'
 * }</pre>
 *
 * <p>This test does not require the local dev stack (Docker/Postgres/Keycloak/API) - it talks
 * directly to the real DataCite test API over the network.
 */
public class DataciteLiveRelatedRaidIntegrationTest {

    private static final String DEFAULT_ENDPOINT = "https://api.test.datacite.org/dois";
    private static final String EXPECTED_RELATED_IDENTIFIER_TYPE = "RAiD";
    private static final String EXPECTED_RESOURCE_TYPE_GENERAL = "Project";

    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpEntityFactory httpEntityFactory = new HttpEntityFactory(new HttpHeadersFactory());

    private final DataciteRelatedIdentifierFactory relatedIdentifierFactory = new DataciteRelatedIdentifierFactory();
    private final DataciteTitleFactory titleFactory = new DataciteTitleFactory();
    private final DataciteDateFactory dateFactory = new DataciteDateFactory();
    private final DataciteTypesFactory typesFactory = new DataciteTypesFactory();
    private final DataciteAlternateIdentifierFactory alternateIdentifierFactory = new DataciteAlternateIdentifierFactory();
    private final DataciteIdentifierFactory identifierFactory = new DataciteIdentifierFactory();

    private String repositoryId;
    private String password;
    private String prefix;
    private String endpoint;

    private String mintedDoi;

    @BeforeEach
    void readDataciteTestCredentials() {
        repositoryId = System.getenv("DATACITE_TEST_REPOSITORY_ID");
        password = System.getenv("DATACITE_TEST_PASSWORD");
        prefix = System.getenv("DATACITE_TEST_PREFIX");
        endpoint = System.getenv().getOrDefault("DATACITE_TEST_ENDPOINT", DEFAULT_ENDPOINT);
    }

    @AfterEach
    void deleteDraftDoi() {
        if (mintedDoi == null) {
            return;
        }

        try {
            final HttpEntity<Void> entity = httpEntityFactory.create(null, repositoryId, password);
            restTemplate.exchange(endpoint + "/" + mintedDoi, HttpMethod.DELETE, entity, Void.class);
        } finally {
            mintedDoi = null;
        }
    }

    @Test
    @DisplayName("DataCite test API accepts a relatedRaid mapped to relatedIdentifierType RAiD")
    @EnabledIfEnvironmentVariable(named = "DATACITE_LIVE_TEST", matches = "true")
    void dataciteAcceptsRelatedRaidAsNativeRaidType() {
        // A RAiD's relatedRaid entry, as it would appear on a RaidCreateRequest.
        final var relatedRaidHandle = "https://raid.org.au/10.12345/" + UUID.randomUUID();

        final var relatedRaid = new RelatedRaid()
                .id(relatedRaidHandle)
                .type(new RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_202)
                        .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367));

        // The exact, unmodified production mapping under test - see DataciteAttributesDtoFactory,
        // which calls this same factory method for every entry in request.getRelatedRaid().
        final DataciteRelatedIdentifier relatedIdentifier = relatedIdentifierFactory.create(relatedRaid);

        // Sanity-check our own construction before spending a network round trip on it.
        assertThat(relatedIdentifier.getRelatedIdentifierType()).isEqualTo(EXPECTED_RELATED_IDENTIFIER_TYPE);
        assertThat(relatedIdentifier.getResourceTypeGeneral()).isEqualTo(EXPECTED_RESOURCE_TYPE_GENERAL);
        assertThat(relatedIdentifier.getRelatedIdentifier()).isEqualTo(relatedRaidHandle);

        final var suffix = "raid797-" + UUID.randomUUID();
        final var doi = prefix + "/" + suffix;

        final var title = new Title()
                .startDate(LocalDate.now().toString())
                .text("RAID-797 DataCite live related RAiD regression test")
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5)
                        .schemaUri(TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376));

        final var date = new Date(LocalDate.now().toString());

        final var creator = new DataciteCreator()
                .setName("ARDC")
                .setNameType("Organizational");

        final var relatedIdentifiers = new ArrayList<DataciteRelatedIdentifier>();
        relatedIdentifiers.add(relatedIdentifier);

        // Everything below other than relatedIdentifiers is scaffolding to make a minimal valid
        // DataCite DOI payload - the relatedRaid mapping above is the thing under test.
        final var attributes = new DataciteAttributesDto()
                .setPrefix(prefix)
                .setDoi(doi)
                .setPublicationYear(String.valueOf(java.time.Year.now()))
                .setTypes(typesFactory.create())
                .setTitles(List.of(titleFactory.create(title)))
                .setCreators(List.of(creator))
                .setDates(List.of(dateFactory.create(date)))
                .setContributors(List.of())
                .setDescriptions(List.of())
                .setRelatedIdentifiers(relatedIdentifiers)
                .setAlternateIdentifiers(List.of())
                .setFundingReferences(List.of())
                .setUrl("https://raid.org.au/" + doi);
        // No .setEvent(...) - a draft DOI does not need to resolve and can be deleted afterwards.

        final var dto = new DataciteDto()
                .setSchemaVersion("http://datacite.org/schema/kernel-4")
                .setType("dois")
                .setAttributes(attributes);
        dto.getDataciteIdentifiers().add(identifierFactory.create(doi, "DOI"));

        final var request = new DataciteRequest().setData(dto);

        final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(request, repositoryId, password);

        final var response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Remember it so @AfterEach cleans it up even if an assertion below fails.
        mintedDoi = doi;

        final JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        final JsonNode createdRelatedIdentifiers = body.path("data").path("attributes").path("relatedIdentifiers");
        assertThat(createdRelatedIdentifiers.isArray()).isTrue();

        final var matching = new ArrayList<JsonNode>();
        createdRelatedIdentifiers.forEach(matching::add);

        final var createdRaidRelatedIdentifier = matching.stream()
                .filter(node -> EXPECTED_RELATED_IDENTIFIER_TYPE.equals(node.path("relatedIdentifierType").asText()))
                .findFirst()
                .orElse(null);

        assertThat(createdRaidRelatedIdentifier).as("DataCite should echo back a relatedIdentifier of type RAiD").isNotNull();
        assertThat(createdRaidRelatedIdentifier.path("resourceTypeGeneral").asText()).isEqualTo(EXPECTED_RESOURCE_TYPE_GENERAL);
        assertThat(createdRaidRelatedIdentifier.path("relatedIdentifier").asText()).isEqualTo(relatedRaidHandle);
    }
}
