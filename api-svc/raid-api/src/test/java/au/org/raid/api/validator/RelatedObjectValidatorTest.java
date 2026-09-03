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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private RelatedObjectValidator validationService;

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
}
