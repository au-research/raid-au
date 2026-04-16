package au.org.raid.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class VocabularyUriReplacementServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VocabularyUriReplacementService service = new VocabularyUriReplacementService(objectMapper);

    @Nested
    @DisplayName("upgradeVocabularyUris")
    class UpgradeVocabularyUris {

        @Test
        @DisplayName("Returns null for null input")
        void returnsNullForNull() {
            assertThat(service.upgradeVocabularyUris(null), is(nullValue()));
        }

        @Test
        @DisplayName("Returns unchanged JSON when no github.com URIs present")
        void returnsUnchangedWhenNoLegacyUris() {
            final var json = "{\"access\":{\"type\":{\"id\":\"https://vocabularies.coar-repositories.org/access_rights/c_abf2/\"}}}";
            assertThat(service.upgradeVocabularyUris(json), is(json));
        }

        @Test
        @DisplayName("Replaces legacy access type URI (open)")
        void replacesAccessTypeOpen() {
            final var json = "{\"access\":{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json\"}}}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabularies.coar-repositories.org/access_rights/c_abf2/"));
            assertThat(result, not(containsString("github.com")));
        }

        @Test
        @DisplayName("Replaces legacy access type URI (closed → embargoed)")
        void replacesAccessTypeClosed() {
            final var json = "{\"access\":{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json\"}}}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabularies.coar-repositories.org/access_rights/c_f1cf/"));
        }

        @Test
        @DisplayName("Replaces legacy access type schema URI")
        void replacesAccessTypeSchemaUri() {
            final var json = "{\"access\":{\"type\":{\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/\"}}}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabularies.coar-repositories.org/access_rights/"));
        }

        @Test
        @DisplayName("Replaces legacy title type URIs")
        void replacesTitleTypeUris() {
            final var json = "{\"title\":[{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1/\"}}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/title.type.schema/5"));
            assertThat(result, containsString("https://vocabulary.raid.org/title.type.schema/376"));
        }

        @Test
        @DisplayName("Replaces legacy description type URIs")
        void replacesDescriptionTypeUris() {
            final var json = "{\"description\":[{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json\"}}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/description.type.schema/318"));
        }

        @Test
        @DisplayName("Replaces legacy organisation role URIs")
        void replacesOrganisationRoleUris() {
            final var json = "{\"organisation\":[{\"role\":[{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json\"}]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/organisation.role.schema/182"));
        }

        @Test
        @DisplayName("Replaces legacy related object type URIs")
        void replacesRelatedObjectTypeUris() {
            final var json = "{\"relatedObject\":[{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dataset.json\"}}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/relatedObject.type.schema/269"));
        }

        @Test
        @DisplayName("Replaces legacy related object category URIs")
        void replacesRelatedObjectCategoryUris() {
            final var json = "{\"relatedObject\":[{\"category\":[{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json\"}]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/relatedObject.category.id/190"));
        }

        @Test
        @DisplayName("Replaces legacy related raid type URIs")
        void replacesRelatedRaidTypeUris() {
            final var json = "{\"relatedRaid\":[{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-part-of.json\"}}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/relatedRaid.type.schema/202"));
        }

        @Test
        @DisplayName("Replaces multiple legacy URIs in same JSON")
        void replacesMultipleLegacyUris() {
            final var json = "{\"access\":{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json\"}}," +
                    "\"title\":[{\"type\":{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json\"}}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabularies.coar-repositories.org/access_rights/c_abf2/"));
            assertThat(result, containsString("https://vocabulary.raid.org/title.type.schema/5"));
            assertThat(result, not(containsString("github.com")));
        }
    }

    @Nested
    @DisplayName("contributor position upgrade")
    class ContributorPositionUpgrade {

        @Test
        @DisplayName("Replaces non-leader/contact contributor positions via URI replacement")
        void replacesRegularContributorPositions() {
            final var json = "{\"contributor\":[{\"position\":[{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\"}]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("https://vocabulary.raid.org/contributor.position.schema/307"));
            assertThat(result, containsString("https://vocabulary.raid.org/contributor.position.schema/305"));
        }

        @Test
        @DisplayName("Converts leader position to boolean flag with default Other position")
        void convertsLeaderPositionToBooleanFlag() {
            final var json = "{\"contributor\":[{\"position\":[{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\",\"startDate\":\"2023-01-01\",\"endDate\":\"2023-12-31\"}]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("\"leader\":true"));
            assertThat(result, containsString("https://vocabulary.raid.org/contributor.position.schema/311"));
            assertThat(result, not(containsString("leader.json")));
        }

        @Test
        @DisplayName("Converts contact position to boolean flag with default Other position")
        void convertsContactPositionToBooleanFlag() {
            final var json = "{\"contributor\":[{\"position\":[{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\",\"startDate\":\"2023-01-01\",\"endDate\":\"2023-12-31\"}]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("\"contact\":true"));
            assertThat(result, containsString("https://vocabulary.raid.org/contributor.position.schema/311"));
            assertThat(result, not(containsString("contact.json")));
        }

        @Test
        @DisplayName("Keeps existing positions when leader/contact removed")
        void keepsExistingPositionsWhenLeaderRemoved() {
            final var json = "{\"contributor\":[{\"position\":[" +
                    "{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\",\"startDate\":\"2023-01-01\",\"endDate\":\"\"}," +
                    "{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\",\"startDate\":\"2023-01-01\",\"endDate\":\"\"}" +
                    "]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("\"leader\":true"));
            // PI position should remain
            assertThat(result, containsString("https://vocabulary.raid.org/contributor.position.schema/307"));
            // Should NOT add default Other position since PI position remains
            assertThat(result, not(containsString("contributor.position.schema/311")));
        }

        @Test
        @DisplayName("Handles both leader and contact on same contributor")
        void handlesBothLeaderAndContact() {
            final var json = "{\"contributor\":[{\"position\":[" +
                    "{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\",\"startDate\":\"2023-01-01\",\"endDate\":\"\"}," +
                    "{\"id\":\"https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact.json\",\"schemaUri\":\"https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/\",\"startDate\":\"2023-01-01\",\"endDate\":\"\"}" +
                    "]}]}";
            final var result = service.upgradeVocabularyUris(json);
            assertThat(result, containsString("\"leader\":true"));
            assertThat(result, containsString("\"contact\":true"));
            assertThat(result, containsString("https://vocabulary.raid.org/contributor.position.schema/311"));
        }
    }
}
