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

public class ArkRelatedObjectIntegrationTest extends AbstractIntegrationTest {

    private static final String RELATED_OBJECT_CATEGORY_SCHEMA_URI =
            "https://vocabulary.raid.org/relatedObject.category.schemaUri/386";
    private static final String INPUT_RELATED_OBJECT_CATEGORY_ID =
            "https://vocabulary.raid.org/relatedObject.category.id/191";

    private static final String ARK_SCHEMA_URI = "https://arks.org/";

    // A normal (non-99999 NAAN) well-formed arks.org ARK, accepted by the in-memory ArkServiceStub.
    private static final String VALID_ARK = "https://arks.org/ark:/12148/cc9wq2rq";

    /* Sentinel values understood by the in-memory ARK stub, mirrored here because the intTest
       source set cannot import au.org.raid.api.service.stub.InMemoryStubTestData (NONEXISTENT_TEST_ARK
       / SERVER_ERROR_TEST_ARK). NAAN 99999 is a reserved test NAAN (ARK Alliance spec) used only as a
       deterministic stub trigger; the real unregistered-NAAN reject signal is host-based and not
       specific to 99999 (see RAID-793 / ArkService.isUnregistered). */
    private static final String NONEXISTENT_TEST_ARK = "https://arks.org/ark:/99999/not-found";
    private static final String SERVER_ERROR_TEST_ARK = "https://arks.org/ark:/99999/server-error";

    private static final String INVALID_ARK_URL_MESSAGE =
            "has invalid/unsupported value - must be a valid ARK URL (e.g. https://arks.org/ark:/12148/cc9wq2rq)";

    private RelatedObject arkRelatedObject(String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.fromValue(ARK_SCHEMA_URI))
                .type(new RelatedObjectType()
                        .id(RelatedObjectTypeIdEnum.fromValue(BOOK_CHAPTER_RELATED_OBJECT_TYPE))
                        .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(RELATED_OBJECT_TYPE_SCHEMA_URI)))
                .category(List.of(new RelatedObjectCategory()
                        .id(RelatedObjectCategoryIdEnum.fromValue(INPUT_RELATED_OBJECT_CATEGORY_ID))
                        .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(RELATED_OBJECT_CATEGORY_SCHEMA_URI))));
    }

    @Test
    @DisplayName("Minting a RAiD with a valid ARK related object succeeds")
    void validArkRelatedObject() {
        createRequest.setRelatedObject(List.of(arkRelatedObject(VALID_ARK)));

        try {
            final var result = raidApi.mintRaid(createRequest);
            final var raid = result.getBody();
            assertThat(raid).isNotNull();
            assertThat(raid.getRelatedObject()).hasSize(1);
            assertThat(raid.getRelatedObject().get(0).getId()).isEqualTo(VALID_ARK);
            assertThat(raid.getRelatedObject().get(0).getSchemaUri())
                    .isEqualTo(RelatedObjectSchemaUriEnum.fromValue(ARK_SCHEMA_URI));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an ARK related object that the resolver reports as non-existent fails validation")
    void nonExistentArk() {
        createRequest.setRelatedObject(List.of(arkRelatedObject(NONEXISTENT_TEST_ARK)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with non-existent ARK");
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
    @DisplayName("Minting a RAiD with an ARK id that has a too-short NAAN fails format validation")
    void naanTooShortFailsValidation() {
        createRequest.setRelatedObject(List.of(arkRelatedObject("https://arks.org/ark:/1234/x")));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with ARK NAAN shorter than 5 digits");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message(INVALID_ARK_URL_MESSAGE));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a bare ARK id (no arks.org host) fails format validation")
    void bareArkWithNoHostFailsValidation() {
        createRequest.setRelatedObject(List.of(arkRelatedObject("ark:/12148/cc9wq2rq")));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with bare ARK id lacking the arks.org host");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message(INVALID_ARK_URL_MESSAGE));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with an ARK related object fails with 503 when the resolver is unavailable, not a validation error")
    void arkServerError() {
        createRequest.setRelatedObject(List.of(arkRelatedObject(SERVER_ERROR_TEST_ARK)));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown when ARK resolver is unavailable");
        } catch (RetryableException e) {
            assertThat(e.status()).isEqualTo(503);
        } catch (Exception e) {
            failOnError(e);
        }
    }
}
