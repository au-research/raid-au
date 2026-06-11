package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedRaidTypeRepository;
import au.org.raid.api.repository.RelatedRaidTypeSchemaRepository;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeSchemaUriEnum;
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
class RelatedRaidTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int RELATED_RAID_TYPE_SCHEMA_ID = 1;

    private static final RelatedRaidTypeSchemaRecord RELATED_RAID_TYPE_SCHEMA_RECORD = new RelatedRaidTypeSchemaRecord()
            .setId(RELATED_RAID_TYPE_SCHEMA_ID)
            .setUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367.getValue());

    private static final RelatedRaidTypeRecord RELATED_RAID_TYPE_RECORD = new RelatedRaidTypeRecord()
            .setSchemaId(RELATED_RAID_TYPE_SCHEMA_ID)
            .setUri(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198.getValue());

    @Mock
    private RelatedRaidTypeSchemaRepository relatedRaidTypeSchemaRepository;
    @Mock
    private RelatedRaidTypeRepository relatedRaidTypeRepository;
    @InjectMocks
    private RelatedRaidTypeValidator validationService;

    @Test
    @DisplayName("Validation passes with valid related raid type")
    void validRelatedRaidType() {
        final var relatedRaidType = new RelatedRaidType()
                .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198)
                .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367);

        when(relatedRaidTypeSchemaRepository.findActiveByUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367.getValue()))
                .thenReturn(Optional.of(RELATED_RAID_TYPE_SCHEMA_RECORD));
        when(relatedRaidTypeRepository.findByUriAndSchemaId(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198.getValue(), RELATED_RAID_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(RELATED_RAID_TYPE_RECORD));

        final var failures = validationService.validate(relatedRaidType, INDEX);

        assertThat(failures, empty());

        verify(relatedRaidTypeSchemaRepository).findActiveByUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367.getValue());
        verify(relatedRaidTypeRepository).findByUriAndSchemaId(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198.getValue(), RELATED_RAID_TYPE_SCHEMA_ID);
    }

    @Test
    @DisplayName("Validation fails when id is null")
    void nullId() {
        final var relatedRaidType = new RelatedRaidType()
                .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367);

        when(relatedRaidTypeSchemaRepository.findActiveByUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367.getValue()))
                .thenReturn(Optional.of(RELATED_RAID_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(relatedRaidType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedRaid[3].type.id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is null")
    void nullSchemaUri() {
        final var relatedRaidType = new RelatedRaidType()
                .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198);

        final var failures = validationService.validate(relatedRaidType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedRaid[3].type.schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails when schemaUri is invalid")
    void invalidSchemaUri() {
        final var relatedRaidType = new RelatedRaidType()
                .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198)
                .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367);

        when(relatedRaidTypeSchemaRepository.findActiveByUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367.getValue()))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(relatedRaidType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedRaid[3].type.schemaUri")
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
                        .fieldId("relatedRaid[3].type")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(relatedRaidTypeSchemaRepository);
        verifyNoInteractions(relatedRaidTypeRepository);
    }

    @Test
    @DisplayName("Validation fails when id not found in schema")
    void invalidTypeForSchema() {
        final var relatedRaidType = new RelatedRaidType()
                .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198)
                .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367);

        when(relatedRaidTypeSchemaRepository.findActiveByUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367.getValue()))
                .thenReturn(Optional.of(RELATED_RAID_TYPE_SCHEMA_RECORD));

        when(relatedRaidTypeRepository.findByUriAndSchemaId(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198.getValue(), RELATED_RAID_TYPE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(relatedRaidType, INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("relatedRaid[3].type.id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}
