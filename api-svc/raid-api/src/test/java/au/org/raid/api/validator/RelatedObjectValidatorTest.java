package au.org.raid.api.validator;

import au.org.raid.api.service.doi.DoiService;
import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectCategorySchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedObjectValidatorTest {
    @Mock
    private RelatedObjectTypeValidator typeValidationService;

    @Mock
    private RelatedObjectCategoryValidator categoryValidationService;

    @Mock
    private DoiService doiService;

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
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Passes validation with empty related objects")
    void emptyRelatedObjects() {
        final var failures = validationService.validateRelatedObjects(Collections.emptyList());

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Passes validation with null related objects")
    void nullRelatedObjects() {
        final var failures = validationService.validateRelatedObjects(null);

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
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

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
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

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
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

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
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

        assertThat(failures, is(List.of(failure)));
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
                validationService.validateRelatedObjects(Collections.singletonList(relatedObject));

        assertThat(failures, hasSize(2));
        assertThat(failures, hasItems(typeError, categoryError));
    }
}
