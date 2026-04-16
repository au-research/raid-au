package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategorySchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategoryRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategorySchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedObjectCategoryValidatorTest {
    private static final int INDEX = 3;
    private static final int RELATED_OBJECT_CATEGORY_SCHEMA_ID = 1;

    private static final RelatedObjectCategorySchemaRecord RELATED_OBJECT_CATEGORY_SCHEMA_RECORD =
            new RelatedObjectCategorySchemaRecord()
                    .setId(RELATED_OBJECT_CATEGORY_SCHEMA_ID)
                    .setUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI);

    private static final RelatedObjectCategoryRecord RELATED_OBJECT_CATEGORY_RECORD =
            new RelatedObjectCategoryRecord()
                    .setSchemaId(RELATED_OBJECT_CATEGORY_SCHEMA_ID)
                    .setUri(TestConstants.INPUT_RELATED_OBJECT_CATEGORY);

    @Mock
    private RelatedObjectCategoryRepository relatedObjectCategoryRepository;

    @Mock
    private RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository;

    @InjectMocks
    private RelatedObjectCategoryValidator validationService;

    @Test
    @DisplayName("Validation passes with valid related object type")
    void validRelatedObjectCategory() {
        var categories = List.of(new RelatedObjectCategory()
                .id(TestConstants.INPUT_RELATED_OBJECT_CATEGORY)
                .schemaUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI));

        when(relatedObjectCategorySchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_CATEGORY_SCHEMA_RECORD));

        when(relatedObjectCategoryRepository
                .findByUriAndSchemaId(TestConstants.INPUT_RELATED_OBJECT_CATEGORY, RELATED_OBJECT_CATEGORY_SCHEMA_ID))
                .thenReturn(Optional.of(RELATED_OBJECT_CATEGORY_RECORD));

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails with null id")
    void nullId() {
        var categories = List.of(new RelatedObjectCategory()
                .schemaUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI));

        when(relatedObjectCategorySchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_CATEGORY_SCHEMA_RECORD));

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].category[0].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(relatedObjectCategoryRepository);
    }

    @Test
    @DisplayName("Validation fails with empty id")
    void emptyId() {
        var categories = List.of(new RelatedObjectCategory()
                .id("")
                .schemaUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI));

        when(relatedObjectCategorySchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_CATEGORY_SCHEMA_RECORD));

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].category[0].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(relatedObjectCategoryRepository);
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullSchemaUri() {
        var categories = List.of(new RelatedObjectCategory()
                .id(TestConstants.INPUT_RELATED_OBJECT_CATEGORY));

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].category[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
        verifyNoInteractions(relatedObjectCategorySchemaRepository);
        verifyNoInteractions(relatedObjectCategoryRepository);
    }

    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptySchemaUri() {
        var categories = List.of(new RelatedObjectCategory()
                .id(TestConstants.INPUT_RELATED_OBJECT_CATEGORY)
                .schemaUri(""));

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].category[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
        verifyNoInteractions(relatedObjectCategorySchemaRepository);
        verifyNoInteractions(relatedObjectCategoryRepository);
    }

    @Test
    @DisplayName("Validation fails if schemaUri does not exist")
    void nonExistentSchemaUri() {
        var categories = List.of(new RelatedObjectCategory()
                .id(TestConstants.INPUT_RELATED_OBJECT_CATEGORY)
                .schemaUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI));

        when(relatedObjectCategorySchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].category[0].schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));
    }

    @Test
    @DisplayName("Validation fails if type does not exist with schema")
    void invalidTypeForSchema() {
        var categories = List.of(new RelatedObjectCategory()
                .id(TestConstants.INPUT_RELATED_OBJECT_CATEGORY)
                .schemaUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI));

        when(relatedObjectCategorySchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_CATEGORY_SCHEMA_RECORD));

        when(relatedObjectCategoryRepository
                .findByUriAndSchemaId(TestConstants.INPUT_RELATED_OBJECT_CATEGORY, RELATED_OBJECT_CATEGORY_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(categories, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].category[0].id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}