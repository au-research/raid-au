package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectCategorySchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.Handle;
import au.org.raid.inttest.service.RaidApiValidationException;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

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

    /* "validate once" sentinel understood by the in-memory Web Archive stub, see
       au.org.raid.api.service.stub.InMemoryStubTestData (RAID-935): a url under this prefix passes
       the first time the stub sees it and returns 503 on every later call. */
    private static final String VALIDATE_ONCE_WEB_ARCHIVE_PREFIX =
            "https://web.archive.org/web/20200101000000/https://validate-once.example.com/";

    private static String freshValidateOnceLink() {
        return VALIDATE_ONCE_WEB_ARCHIVE_PREFIX + UUID.randomUUID();
    }

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

    private RelatedObject doiRelatedObject(String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.fromValue(DOI_SCHEMA_URI))
                .type(new RelatedObjectType()
                        .id(RelatedObjectTypeIdEnum.fromValue(BOOK_CHAPTER_RELATED_OBJECT_TYPE))
                        .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(RELATED_OBJECT_TYPE_SCHEMA_URI)))
                .category(List.of(new RelatedObjectCategory()
                        .id(RelatedObjectCategoryIdEnum.fromValue(INPUT_RELATED_OBJECT_CATEGORY_ID))
                        .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(RELATED_OBJECT_CATEGORY_SCHEMA_URI))));
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

    @Test
    @DisplayName("Minting a RAiD with a dx.doi.org DOI related object succeeds and stores the id unchanged (RAID-798)")
    void validDxDoiRelatedObject() {
        createRequest.setRelatedObject(List.of(doiRelatedObject(VALID_DX_DOI)));

        try {
            final var result = raidApi.mintRaid(createRequest);
            final var raid = result.getBody();
            assertThat(raid).isNotNull();
            assertThat(raid.getRelatedObject()).hasSize(1);
            // The submitted dx.doi.org form is stored verbatim, not normalised to doi.org.
            assertThat(raid.getRelatedObject().get(0).getId()).isEqualTo(VALID_DX_DOI);
            assertThat(raid.getRelatedObject().get(0).getSchemaUri())
                    .isEqualTo(RelatedObjectSchemaUriEnum.fromValue(DOI_SCHEMA_URI));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a dx.doi.org id that fails DOI format is rejected (RAID-798 regression guard)")
    void malformedDxDoiIsRejected() {
        createRequest.setRelatedObject(List.of(doiRelatedObject("https://dx.doi.org/not-a-doi")));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with malformed dx.doi.org DOI");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).getFieldId()).isEqualTo("relatedObject[0].id");
            assertThat(failures.get(0).getErrorType()).isEqualTo("invalidValue");
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

    // RAID-935: an unchanged link is verified on entry, not on every save. These tests rely on the
    // "validate once" stub sentinel: a second resolver call for the same link would be a 503.
    //
    // These tests must pass with the success cache ON (branch envs, which enable the stubs by
    // env var and use the 30m default) and OFF (the dev profile used by GitHub CI and local runs:
    // raid.uri-validation.success-cache.expire-after-write: 0s in application-dev.yaml). With the
    // cache off, a PUT that passes proves the stored-version skip end to end. In a branch env a
    // cached success can also explain the pass, so only the dev profile proves the stored skip.
    // The sentinel itself is proven in WebArchiveServiceStubTest, and the cache in
    // RelatedObjectValidatorTest, so there is deliberately no "second mint gets a 503" test here:
    // it would get a cache hit and fail when the cache is on.

    @Test
    @DisplayName("Updating an unrelated field keeps an already accepted web archive link without re-checking it")
    void updateUnrelatedFieldDoesNotRecheckUnchangedLink() {
        final var link = freshValidateOnceLink();
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(link)));

        final var minted = raidApi.mintRaid(createRequest).getBody();
        assertThat(minted).isNotNull();
        final var handle = new Handle(minted.getIdentifier().getId());

        try {
            // version 1 -> 2: the link is in the stored version, the stub would now answer 503
            final var firstUpdate = titleUpdate(handle, " first");
            assertThat(firstUpdate.getIdentifier().getVersion()).isEqualTo(2);
            assertThat(firstUpdate.getRelatedObject().get(0).getId()).isEqualTo(link);

            // version 2 -> 3: the link is in version 2 too
            final var secondUpdate = titleUpdate(handle, " second");
            assertThat(secondUpdate.getIdentifier().getVersion()).isEqualTo(3);
            assertThat(secondUpdate.getRelatedObject().get(0).getId()).isEqualTo(link);
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Adding a new link on update checks only the new link; the existing link is not re-checked")
    void updateAddingNewLinkChecksOnlyTheNewLink() {
        final var existing = freshValidateOnceLink();
        final var added = freshValidateOnceLink();
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(existing)));

        final var minted = raidApi.mintRaid(createRequest).getBody();
        assertThat(minted).isNotNull();
        final var handle = new Handle(minted.getIdentifier().getId());

        try {
            final var read = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
            assertThat(read).isNotNull();
            final var update = mapReadToUpdate(read);
            // the new link goes first, so the unchanged one moves index; order does not matter
            update.setRelatedObject(List.of(webArchiveRelatedObject(added), webArchiveRelatedObject(existing)));

            final var result = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), update).getBody();

            assertThat(result).isNotNull();
            assertThat(result.getRelatedObject()).extracting(RelatedObject::getId).containsExactly(added, existing);
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Changing a link to a web archive snapshot that does not exist is still rejected on update")
    void updateChangingLinkToNonExistentStillFails() {
        final var link = freshValidateOnceLink();
        createRequest.setRelatedObject(List.of(webArchiveRelatedObject(link)));

        final var minted = raidApi.mintRaid(createRequest).getBody();
        assertThat(minted).isNotNull();
        final var handle = new Handle(minted.getIdentifier().getId());
        final var read = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assertThat(read).isNotNull();
        final var update = mapReadToUpdate(read);
        update.setRelatedObject(List.of(webArchiveRelatedObject(NONEXISTENT_TEST_WEB_ARCHIVE)));

        try {
            raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), update);
            fail("No exception thrown when changing a link to a non-existent Web Archive snapshot");
        } catch (RaidApiValidationException e) {
            assertThat(e.getFailures()).containsExactly(new ValidationFailure()
                    .fieldId("relatedObject[0].id")
                    .errorType("invalidValue")
                    .message("uri not found"));
        } catch (Exception e) {
            failOnError(e);
        }
    }

    private RaidDto titleUpdate(final Handle handle, final String titleSuffix) {
        final var read = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assertThat(read).isNotNull();
        final var update = mapReadToUpdate(read);
        update.getTitle().get(0).setText(update.getTitle().get(0).getText() + titleSuffix);

        final var result = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), update).getBody();
        assertThat(result).isNotNull();
        return result;
    }

    private RaidUpdateRequest mapReadToUpdate(final RaidDto read) {
        return new RaidUpdateRequest()
                .metadata(read.getMetadata())
                .identifier(read.getIdentifier())
                .title(read.getTitle())
                .date(read.getDate())
                .description(read.getDescription())
                .access(read.getAccess())
                .alternateUrl(read.getAlternateUrl())
                .contributor(read.getContributor())
                .organisation(read.getOrganisation())
                .subject(read.getSubject())
                .relatedRaid(read.getRelatedRaid())
                .relatedObject(read.getRelatedObject())
                .alternateIdentifier(read.getAlternateIdentifier())
                .spatialCoverage(read.getSpatialCoverage());
    }
}
