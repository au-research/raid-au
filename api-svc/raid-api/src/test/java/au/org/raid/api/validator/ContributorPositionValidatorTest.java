package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorPositionRepository;
import au.org.raid.api.repository.ContributorPositionSchemaRepository;
import au.org.raid.db.jooq.tables.records.ContributorPositionRecord;
import au.org.raid.db.jooq.tables.records.ContributorPositionSchemaRecord;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static au.org.raid.api.util.TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI;
import static au.org.raid.api.util.TestConstants.LEADER_CONTRIBUTOR_POSITION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorPositionValidatorTest {
    private static final int CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID = 1;

    private static final ContributorPositionSchemaRecord CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD =
            new ContributorPositionSchemaRecord()
                    .setId(CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID)
                    .setUri(CONTRIBUTOR_POSITION_SCHEMA_URI);

    private static final ContributorPositionRecord CONTRIBUTOR_POSITION_TYPE_RECORD =
            new ContributorPositionRecord()
                    .setSchemaId(CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID)
                    .setUri(LEADER_CONTRIBUTOR_POSITION);


    @Mock
    private ContributorPositionSchemaRepository contributorPositionSchemaRepository;

    @Mock
    private ContributorPositionRepository contributorPositionRepository;

    @InjectMocks
    private ContributorPositionValidator validationService;

    @Test
    @DisplayName("Validation passes with valid ContributorPosition")
    void validContributorPosition() {
        final var position = new ContributorPosition()
                .id(LEADER_CONTRIBUTOR_POSITION)
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD));

        when(contributorPositionRepository
                .findByUriAndSchemaId(LEADER_CONTRIBUTOR_POSITION, CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_RECORD));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails if end date is before start date")
    void endDateBeforeStartDate() {
        final var position = new ContributorPosition()
                .id(LEADER_CONTRIBUTOR_POSITION)
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .startDate("2022-03")
                .endDate("2022-02");

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD));

        when(contributorPositionRepository
                .findByUriAndSchemaId(LEADER_CONTRIBUTOR_POSITION, CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_RECORD));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].endDate")
                        .errorType("invalidValue")
                        .message("end date is before start date")
        )));
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullSchemaUri() {
        final var position = new ContributorPosition()
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorPositionSchemaRepository);
        verifyNoInteractions(contributorPositionRepository);
    }

    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptySchemaUri() {
        final var position = new ContributorPosition()
                .schemaUri("")
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorPositionSchemaRepository);
        verifyNoInteractions(contributorPositionRepository);
    }

    @Test
    @DisplayName("Validation fails with invalid schemaUri")
    void invalidSchemaUri() {
        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));

        verifyNoInteractions(contributorPositionRepository);
    }

    @Test
    @DisplayName("Validation fails with null position")
    void nullPosition() {
        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorPositionRepository);
    }

    @Test
    @DisplayName("Validation fails with empty position")
    void emptyPosition() {
        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id("")
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorPositionRepository);
    }

    @Test
    @DisplayName("Validation fails with invalid position")
    void invalidPosition() {
        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD));

        when(contributorPositionRepository
                .findByUriAndSchemaId(LEADER_CONTRIBUTOR_POSITION, CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }

    @Test
    @DisplayName("Validation fails with null startDate")
    void nullstartDate() {
        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        when(contributorPositionSchemaRepository.findActiveByUri(CONTRIBUTOR_POSITION_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_SCHEMA_RECORD));

        when(contributorPositionRepository
                .findByUriAndSchemaId(LEADER_CONTRIBUTOR_POSITION, CONTRIBUTOR_POSITION_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(CONTRIBUTOR_POSITION_TYPE_RECORD));

        final var failures = validationService.validate(position, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].position[3].startDate")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }
}