package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.service.doi.DoiService;
import au.org.raid.api.service.handle.HandleService;
import au.org.raid.api.service.rrid.RridService;
import au.org.raid.api.service.webarchive.WebArchiveService;
import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectCategorySchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedObjectValidatorTest {
    @Mock
    private RelatedObjectTypeValidator typeValidationService;

    @Mock
    private RelatedObjectCategoryValidator categoryValidationService;

    @Mock
    private DoiService doiService;

    @Mock
    private HandleService handleService;

    @Mock
    private RridService rridService;

    @Mock
    private WebArchiveService webArchiveService;

    private RelatedObjectSuccessCache successCache;

    private RelatedObjectValidator validationService;

    @BeforeEach
    void setUp() {
        successCache = new RelatedObjectSuccessCache(Duration.ofMinutes(5), 100);
        validationService = new RelatedObjectValidator(
                null, doiService, handleService, rridService, webArchiveService,
                typeValidationService, categoryValidationService, successCache);
    }

    @Test
    @DisplayName("Validation passes with valid related object")
    void validaRelatedObject() {
        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(TestConstants.VALID_DOI)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(type)
                .category(categories);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Passes validation with empty related objects")
    void emptyRelatedObjects() {
        final var failures = validationService.validateRelatedObjects(Collections.emptyList()).failures();

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Passes validation with null related objects")
    void nullRelatedObjects() {
        final var failures = validationService.validateRelatedObjects(null).failures();

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Fails validation with null related object id")
    void nullId() {
        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(type)
                .category(categories);

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[0].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Fails validation with empty related object id")
    void emptyId() {
        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id("")
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(type)
                .category(categories);

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[0].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Fails validation with null schemaUri")
    void nullSchemeUri() {
        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(TestConstants.VALID_DOI)
                .type(type)
                .category(categories);

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails if DOI does not exist")
    void addsFailureIfDoiDoesNotExist() {
        final var fieldId = "relatedObject[0].id";
        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(TestConstants.VALID_DOI)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(type)
                .category(categories);

        final var failure = new ValidationFailure()
                .fieldId(fieldId)
                .errorType("invalidValue")
                .message("uri not found");

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(doiService.validate(TestConstants.VALID_DOI, fieldId)).thenReturn(List.of(failure));

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, is(List.of(failure)));
    }

    @Test
    @DisplayName("Validation passes with valid Handle related object")
    void validHandleRelatedObject() {
        final var handleUri = "https://hdl.handle.net/20.500.12345/abc123";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(handleUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_HDL_HANDLE_NET_)
                .type(type)
                .category(categories);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(handleService.validate(handleUri, "relatedObject[0].id")).thenReturn(Collections.emptyList());

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails if Handle does not resolve")
    void addsFailureIfHandleDoesNotExist() {
        final var handleUri = "https://hdl.handle.net/20.500.12345/not-found";
        final var fieldId = "relatedObject[0].id";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(handleUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_HDL_HANDLE_NET_)
                .type(type)
                .category(categories);

        final var failure = new ValidationFailure()
                .fieldId(fieldId)
                .errorType("invalidValue")
                .message("uri not found");

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(handleService.validate(handleUri, fieldId)).thenReturn(List.of(failure));

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, is(List.of(failure)));
    }

    @Test
    @DisplayName("A DOI-shaped id under the Handle schemaUri is dispatched to HandleService, not DoiService")
    void doiShapedIdUnderHandleSchemaUriUsesHandleService() {
        final var doiShapedHandleUri = "https://hdl.handle.net/10.1234/xyz";
        final var fieldId = "relatedObject[0].id";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(doiShapedHandleUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_HDL_HANDLE_NET_)
                .type(type)
                .category(categories);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(handleService.validate(doiShapedHandleUri, fieldId)).thenReturn(Collections.emptyList());

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, empty());
        verify(handleService).validate(doiShapedHandleUri, fieldId);
        verify(doiService, never()).validate(any(), any());
    }

    @Test
    @DisplayName("Validation passes with valid RRID related object")
    void validRridRelatedObject() {
        final var rridUri = "https://scicrunch.org/resolver/RRID:AB_2298772";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(rridUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_SCICRUNCH_ORG_RESOLVER_)
                .type(type)
                .category(categories);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(rridService.validate(rridUri, "relatedObject[0].id")).thenReturn(Collections.emptyList());

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("An RRID-shaped id under the SciCrunch schemaUri is dispatched to RridService, not DoiService or HandleService")
    void rridShapedIdUnderScicrunchSchemaUriUsesRridService() {
        final var rridUri = "https://scicrunch.org/resolver/RRID:AB_2298772";
        final var fieldId = "relatedObject[0].id";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(rridUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_SCICRUNCH_ORG_RESOLVER_)
                .type(type)
                .category(categories);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(rridService.validate(rridUri, fieldId)).thenReturn(Collections.emptyList());

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, empty());
        verify(rridService).validate(rridUri, fieldId);
        verify(doiService, never()).validate(any(), any());
        verify(handleService, never()).validate(any(), any());
    }

    @Test
    @DisplayName("Validation failures in type and category are returned")
    void typeAndCategoryFailuresAreReturned() {
        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(TestConstants.VALID_DOI)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(type)
                .category(categories);

        final var typeError = new ValidationFailure()
                .fieldId("relatedObject[0].type.id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        final var categoryError = new ValidationFailure()
                .fieldId("relatedObject[0].category.id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        when(typeValidationService.validate(type, 0)).thenReturn(List.of(typeError));
        when(categoryValidationService.validate(categories, 0)).thenReturn(List.of(categoryError));

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, hasSize(2));
        assertThat(failures, hasItems(typeError, categoryError));
    }

    @Test
    @DisplayName("A resolver failure for one related object does not abort validation of the rest of the request")
    void resolverUnavailableForOneRelatedObjectDoesNotAbortValidationOfOthers() {
        final var doiFieldId = "relatedObject[0].id";
        final var handleUri = "https://hdl.handle.net/20.500.12345/abc123";
        final var handleFieldId = "relatedObject[1].id";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var doiRelatedObject = new RelatedObject()
                .id(TestConstants.VALID_DOI)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(type)
                .category(categories);

        final var handleRelatedObject = new RelatedObject()
                .id(handleUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_HDL_HANDLE_NET_)
                .type(type)
                .category(categories);

        final var unavailable = new UnavailableResolver()
                .field(doiFieldId)
                .value(TestConstants.VALID_DOI)
                .resolver("DOI")
                .downstreamStatus(null);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(typeValidationService.validate(type, 1)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 1)).thenReturn(Collections.emptyList());
        when(doiService.validate(TestConstants.VALID_DOI, doiFieldId))
                .thenThrow(new ResolverUnavailableException(List.of(unavailable)));
        when(handleService.validate(handleUri, handleFieldId)).thenReturn(Collections.emptyList());

        final var result = validationService.validateRelatedObjects(List.of(doiRelatedObject, handleRelatedObject));

        assertThat(result.failures(), empty());
        assertThat(result.unavailableResolvers(), hasSize(1));
        assertThat(result.unavailableResolvers().get(0), is(unavailable));

        // proves the per-item try/catch didn't abort the loop: the second related object was
        // still checked, and type/category validation across the full list still ran.
        verify(handleService).validate(handleUri, handleFieldId);
        verify(typeValidationService).validate(type, 1);
        verify(categoryValidationService).validate(categories, 1);
    }

    @Test
    @DisplayName("Validation passes with valid Web Archive related object")
    void validWebArchiveRelatedObject() {
        final var webArchiveUri = "https://web.archive.org/web/20220101000000/https://example.com";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(webArchiveUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_WEB_ARCHIVE_ORG_)
                .type(type)
                .category(categories);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(webArchiveService.validate(webArchiveUri, "relatedObject[0].id")).thenReturn(Collections.emptyList());

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, empty());
        verify(webArchiveService).validate(webArchiveUri, "relatedObject[0].id");
    }

    @Test
    @DisplayName("Validation fails if Web Archive service reports a failure")
    void addsFailureIfWebArchiveValidationFails() {
        final var webArchiveUri = "https://web.archive.org/web/20220101000000/https://example.com";
        final var fieldId = "relatedObject[0].id";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(webArchiveUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_WEB_ARCHIVE_ORG_)
                .type(type)
                .category(categories);

        final var failure = new ValidationFailure()
                .fieldId(fieldId)
                .errorType("invalidValue")
                .message("uri not found");

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(webArchiveService.validate(webArchiveUri, fieldId)).thenReturn(List.of(failure));

        final var failures =
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject)).failures();

        assertThat(failures, is(List.of(failure)));
    }

    @Test
    @DisplayName("A resolver failure from the Web Archive service is collected into the unavailable list")
    void webArchiveResolverUnavailableIsCollected() {
        final var webArchiveUri = "https://web.archive.org/web/20220101000000/https://example.com";
        final var fieldId = "relatedObject[0].id";

        final var type = new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);

        final var categories = List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));

        final var relatedObject = new RelatedObject()
                .id(webArchiveUri)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_WEB_ARCHIVE_ORG_)
                .type(type)
                .category(categories);

        final var unavailable = new UnavailableResolver()
                .field(fieldId)
                .value(webArchiveUri)
                .resolver("Web Archive")
                .downstreamStatus(null);

        when(typeValidationService.validate(type, 0)).thenReturn(Collections.emptyList());
        when(categoryValidationService.validate(categories, 0)).thenReturn(Collections.emptyList());
        when(webArchiveService.validate(webArchiveUri, fieldId))
                .thenThrow(new ResolverUnavailableException(List.of(unavailable)));

        final var result = validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

        assertThat(result.failures(), empty());
        assertThat(result.unavailableResolvers(), is(List.of(unavailable)));
    }

    // -----------------------------------------------------------------------
    // RAID-935: skip the resolver for unchanged / already confirmed related objects
    // -----------------------------------------------------------------------

    private static final String DOI_SCHEMA = "https://doi.org/";
    private static final String HANDLE_SCHEMA = "https://hdl.handle.net/";
    private static final String DOI_A = "https://doi.org/10.1000/a";
    private static final String DOI_B = "https://doi.org/10.1000/b";
    private static final String DOI_C = "https://doi.org/10.1000/c";

    private static RelatedObjectType validType() {
        return new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);
    }

    private static List<RelatedObjectCategory> validCategories() {
        return List.of(new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386));
    }

    private static RelatedObject doi(final String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(validType())
                .category(validCategories());
    }

    private static RelatedObject handle(final String id) {
        return new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_HDL_HANDLE_NET_)
                .type(validType())
                .category(validCategories());
    }

    @Test
    @DisplayName("An unchanged related object is not sent to the resolver, but still runs the local checks")
    void unchangedIsSkipped() {
        final var result = validationService.validateRelatedObjects(
                List.of(doi(DOI_A)), Set.of(new RelatedObjectKey(DOI_SCHEMA, DOI_A)));

        assertThat(result.failures(), empty());
        verify(doiService).validateLocally(DOI_A, "relatedObject[0].id");
        verify(doiService, never()).validate(any(), any());
    }

    @Test
    @DisplayName("A related object with a changed id is sent to the resolver")
    void changedIdIsValidated() {
        final var result = validationService.validateRelatedObjects(
                List.of(doi(DOI_B)), Set.of(new RelatedObjectKey(DOI_SCHEMA, DOI_A)));

        assertThat(result.failures(), empty());
        verify(doiService).validate(DOI_B, "relatedObject[0].id");
    }

    @Test
    @DisplayName("A related object with the same id under a changed schemaUri is sent to the resolver")
    void changedSchemaUriIsValidated() {
        final var id = "https://hdl.handle.net/10.1000/a";

        validationService.validateRelatedObjects(
                List.of(handle(id)), Set.of(new RelatedObjectKey(DOI_SCHEMA, id)));

        verify(handleService).validate(id, "relatedObject[0].id");
    }

    @Test
    @DisplayName("A new related object is sent to the resolver while an unchanged one is skipped")
    void newItemIsValidated() {
        validationService.validateRelatedObjects(
                List.of(doi(DOI_A), doi(DOI_B)), Set.of(new RelatedObjectKey(DOI_SCHEMA, DOI_A)));

        verify(doiService, never()).validate(DOI_A, "relatedObject[0].id");
        verify(doiService).validate(DOI_B, "relatedObject[1].id");
    }

    @Test
    @DisplayName("An empty stored set sends every related object to the resolver")
    void emptyStoredSetValidatesEverything() {
        validationService.validateRelatedObjects(List.of(doi(DOI_A), doi(DOI_B)), Set.of());

        verify(doiService).validate(DOI_A, "relatedObject[0].id");
        verify(doiService).validate(DOI_B, "relatedObject[1].id");
    }

    @Test
    @DisplayName("The one-argument method checks every related object")
    void oneArgumentValidatesEverything() {
        validationService.validateRelatedObjects(List.of(doi(DOI_A)));

        verify(doiService).validate(DOI_A, "relatedObject[0].id");
    }

    @Test
    @DisplayName("A null stored set is treated as empty")
    void nullStoredSetValidatesEverything() {
        validationService.validateRelatedObjects(List.of(doi(DOI_A)), null);

        verify(doiService).validate(DOI_A, "relatedObject[0].id");
    }

    @Test
    @DisplayName("Reordered unchanged related objects are still skipped")
    void reorderedUnchangedAreSkipped() {
        final var stored = Set.of(
                new RelatedObjectKey(DOI_SCHEMA, DOI_A),
                new RelatedObjectKey(DOI_SCHEMA, DOI_B));

        final var result = validationService.validateRelatedObjects(List.of(doi(DOI_B), doi(DOI_A)), stored);

        assertThat(result.failures(), empty());
        verify(doiService, never()).validate(any(), any());
    }

    @Test
    @DisplayName("A local failure on an unchanged related object reports the correct index")
    void localFailureOnUnchangedItemUsesItsIndex() {
        final var failure = new ValidationFailure()
                .fieldId("relatedObject[1].id")
                .errorType("invalid")
                .message("local failure");
        when(doiService.validateLocally(DOI_B, "relatedObject[1].id")).thenReturn(List.of(failure));

        final var result = validationService.validateRelatedObjects(
                List.of(doi(DOI_A), doi(DOI_B)), Set.of(new RelatedObjectKey(DOI_SCHEMA, DOI_B)));

        assertThat(result.failures(), is(List.of(failure)));
        verify(doiService, never()).validate(DOI_B, "relatedObject[1].id");
    }

    @Test
    @DisplayName("Type and category are still validated for an unchanged related object")
    void typeAndCategoryStillValidatedForSkippedItem() {
        final var typeFailure = new ValidationFailure().fieldId("relatedObject[0].type").message("bad type");
        final var categoryFailure = new ValidationFailure().fieldId("relatedObject[0].category").message("bad category");
        final var relatedObject = doi(DOI_A);
        when(typeValidationService.validate(relatedObject.getType(), 0)).thenReturn(List.of(typeFailure));
        when(categoryValidationService.validate(relatedObject.getCategory(), 0)).thenReturn(List.of(categoryFailure));

        final var result = validationService.validateRelatedObjects(
                List.of(relatedObject), Set.of(new RelatedObjectKey(DOI_SCHEMA, DOI_A)));

        assertThat(result.failures(), is(List.of(typeFailure, categoryFailure)));
        verify(doiService, never()).validate(any(), any());
    }

    @Test
    @DisplayName("A blank id is still reported for an item when the stored set is non-empty")
    void blankIdStillReported() {
        final var result = validationService.validateRelatedObjects(
                List.of(doi(" ")), Set.of(new RelatedObjectKey(DOI_SCHEMA, DOI_A)));

        assertThat(result.failures(), hasItem(new ValidationFailure()
                .fieldId("relatedObject[0].id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE)));
    }

    // ---- success cache ----

    @Test
    @DisplayName("A successful resolver check is cached, so the same related object is not sent to the resolver again")
    void successIsCachedAndSkippedNextTime() {
        validationService.validateRelatedObjects(List.of(doi(DOI_A)));
        verify(doiService, times(1)).validate(DOI_A, "relatedObject[0].id");

        final var second = validationService.validateRelatedObjects(List.of(doi(DOI_A)));

        assertThat(second.failures(), empty());
        verify(doiService, times(1)).validate(DOI_A, "relatedObject[0].id");
        verify(doiService).validateLocally(DOI_A, "relatedObject[0].id");
        assertThat(successCache.contains(new RelatedObjectKey(DOI_SCHEMA, DOI_A)), is(true));
    }

    @Test
    @DisplayName("A retry after a 503 skips the links the first attempt confirmed")
    void retryAfterUnavailableMakesProgress() {
        final var unavailable = new UnavailableResolver()
                .field("relatedObject[1].id").value(DOI_B).resolver("DOI").downstreamStatus(503);
        when(doiService.validate(DOI_A, "relatedObject[0].id")).thenReturn(List.of());
        when(doiService.validate(DOI_B, "relatedObject[1].id"))
                .thenThrow(new ResolverUnavailableException(List.of(unavailable)))
                .thenReturn(List.of());

        final var first = validationService.validateRelatedObjects(List.of(doi(DOI_A), doi(DOI_B)));
        assertThat(first.unavailableResolvers(), is(List.of(unavailable)));

        final var retry = validationService.validateRelatedObjects(List.of(doi(DOI_A), doi(DOI_B)));

        assertThat(retry.failures(), empty());
        assertThat(retry.unavailableResolvers(), empty());
        verify(doiService, times(1)).validate(DOI_A, "relatedObject[0].id");
        verify(doiService, times(2)).validate(DOI_B, "relatedObject[1].id");
    }

    @Test
    @DisplayName("A failed check is not cached")
    void failureIsNotCached() {
        final var failure = new ValidationFailure()
                .fieldId("relatedObject[0].id").errorType("invalidValue").message("uri not found");
        when(doiService.validate(DOI_A, "relatedObject[0].id")).thenReturn(List.of(failure));

        final var first = validationService.validateRelatedObjects(List.of(doi(DOI_A)));
        final var second = validationService.validateRelatedObjects(List.of(doi(DOI_A)));

        assertThat(first.failures(), is(List.of(failure)));
        assertThat(second.failures(), is(List.of(failure)));
        verify(doiService, times(2)).validate(DOI_A, "relatedObject[0].id");
        assertThat(successCache.contains(new RelatedObjectKey(DOI_SCHEMA, DOI_A)), is(false));
    }

    @Test
    @DisplayName("An unavailable resolver is not cached")
    void unavailableIsNotCached() {
        final var unavailable = new UnavailableResolver()
                .field("relatedObject[0].id").value(DOI_A).resolver("DOI").downstreamStatus(503);
        when(doiService.validate(DOI_A, "relatedObject[0].id"))
                .thenThrow(new ResolverUnavailableException(List.of(unavailable)));

        validationService.validateRelatedObjects(List.of(doi(DOI_A)));
        validationService.validateRelatedObjects(List.of(doi(DOI_A)));

        verify(doiService, times(2)).validate(DOI_A, "relatedObject[0].id");
        assertThat(successCache.contains(new RelatedObjectKey(DOI_SCHEMA, DOI_A)), is(false));
    }

    @Test
    @DisplayName("A cached related object still reports a local failure")
    void cachedItemStillRunsLocalChecks() {
        successCache.recordSuccess(new RelatedObjectKey(DOI_SCHEMA, DOI_C));
        final var failure = new ValidationFailure().fieldId("relatedObject[0].id").message("local failure");
        when(doiService.validateLocally(DOI_C, "relatedObject[0].id")).thenReturn(List.of(failure));

        final var result = validationService.validateRelatedObjects(List.of(doi(DOI_C)));

        assertThat(result.failures(), is(List.of(failure)));
        verify(doiService, never()).validate(any(), any());
    }

    @Test
    @DisplayName("The cache is keyed on schemaUri as well as id")
    void cacheKeyIncludesSchemaUri() {
        final var id = "https://hdl.handle.net/10.1000/a";
        successCache.recordSuccess(new RelatedObjectKey(DOI_SCHEMA, id));

        validationService.validateRelatedObjects(List.of(handle(id)));

        verify(handleService).validate(id, "relatedObject[0].id");
    }

    @Test
    @DisplayName("A disabled cache never skips the resolver")
    void disabledCacheNeverSkips() {
        final var disabled = new RelatedObjectSuccessCache(Duration.ZERO, 100);
        final var validator = new RelatedObjectValidator(
                null, doiService, handleService, rridService, webArchiveService,
                typeValidationService, categoryValidationService, disabled);

        validator.validateRelatedObjects(List.of(doi(DOI_A)));
        validator.validateRelatedObjects(List.of(doi(DOI_A)));

        verify(doiService, times(2)).validate(DOI_A, "relatedObject[0].id");
        assertThat(disabled.isEnabled(), is(false));
    }
}
