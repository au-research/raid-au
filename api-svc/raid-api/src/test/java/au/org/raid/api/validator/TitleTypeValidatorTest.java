package au.org.raid.api.validator;

import au.org.raid.api.repository.TitleTypeRepository;
import au.org.raid.api.repository.TitleTypeSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.TitleTypeRecord;
import au.org.raid.db.jooq.tables.records.TitleTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.TitleType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TitleTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int TITLE_TYPE_SCHEMA_ID = 1;

    private static final TitleTypeSchemaRecord TITLE_TYPE_SCHEMA_RECORD = new TitleTypeSchemaRecord()
            .setId(TITLE_TYPE_SCHEMA_ID)
            .setUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

    private static final TitleTypeRecord TITLE_TYPE_RECORD = new TitleTypeRecord()
            .setSchemaId(TITLE_TYPE_SCHEMA_ID)
            .setUri(TestConstants.PRIMARY_TITLE_TYPE_ID);

    @Mock
    private TitleTypeSchemaRepository titleTypeSchemaRepository;
    @Mock
    private TitleTypeRepository titleTypeRepository;
    @InjectMocks
    private TitleTypeValidator validationService;


    @Test
    @DisplayName("Validation passes with valid title type")
    void validTitleType() {
        final var titleType = new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE_ID)
                .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

        when(titleTypeSchemaRepository.findActiveByUri(TestConstants.TITLE_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(TITLE_TYPE_SCHEMA_RECORD));
        when(titleTypeRepository.findByUriAndSchemaId(TestConstants.PRIMARY_TITLE_TYPE_ID, TITLE_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(TITLE_TYPE_RECORD));

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, empty());

        verify(titleTypeSchemaRepository).findActiveByUri(TestConstants.TITLE_TYPE_SCHEMA_URI);
        verify(titleTypeRepository).findByUriAndSchemaId(TestConstants.PRIMARY_TITLE_TYPE_ID, TITLE_TYPE_SCHEMA_ID);
    }

    @Test
    @DisplayName("Validation fails when id is null")
    void nullId() {
        final var titleType = new TitleType()
                .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

        when(titleTypeSchemaRepository.findActiveByUri(TestConstants.TITLE_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(TITLE_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type.id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when id is empty string")
    void emptyId() {
        final var titleType = new TitleType()
                .id("")
                .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

        when(titleTypeSchemaRepository.findActiveByUri(TestConstants.TITLE_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(TITLE_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type.id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is null")
    void nullSchemaUri() {
        final var titleType = new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE_ID);

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type.schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is empty")
    void emptySchemaUri() {
        final var titleType = new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE_ID)
                .schemaUri("");

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type.schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is invalid")
    void invalidSchemaUri() {
        final var titleType = new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE_ID)
                .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

        when(titleTypeSchemaRepository.findActiveByUri(TestConstants.TITLE_TYPE_SCHEMA_URI))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type.schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));
    }

    @Test
    @DisplayName("Validation fails when type is null")
    void nullType() {
        final var failures = validationService.validate(null, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(titleTypeSchemaRepository);
        verifyNoInteractions(titleTypeRepository);
    }

    @Test
    @DisplayName("Validation fails when id not found in schema")
    void invalidTypeForSchema() {
        final var titleType = new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE_ID)
                .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

        when(titleTypeSchemaRepository.findActiveByUri(TestConstants.TITLE_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(TITLE_TYPE_SCHEMA_RECORD));

        when(titleTypeRepository.findByUriAndSchemaId(TestConstants.PRIMARY_TITLE_TYPE_ID, TITLE_TYPE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(titleType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("title[3].type.id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}