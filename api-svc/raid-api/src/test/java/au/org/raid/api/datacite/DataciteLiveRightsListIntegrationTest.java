package au.org.raid.api.datacite;

import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.HttpHeadersFactory;
import au.org.raid.api.factory.datacite.DataciteDateFactory;
import au.org.raid.api.factory.datacite.DataciteIdentifierFactory;
import au.org.raid.api.factory.datacite.DataciteRightFactory;
import au.org.raid.api.factory.datacite.DataciteTitleFactory;
import au.org.raid.api.factory.datacite.DataciteTypesFactory;
import au.org.raid.api.model.datacite.doi.DataciteAttributesDto;
import au.org.raid.api.model.datacite.doi.DataciteCreator;
import au.org.raid.api.model.datacite.doi.DataciteDto;
import au.org.raid.api.model.datacite.doi.DataciteRequest;
import au.org.raid.idl.raidv2.model.Date;
import au.org.raid.idl.raidv2.model.Id;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live regression test for RAID-834: proves the real DataCite test API accepts the {@code rightsList}
 * that {@link DataciteRightFactory} builds from RAiD's configured licence ("Creative Commons CC-0"),
 * and echoes back the canonical SPDX CC0 entry.
 *
 * <p>Follows the same pattern as {@link DataciteLiveRelatedRaidIntegrationTest}: the rights entry
 * comes from the unmodified production factory, the rest of the payload is minimal scaffolding, only
 * a DRAFT DOI is minted, and it is deleted again afterwards.
 *
 * <p><b>This test is INERT by default.</b> Enable it with the same environment variables as
 * {@link DataciteLiveRelatedRaidIntegrationTest} ({@code DATACITE_LIVE_TEST=true},
 * {@code DATACITE_TEST_REPOSITORY_ID}, {@code DATACITE_TEST_PASSWORD}, {@code DATACITE_TEST_PREFIX},
 * optional {@code DATACITE_TEST_ENDPOINT}).
 */
public class DataciteLiveRightsListIntegrationTest {

    private static final String DEFAULT_ENDPOINT = "https://api.test.datacite.org/dois";
    private static final String RAID_LICENSE = "Creative Commons CC-0";

    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpEntityFactory httpEntityFactory = new HttpEntityFactory(new HttpHeadersFactory());

    private final DataciteRightFactory rightFactory = new DataciteRightFactory();
    private final DataciteTitleFactory titleFactory = new DataciteTitleFactory();
    private final DataciteDateFactory dateFactory = new DataciteDateFactory();
    private final DataciteTypesFactory typesFactory = new DataciteTypesFactory();
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
    @DisplayName("DataCite test API accepts the CC0 rightsList built from the RAiD licence")
    @EnabledIfEnvironmentVariable(named = "DATACITE_LIVE_TEST", matches = "true")
    void dataciteAcceptsCc0RightsList() {
        // The exact, unmodified production mapping under test.
        final var rightsList = rightFactory.create(new Id().license(RAID_LICENSE));

        assertThat(rightsList).hasSize(1);

        final var doi = prefix + "/raid834-" + UUID.randomUUID();

        final var title = new Title()
                .startDate(LocalDate.now().toString())
                .text("RAID-834 DataCite live rightsList regression test")
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5)
                        .schemaUri(TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376));

        final var attributes = new DataciteAttributesDto()
                .setPrefix(prefix)
                .setDoi(doi)
                .setPublicationYear(String.valueOf(java.time.Year.now()))
                .setTypes(typesFactory.create())
                .setTitles(List.of(titleFactory.create(title)))
                .setCreators(List.of(new DataciteCreator().setName("ARDC").setNameType("Organizational")))
                .setDates(List.of(dateFactory.create(new Date(LocalDate.now().toString()))))
                .setContributors(List.of())
                .setDescriptions(List.of())
                .setRelatedIdentifiers(List.of())
                .setAlternateIdentifiers(List.of())
                .setFundingReferences(List.of())
                .setRightsList(rightsList)
                .setUrl("https://raid.org.au/" + doi);
        // No .setEvent(...) - a draft DOI does not need to resolve and can be deleted afterwards.

        final var dto = new DataciteDto()
                .setSchemaVersion("http://datacite.org/schema/kernel-4")
                .setType("dois")
                .setAttributes(attributes);
        dto.getDataciteIdentifiers().add(identifierFactory.create(doi, "DOI"));

        final HttpEntity<DataciteRequest> entity =
                httpEntityFactory.create(new DataciteRequest().setData(dto), repositoryId, password);

        final var response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Remember it so @AfterEach cleans it up even if an assertion below fails.
        mintedDoi = doi;

        final JsonNode createdRights = response.getBody().path("data").path("attributes").path("rightsList");
        assertThat(createdRights.isArray()).isTrue();
        assertThat(createdRights.size()).isEqualTo(1);

        final JsonNode right = createdRights.get(0);
        assertThat(right.path("rights").asText()).isEqualTo("Creative Commons Zero v1.0 Universal");
        assertThat(right.path("rightsUri").asText()).isEqualTo("https://creativecommons.org/publicdomain/zero/1.0/legalcode");
        assertThat(right.path("rightsIdentifier").asText()).isEqualToIgnoringCase("cc0-1.0");
        assertThat(right.path("rightsIdentifierScheme").asText()).isEqualTo("SPDX");
        assertThat(right.path("schemeUri").asText()).isEqualTo("https://spdx.org/licenses/");
    }
}
