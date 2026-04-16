package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.repository.RelatedObjectTypeSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedObjectTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int RELATED_OBJECT_TYPE_SCHEMA_ID = 1;

    private static final RelatedObjectTypeSchemaRecord RELATED_OBJECT_TYPE_SCHEMA_RECORD =
            new RelatedObjectTypeSchemaRecord()
                    .setId(RELATED_OBJECT_TYPE_SCHEMA_ID)
                    .setUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

    private static final RelatedObjectTypeRecord RELATED_OBJECT_TYPE_RECORD =
            new RelatedObjectTypeRecord()
                    .setSchemaId(RELATED_OBJECT_TYPE_SCHEMA_ID)
                    .setUri(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE);

    @Mock
    private RelatedObjectTypeRepository relatedObjectTypeRepository;

    @Mock
    private RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository;

    @InjectMocks
    private RelatedObjectTypeValidator validationService;

    @Test
    @DisplayName("Validation passes with valid related object type")
    void validRelatedObjectType() {
        var relatedObjectType = new RelatedObjectType()
                .id(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE)
                .schemaUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

        when(relatedObjectTypeSchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_TYPE_SCHEMA_RECORD));

        when(relatedObjectTypeRepository
                .findByUriAndSchemaId(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE, RELATED_OBJECT_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(RELATED_OBJECT_TYPE_RECORD));

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails with null id")
    void nullId() {
        var relatedObjectType = new RelatedObjectType()
                .schemaUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

        when(relatedObjectTypeSchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].type.id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(relatedObjectTypeRepository);
    }

    @Test
    @DisplayName("Validation fails with empty id")
    void emptyId() {
        var relatedObjectType = new RelatedObjectType()
                .id("")
                .schemaUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

        when(relatedObjectTypeSchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].type.id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
        verifyNoInteractions(relatedObjectTypeRepository);
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullSchemaUri() {
        var relatedObjectType = new RelatedObjectType()
                .id(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE);

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].type.schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
        verifyNoInteractions(relatedObjectTypeSchemaRepository);
        verifyNoInteractions(relatedObjectTypeRepository);
    }

    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptySchemaUri() {
        var relatedObjectType = new RelatedObjectType()
                .id(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE)
                .schemaUri("");

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].type.schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
        verifyNoInteractions(relatedObjectTypeSchemaRepository);
        verifyNoInteractions(relatedObjectTypeRepository);
    }

    @Test
    @DisplayName("Validation fails if schemaUri does not exist")
    void nonExistentSchemaUri() {
        var relatedObjectType = new RelatedObjectType()
                .id(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE)
                .schemaUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

        when(relatedObjectTypeSchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].type.schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));
    }

    @Test
    @DisplayName("Validation fails if type does not exist with schema")
    void invalidTypeForSchema() {
        var relatedObjectType = new RelatedObjectType()
                .id(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE)
                .schemaUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

        when(relatedObjectTypeSchemaRepository.findActiveByUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(RELATED_OBJECT_TYPE_SCHEMA_RECORD));

        when(relatedObjectTypeRepository
                .findByUriAndSchemaId(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE, RELATED_OBJECT_TYPE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(relatedObjectType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedObject[3].type.id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}