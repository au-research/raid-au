package au.org.raid.api.validator;

import au.org.raid.api.client.contributor.ContributorClient;
import au.org.raid.api.config.properties.ContributorValidationProperties.ContributorTypeValidationProperties;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.TestConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorTypeValidatorTest {
    private static final String URL_PREFIX = "https://orcid.org/";
    private static final String SCHEMA_URI_PATTERN = "https://orcid.org/";
    private static final int CONTRIBUTOR_INDEX = 0;

    @Mock
    private ContributorClient contributorClient;

    @Mock
    private ContributorRoleValidator roleValidator;

    @Mock
    private ContributorPositionValidator positionValidator;

    private ContributorTypeValidator validator;
    private ContributorTypeValidationProperties validationProperties;

    @BeforeEach
    void setUp() {
        validationProperties = ContributorTypeValidationProperties.builder()
                .urlPrefix(URL_PREFIX)
                .schemaUri(SCHEMA_URI_PATTERN)
                .build();

        validator = new ContributorTypeValidator(
                validationProperties,
                contributorClient,
                roleValidator,
                positionValidator
        );
    }

    @Test
    @DisplayName("Validation passes with valid contributor")
    void validContributor() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, empty());

        verify(contributorClient).exists(VALID_ORCID);
        verify(roleValidator).validate(role, CONTRIBUTOR_INDEX, 0);
        verify(positionValidator).validate(position, CONTRIBUTOR_INDEX, 0);
    }

    @Test
    @DisplayName("Validation fails with blank contributor id")
    void blankContributorId() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id("")
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].id")
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
        ));

        verifyNoInteractions(contributorClient);
    }

    @Test
    @DisplayName("Validation fails with null contributor id")
    void nullContributorId() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].id")
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
        ));

        verifyNoInteractions(contributorClient);
    }

    @Test
    @DisplayName("Validation fails when contributor id doesn't start with URL prefix")
    void contributorIdWithInvalidPrefix() {
        final var invalidId = "https://invalid.org/0000-0000-0000-0001";
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(invalidId)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].id")
                        .errorType(INVALID_VALUE_TYPE)
                        .message("Contributor id should start with " + URL_PREFIX)
        ));

        verifyNoInteractions(contributorClient);
    }

    @Test
    @DisplayName("Validation fails when contributor id does not exist")
    void contributorIdDoesNotExist() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(false);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].id")
                        .errorType(NOT_FOUND_TYPE)
                        .message("This id does not exist")
        ));

        verify(contributorClient).exists(VALID_ORCID);
    }

    @Test
    @DisplayName("Validation fails with blank schema URI")
    void blankSchemaUri() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri("")
                .role(List.of(role))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
        ));
    }

    @Test
    @DisplayName("Validation fails with null schema URI")
    void nullSchemaUri() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
        ));
    }

    @Test
    @DisplayName("Validation fails with invalid schema URI")
    void invalidSchemaUri() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri("https://invalid.org/")
                .role(List.of(role))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_VALUE_MESSAGE + " - should be " + SCHEMA_URI_PATTERN)
        ));
    }

    @Test
    @DisplayName("Validation fails with null position list")
    void nullPositionList() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0]")
                        .errorType(NOT_SET_TYPE)
                        .message("A contributor must have a position")
        ));

        verifyNoInteractions(positionValidator);
    }

    @Test
    @DisplayName("Validation fails with empty position list")
    void emptyPositionList() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(Collections.emptyList());

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0]")
                        .errorType(NOT_SET_TYPE)
                        .message("A contributor must have a position")
        ));

        verifyNoInteractions(positionValidator);
    }

    @Test
    @DisplayName("Validation delegates to role validator for each role")
    void multipleRoles() {
        final var role1 = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var role2 = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role1, role2))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role1, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(roleValidator.validate(role2, CONTRIBUTOR_INDEX, 1)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, empty());

        verify(roleValidator).validate(role1, CONTRIBUTOR_INDEX, 0);
        verify(roleValidator).validate(role2, CONTRIBUTOR_INDEX, 1);
    }

    @Test
    @DisplayName("Validation collects failures from role validator")
    void roleValidatorFailures() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        final var roleFailure = new ValidationFailure()
                .fieldId("contributor[0].role[0].id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(List.of(roleFailure));
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(roleFailure));
    }

    @Test
    @DisplayName("Validation delegates to position validator for each position")
    void multiplePositions() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01")
                .endDate("2021-12-31");

        final var position2 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2022-01-01");

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position1, position2));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position1, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position2, CONTRIBUTOR_INDEX, 1)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, empty());

        verify(positionValidator).validate(position1, CONTRIBUTOR_INDEX, 0);
        verify(positionValidator).validate(position2, CONTRIBUTOR_INDEX, 1);
    }

    @Test
    @DisplayName("Validation collects failures from position validator")
    void positionValidatorFailures() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        final var positionFailure = new ValidationFailure()
                .fieldId("contributor[0].position[0].id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(List.of(positionFailure));

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(positionFailure));
    }

    @Test
    @DisplayName("Validation fails with overlapping positions")
    void overlappingPositions() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01")
                .endDate("2021-12-31");

        final var position2 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2021-06-01")
                .endDate("2023-06-01");

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position1, position2));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position1, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position2, CONTRIBUTOR_INDEX, 1)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].position[1].startDate")
                        .errorType(INVALID_VALUE_TYPE)
                        .message("Contributors can only hold one position at any given time. This position conflicts with contributor[0].position[0]")
        ));
    }

    @Test
    @DisplayName("Validation passes with non-overlapping positions")
    void nonOverlappingPositions() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01")
                .endDate("2021-12-31");

        final var position2 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2022-01-01")
                .endDate("2023-06-01");

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position1, position2));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position1, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position2, CONTRIBUTOR_INDEX, 1)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation passes when positions end date matches next start date")
    void positionsWithMatchingEndAndStartDates() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01")
                .endDate("2022-01-01");

        final var position2 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2022-01-01")
                .endDate("2023-06-01");

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position1, position2));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position1, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position2, CONTRIBUTOR_INDEX, 1)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation handles position without end date")
    void positionWithoutEndDate() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01");

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails when position without end date overlaps with earlier position")
    void positionWithoutEndDateOverlaps() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position1 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2020-01-01");

        final var position2 = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate("2021-01-01");

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ORCID_SCHEMA_URI)
                .role(List.of(role))
                .position(List.of(position1, position2));

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position1, CONTRIBUTOR_INDEX, 0)).thenReturn(Collections.emptyList());
        when(positionValidator.validate(position2, CONTRIBUTOR_INDEX, 1)).thenReturn(Collections.emptyList());

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].position[1].startDate")
                        .errorType(INVALID_VALUE_TYPE)
                        .message("Contributors can only hold one position at any given time. This position conflicts with contributor[0].position[0]")
        ));
    }

    @Test
    @DisplayName("Validation collects multiple failures from different validators")
    void multipleFailures() {
        final var role = new ContributorRole()
                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(SUPERVISION_CONTRIBUTOR_ROLE);

        final var position = new ContributorPosition()
                .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                .id(LEADER_CONTRIBUTOR_POSITION)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri("https://invalid.org/")
                .role(List.of(role))
                .position(List.of(position));

        final var roleFailure = new ValidationFailure()
                .fieldId("contributor[0].role[0].id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        final var positionFailure = new ValidationFailure()
                .fieldId("contributor[0].position[0].id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        when(contributorClient.exists(VALID_ORCID)).thenReturn(true);
        when(roleValidator.validate(role, CONTRIBUTOR_INDEX, 0)).thenReturn(List.of(roleFailure));
        when(positionValidator.validate(position, CONTRIBUTOR_INDEX, 0)).thenReturn(List.of(positionFailure));

        final var failures = validator.validate(contributor, CONTRIBUTOR_INDEX);

        assertThat(failures, hasSize(3));
        assertThat(failures, hasItem(roleFailure));
        assertThat(failures, hasItem(positionFailure));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_VALUE_MESSAGE + " - should be " + SCHEMA_URI_PATTERN)
        ));
    }
}