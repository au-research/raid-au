package au.org.raid.api.validator;

import au.org.raid.api.repository.DescriptionTypeRepository;
import au.org.raid.api.repository.DescriptionTypeSchemaRepository;
import au.org.raid.db.jooq.tables.records.DescriptionTypeRecord;
import au.org.raid.db.jooq.tables.records.DescriptionTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.DescriptionTypeIdEnum;
import au.org.raid.idl.raidv2.model.DescriptionTypeSchemaURIEnum;
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
class DescriptionTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int DESCRIPTION_TYPE_SCHEMA_ID = 1;

    private static final DescriptionTypeSchemaRecord DESCRIPTION_TYPE_SCHEMA_RECORD = new DescriptionTypeSchemaRecord()
            .setId(DESCRIPTION_TYPE_SCHEMA_ID)
            .setUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320.getValue());

    private static final DescriptionTypeRecord DESCRIPTION_TYPE_RECORD = new DescriptionTypeRecord()
            .setSchemaId(DESCRIPTION_TYPE_SCHEMA_ID)
            .setUri(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3.getValue());

    @Mock
    private DescriptionTypeSchemaRepository descriptionTypeSchemaRepository;
    @Mock
    private DescriptionTypeRepository descriptionTypeRepository;
    @InjectMocks
    private DescriptionTypeValidator validationService;


    @Test
    @DisplayName("Validation passes with valid description type")
    void validDescriptionType() {
        final var descriptionType = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3)
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        when(descriptionTypeSchemaRepository.findActiveByUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320.getValue()))
                .thenReturn(Optional.of(DESCRIPTION_TYPE_SCHEMA_RECORD));
        when(descriptionTypeRepository.findByUriAndSchemaId(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3.getValue(), DESCRIPTION_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(DESCRIPTION_TYPE_RECORD));

        final var failures = validationService.validate(descriptionType, INDEX);

        assertThat(failures, empty());

        verify(descriptionTypeSchemaRepository).findActiveByUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320.getValue());
        verify(descriptionTypeRepository).findByUriAndSchemaId(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3.getValue(), DESCRIPTION_TYPE_SCHEMA_ID);
    }

    @Test
    @DisplayName("Validation fails when id is null")
    void nullId() {
        final var descriptionType = new DescriptionType()
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        when(descriptionTypeSchemaRepository.findActiveByUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320.getValue()))
                .thenReturn(Optional.of(DESCRIPTION_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(descriptionType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("description[3].type.id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is null")
    void nullSchemeUri() {
        final var descriptionType = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3);

        final var failures = validationService.validate(descriptionType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("description[3].type.schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is invalid")
    void invalidSchemeUri() {
        final var descriptionType = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3)
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        when(descriptionTypeSchemaRepository.findActiveByUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320.getValue()))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(descriptionType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("description[3].type.schemaUri")
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
                        .fieldId("description[3].type")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(descriptionTypeSchemaRepository);
        verifyNoInteractions(descriptionTypeRepository);
    }

    @Test
    @DisplayName("Validation fails when id not found in schema")
    void invalidTypeForScheme() {
        final var descriptionType = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3)
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        when(descriptionTypeSchemaRepository.findActiveByUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320.getValue()))
                .thenReturn(Optional.of(DESCRIPTION_TYPE_SCHEMA_RECORD));

        when(descriptionTypeRepository.findByUriAndSchemaId(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3.getValue(), DESCRIPTION_TYPE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(descriptionType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("description[3].type.id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}
