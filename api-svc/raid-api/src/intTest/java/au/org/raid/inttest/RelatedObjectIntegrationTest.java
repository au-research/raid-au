package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class RelatedObjectIntegrationTest extends AbstractIntegrationTest {

    private static final String RELATED_OBJECT_CATEGORY_SCHEMA_URI =
            "https://vocabulary.raid.org/relatedObject.category.schemaUri/386";
    private static final String INPUT_RELATED_OBJECT_CATEGORY_ID =
            "https://vocabulary.raid.org/relatedObject.category.id/191";

    private RelatedObject webArchiveRelatedObject(String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(WEB_ARCHIVE_SCHEMA_URI)
                .type(new RelatedObjectType()
                        .id(BOOK_CHAPTER_RELATED_OBJECT_TYPE)
                        .schemaUri(RELATED_OBJECT_TYPE_SCHEMA_URI))
                .category(List.of(new RelatedObjectCategory()
                        .id(INPUT_RELATED_OBJECT_CATEGORY_ID)
                        .schemaUri(RELATED_OBJECT_CATEGORY_SCHEMA_URI)));
    }

    @Test
    @DisplayName("Minting a RAiD with a valid web archive related object succeeds")
    void validWebArchiveRelatedObject() {
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(VALID_WEB_ARCHIVE_URL)));

        try {
            final var result = raidApi.mintRaid(createRequest);
            final var raid = result.getBody();
            assertThat(raid).isNotNull();
            assertThat(raid.getRelatedObject()).hasSize(1);
            assertThat(raid.getRelatedObject().get(0).getId()).isEqualTo(VALID_WEB_ARCHIVE_URL);
            assertThat(raid.getRelatedObject().get(0).getSchemaUri()).isEqualTo(WEB_ARCHIVE_SCHEMA_URI);
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an invalid web archive URL fails validation")
    void invalidWebArchiveUrl() {
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(INVALID_WEB_ARCHIVE_URL)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with invalid web archive URL");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalid")
                    .message("Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an unsupported related object schemaUri fails validation")
    void unsupportedSchemaUri() {
        final var relatedObject = new RelatedObject()
                .id("https://example.com/some-object")
                .schemaUri("https://example.com/")
                .type(new RelatedObjectType()
                        .id(BOOK_CHAPTER_RELATED_OBJECT_TYPE)
                        .schemaUri(RELATED_OBJECT_TYPE_SCHEMA_URI))
                .category(List.of(new RelatedObjectCategory()
                        .id(INPUT_RELATED_OBJECT_CATEGORY_ID)
                        .schemaUri(RELATED_OBJECT_CATEGORY_SCHEMA_URI)));

        createRequest.setRelatedObject(List.of(relatedObject));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with unsupported schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].schemaUri")
                    .errorType("invalid")
                    .message("Only [https://doi.org/, https://web.archive.org/] is supported."));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a web archive URL missing the inner URL fails validation")
    void webArchiveUrlMissingInnerUrl() {
        createRequest.setRelatedObject(List.of(
                webArchiveRelatedObject("https://web.archive.org/web/20220101000000/https://")));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with web archive URL missing inner URL");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalid")
                    .message("Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)"));
        } catch (Exception e) {
            failOnError(e);
        }
    }
}
