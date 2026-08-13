package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectCategorySchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import feign.RetryableException;
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

    /* confirmed sentinel values understood by the in-memory Handle stub, see
       au.org.raid.api.service.stub.InMemoryStubTestData */
    private static final String NONEXISTENT_TEST_HANDLE = "https://hdl.handle.net/0.0/not-found";
    private static final String SERVER_ERROR_TEST_HANDLE = "https://hdl.handle.net/0.0/server-error";

    /* confirmed sentinel values understood by the in-memory RRID stub, see
       au.org.raid.api.service.stub.InMemoryStubTestData */
    private static final String NONEXISTENT_TEST_RRID = "https://scicrunch.org/resolver/RRID:AB_0000000";
    private static final String SERVER_ERROR_TEST_RRID = "https://scicrunch.org/resolver/RRID:AB_5000000";

    private RelatedObject webArchiveRelatedObject(String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.fromValue(WEB_ARCHIVE_SCHEMA_URI))
                .type(new RelatedObjectType()
                        .id(RelatedObjectTypeIdEnum.fromValue(BOOK_CHAPTER_RELATED_OBJECT_TYPE))
                        .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(RELATED_OBJECT_TYPE_SCHEMA_URI)))
                .category(List.of(new RelatedObjectCategory()
                        .id(RelatedObjectCategoryIdEnum.fromValue(INPUT_RELATED_OBJECT_CATEGORY_ID))
                        .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(RELATED_OBJECT_CATEGORY_SCHEMA_URI))));
    }

    private RelatedObject handleRelatedObject(String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.fromValue(HANDLE_SCHEMA_URI))
                .type(new RelatedObjectType()
                        .id(RelatedObjectTypeIdEnum.fromValue(BOOK_CHAPTER_RELATED_OBJECT_TYPE))
                        .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(RELATED_OBJECT_TYPE_SCHEMA_URI)))
                .category(List.of(new RelatedObjectCategory()
                        .id(RelatedObjectCategoryIdEnum.fromValue(INPUT_RELATED_OBJECT_CATEGORY_ID))
                        .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(RELATED_OBJECT_CATEGORY_SCHEMA_URI))));
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
            assertThat(raid.getRelatedObject().get(0).getSchemaUri()).isEqualTo(RelatedObjectSchemaUriEnum.fromValue(WEB_ARCHIVE_SCHEMA_URI));
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
    @DisplayName("Minting a RAiD with web archive schemaUri but invalid id fails validation")
    void webArchiveSchemaUriWithInvalidId() {
        final var relatedObject = new RelatedObject()
                .id("https://example.com/some-object")
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_WEB_ARCHIVE_ORG_)
                .type(new RelatedObjectType()
                        .id(RelatedObjectTypeIdEnum.fromValue(BOOK_CHAPTER_RELATED_OBJECT_TYPE))
                        .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(RELATED_OBJECT_TYPE_SCHEMA_URI)))
                .category(List.of(new RelatedObjectCategory()
                        .id(RelatedObjectCategoryIdEnum.fromValue(INPUT_RELATED_OBJECT_CATEGORY_ID))
                        .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(RELATED_OBJECT_CATEGORY_SCHEMA_URI))));

        createRequest.setRelatedObject(List.of(relatedObject));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with invalid web archive id");
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

    @Test
    @DisplayName("Minting a RAiD with a web archive related object that the resolver reports as non-existent fails validation")
    void nonExistentWebArchiveSnapshot() {
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(NONEXISTENT_TEST_WEB_ARCHIVE)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with non-existent Web Archive snapshot");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("uri not found"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a web archive related object fails with 503 when the resolver is unavailable, not a validation error")
    void webArchiveServerError() {
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(SERVER_ERROR_TEST_WEB_ARCHIVE)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown when Web Archive resolver is unavailable");
        } catch (RetryableException e) {
            assertThat(e.status()).isEqualTo(503);
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an implausible web archive timestamp year fails validation without calling the resolver")
    void webArchiveImplausibleYear() {
        createRequest.setRelatedObject(List.of(
                webArchiveRelatedObject("https://web.archive.org/web/14062026010101/https://example.com")));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with implausible Web Archive timestamp year");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("web archive timestamp year 1406 is implausible"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    private RelatedObject rridRelatedObject(String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.fromValue(RRID_SCHEMA_URI))
                .type(new RelatedObjectType()
                        .id(RelatedObjectTypeIdEnum.fromValue(BOOK_CHAPTER_RELATED_OBJECT_TYPE))
                        .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(RELATED_OBJECT_TYPE_SCHEMA_URI)))
                .category(List.of(new RelatedObjectCategory()
                        .id(RelatedObjectCategoryIdEnum.fromValue(INPUT_RELATED_OBJECT_CATEGORY_ID))
                        .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(RELATED_OBJECT_CATEGORY_SCHEMA_URI))));
    }

    @Test
    @DisplayName("Minting a RAiD with a valid Handle related object succeeds")
    void validHandleRelatedObject() {
        createRequest.setRelatedObject(List.of(handleRelatedObject(VALID_HANDLE)));

        try {
            final var result = raidApi.mintRaid(createRequest);
            final var raid = result.getBody();
            assertThat(raid).isNotNull();
            assertThat(raid.getRelatedObject()).hasSize(1);
            assertThat(raid.getRelatedObject().get(0).getId()).isEqualTo(VALID_HANDLE);
            assertThat(raid.getRelatedObject().get(0).getSchemaUri()).isEqualTo(RelatedObjectSchemaUriEnum.fromValue(HANDLE_SCHEMA_URI));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a Handle related object that the resolver reports as non-existent fails validation")
    void nonExistentHandle() {
        createRequest.setRelatedObject(List.of(handleRelatedObject(NONEXISTENT_TEST_HANDLE)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with non-existent Handle");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("uri not found"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a Handle related object fails validation when the resolver reports a server error")
    void handleServerError() {
        createRequest.setRelatedObject(List.of(handleRelatedObject(SERVER_ERROR_TEST_HANDLE)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown when Handle resolver returns a server error");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("uri could not be validated - server error"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("A DOI-shaped id under Handle schemaUri is validated via the Handle resolver, not the DOI resolver")
    void doiShapedIdUnderHandleSchemaUriUsesHandleResolver() {
        // Regex-valid Handle URL whose suffix happens to look like a DOI. At the unit level,
        // DataciteRelatedIdentifierFactoryTest / RelatedObjectValidatorTest prove the dispatch map
        // routes this to HandleService and never calls DoiService. Here we confirm end-to-end that
        // the mint succeeds via the Handle stub path (a DOI-path routing bug would either call the
        // (unstubbed) DOI resolver, or reject the id outright).
        createRequest.setRelatedObject(List.of(handleRelatedObject("https://hdl.handle.net/10.1234/xyz")));

        try {
            final var result = raidApi.mintRaid(createRequest);
            final var raid = result.getBody();
            assertThat(raid).isNotNull();
            assertThat(raid.getRelatedObject()).hasSize(1);
            assertThat(raid.getRelatedObject().get(0).getId()).isEqualTo("https://hdl.handle.net/10.1234/xyz");
        } catch (Exception e) {
            failOnError(e);
        }
    }

    // Scenario 4 (Handle represented correctly in the outbound DataCite request, i.e.
    // relatedIdentifierType = "Handle") is not covered here. RelatedObjectIntegrationTest and the
    // other intTest classes in this package have no harness for asserting on the outbound DataCite
    // payload (DataciteErrorIntegrationTest only stubs DataCite error responses, it doesn't let us
    // inspect the request body). Adding one would mean either standing up a MockServer verify()
    // expectation against the DataCite POST body or duplicating factory-level logic in the test
    // itself - both are disproportionate to what's already asserted by the unit test
    // DataciteRelatedIdentifierFactoryTest, which directly asserts
    // relatedIdentifierType = RelatedIdentifierType.HANDLE.getName() for a Handle-scoped
    // RelatedObject. Rather than fabricate a brittle intTest, Scenario 4 is left to that unit test.

    @Test
    @DisplayName("Minting a RAiD with a valid RRID related object succeeds")
    void validRridRelatedObject() {
        createRequest.setRelatedObject(List.of(rridRelatedObject(VALID_RRID)));

        try {
            final var result = raidApi.mintRaid(createRequest);
            final var raid = result.getBody();
            assertThat(raid).isNotNull();
            assertThat(raid.getRelatedObject()).hasSize(1);
            assertThat(raid.getRelatedObject().get(0).getId()).isEqualTo(VALID_RRID);
            assertThat(raid.getRelatedObject().get(0).getSchemaUri()).isEqualTo(RelatedObjectSchemaUriEnum.fromValue(RRID_SCHEMA_URI));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an RRID related object that the resolver reports as non-existent fails validation")
    void nonExistentRrid() {
        createRequest.setRelatedObject(List.of(rridRelatedObject(NONEXISTENT_TEST_RRID)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with non-existent RRID");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("uri not found"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an RRID related object fails validation when the resolver reports a server error")
    void rridServerError() {
        createRequest.setRelatedObject(List.of(rridRelatedObject(SERVER_ERROR_TEST_RRID)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown when RRID resolver returns a server error");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("uri could not be validated - server error"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    // As with Handle (see Scenario 4 note above), the RRID DataCite mapping (relatedIdentifierType =
    // "RRID") is not asserted at the intTest level for the same reasons - this package has no harness
    // for inspecting the outbound DataCite request body. It is covered by the unit test
    // DataciteRelatedIdentifierFactoryTest, which asserts relatedIdentifierType is "RRID" for a
    // scicrunch-scoped RelatedObject.
}
