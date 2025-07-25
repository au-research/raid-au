package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static au.org.raid.api.util.TestConstants.VALID_ORCID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorValidatorTest {
    @Mock
    private ContributorRoleValidator roleValidationService;

    @Mock
    private ContributorPositionValidator positionValidationService;

    @Mock
    private ContributorRepository contributorRepository;

    @InjectMocks
    private ContributorValidator validationService;

    @Test
    @DisplayName("Validation fails with missing position")
    void missingPositions() {

        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var contributor = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role))
                .leader(true)
                .contact(true);

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0]")
                        .errorType("notSet")
                        .message("A contributor must have a position")
        ));

        verify(roleValidationService).validate(role, 0, 0);
        verifyNoInteractions(positionValidationService);
    }

    @Test
    @DisplayName("Validation fails with missing leader")
    void missingLeadPositions() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id("https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/other-participant.json")
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .contact(true);

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor")
                        .errorType("notSet")
                        .message("At least one contributor must be flagged as a project leader")
        ));

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
    }

    @Test
    @DisplayName("Validation fails with no contributor")
    void noContributors() {
        final var failures = validationService.validate(Collections.emptyList());

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(roleValidationService);
        verifyNoInteractions(positionValidationService);
    }

    @Test
    @DisplayName("Validation fails with null contributor")
    void nullContributors() {
        final var failures = validationService.validate(null);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(roleValidationService);
        verifyNoInteractions(positionValidationService);
    }

    @Test
    @DisplayName("Validation passes with valid contributor")
    void validContributor() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, empty());

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
    }

    @Test
    @DisplayName("Failures in validation services are added to return value")
    void roleValidationFailures() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        final var roleError = new ValidationFailure()
                .fieldId("contributor[0].roles[0].role")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        final var positionError = new ValidationFailure()
                .fieldId("contributor[0].position[0].position")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        when(roleValidationService.validate(role, 0, 0)).thenReturn(List.of(roleError));
        when(positionValidationService.validate(position, 0, 0)).thenReturn(List.of(positionError));

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(2));

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
    }

    @Test
    @DisplayName("Validation passes with multiple lead position - year-month dates")
    void multipleLeadPositionsWithYearMonthDates() {
        final var role1 = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01")
                .endDate("2021-06");

        final var contributor1 = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role1))
                .position(List.of(position1))
                .leader(true)
                .contact(true);

        final var role2 = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position2 = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate("2021-06")
                .endDate("2023-06");

        final var contributor2 = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role2))
                .position(List.of(position2))
                .leader(true)
                .contact(true);

        final var failures = validationService.validate(List.of(contributor2, contributor1));

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails if contributor has overlapping positions - year-month-day dates")
    void overlappingPositions() {
        final var role1 = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01")
                .endDate("2021-12-31");

        final var position2 = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate("2021-06-01")
                .endDate("2023-06-01");

        final var contributor1 = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role1))
                .position(List.of(position1, position2))
                .leader(true)
                .contact(true);


        final var failures = validationService.validate(List.of(contributor1));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("contributor[0].position[1].startDate")
                        .errorType("invalidValue")
                        .message("Contributors can only hold one position at any given time. This position conflicts with contributor[0].position[0]")
        )));
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullIdentifierSchemeUri() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

//        when(contributorRepository.findByPid(VALID_ORCID)).thenReturn(Optional.of(new ContributorRecord()));

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
    }
    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptyIdentifierSchemeUri() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri("")
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
    }

    @Test
    @DisplayName("Validation fails with invalid schemaUri")
    void invalidIdentifierSchemeUri() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri("https://example.org/")
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType("invalidValue")
                        .message("has invalid/unsupported value - should be https://orcid.org/")
        ));

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
    }

    @Test
    @DisplayName("Validation passes with valid orcid")
    void validOrcid() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(0));

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
        verifyNoInteractions(contributorRepository);
    }

    @Test
    @DisplayName("Validation passes with valid contributor with email address")
    void validContributorWithEmail() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(TestConstants.CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(TestConstants.LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(TestConstants.CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);


        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, empty());

        verify(roleValidationService).validate(role, 0, 0);
        verify(positionValidationService).validate(position, 0, 0);
        verifyNoInteractions(contributorRepository);
    }

}