package au.org.raid.api.validator;

import au.org.raid.api.config.properties.ContributorValidationProperties;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorPositionIdEnum;
import au.org.raid.idl.raidv2.model.ContributorPositionSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ContributorRoleIdEnum;
import au.org.raid.idl.raidv2.model.ContributorRoleSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ContributorSchemaUriEnum;
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

import static au.org.raid.api.util.TestConstants.VALID_ISNI;
import static au.org.raid.api.util.TestConstants.VALID_ORCID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorValidatorTest {

    @Mock
    private ContributorRepository contributorRepository;

    @Mock
    private ContributorTypeValidator isniValidator;

    @Mock
    private ContributorTypeValidator orcidValidator;

    @Mock
    private ContributorValidationProperties validationProperties;

    @Mock
    private ContributorRoleValidator roleValidator;

    @Mock
    private ContributorPositionValidator positionValidator;

    private ContributorValidator validationService;

    @BeforeEach
    void setUp() {
        final var orcidProperties = ContributorValidationProperties.ContributorTypeValidationProperties.builder()
                .urlPrefix(TestConstants.ORCID_SCHEMA_URI)
                .schemaUri(TestConstants.ORCID_SCHEMA_URI)
                .build();

        final var isniProperties = ContributorValidationProperties.ContributorTypeValidationProperties.builder()
                .urlPrefix(TestConstants.ISNI_SCHEMA_URI)
                .schemaUri(TestConstants.ISNI_SCHEMA_URI)
                .build();

        lenient().when(validationProperties.getOrcid()).thenReturn(orcidProperties);
        lenient().when(validationProperties.getIsni()).thenReturn(isniProperties);

        validationService = new ContributorValidator(
                contributorRepository,
                isniValidator,
                orcidValidator,
                validationProperties,
                roleValidator,
                positionValidator
        );
    }

    @Test
    @DisplayName("Validation fails with missing position")
    void missingPositions() {

        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(List.of(
                new ValidationFailure()
                        .fieldId("contributor[0]")
                        .errorType("notSet")
                        .message("A contributor must have a position")
        ));

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0]")
                        .errorType("notSet")
                        .message("A contributor must have a position")
        ));

        verify(orcidValidator).validate(contributor, 0);
    }

    @Test
    @DisplayName("Validation fails with missing leader")
    void missingLeadPositions() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_308)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor")
                        .errorType("notSet")
                        .message("At least one contributor must be flagged as a project leader")
        ));

        verify(orcidValidator).validate(contributor, 0);
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

        verifyNoInteractions(orcidValidator);
        verifyNoInteractions(isniValidator);
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

        verifyNoInteractions(orcidValidator);
        verifyNoInteractions(isniValidator);
    }

    @Test
    @DisplayName("Validation passes with valid contributor")
    void validContributor() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, empty());

        verify(orcidValidator).validate(contributor, 0);
    }

    @Test
    @DisplayName("Failures in validation services are added to return value")
    void roleValidationFailures() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        final var roleError = new ValidationFailure()
                .fieldId("contributor[0].roles[0].role")
                .errorType("notSet")
                .message("field must be set");

        final var positionError = new ValidationFailure()
                .fieldId("contributor[0].position[0].position")
                .errorType("notSet")
                .message("field must be set");

        when(orcidValidator.validate(contributor, 0)).thenReturn(List.of(roleError, positionError));

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(2));

        verify(orcidValidator).validate(contributor, 0);
    }

    @Test
    @DisplayName("Validation passes with multiple lead position - year-month dates")
    void multipleLeadPositionsWithYearMonthDates() {

        final var orcid = "https://orcid.org/0000-0000-0000-0002";
        final var role1 = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position1 = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate("2020-01")
                .endDate("2021-06");

        final var contributor1 = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role1))
                .position(List.of(position1))
                .leader(true)
                .contact(true);

        final var role2 = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position2 = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate("2021-06")
                .endDate("2023-06");

        final var contributor2 = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(orcid)
                .role(List.of(role2))
                .position(List.of(position2))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(any(Contributor.class), anyInt())).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor2, contributor1));

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails with duplicate contributors")
    void duplicateContributors() {
        final var role1 = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position1 = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate("2020-01")
                .endDate("2021-06");

        final var contributor1 = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role1))
                .position(List.of(position1))
                .leader(true)
                .contact(true);

        final var role2 = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position2 = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate("2021-06")
                .endDate("2023-06");

        final var contributor2 = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role2))
                .position(List.of(position2))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(any(Contributor.class), anyInt())).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor2, contributor1));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("contributor[1].id")
                        .errorType("duplicateValue")
                        .message("A contributor can appear only once. There are 2 occurrences of https://orcid.org/0000-0000-0000-0001")
        )));
    }

    @Test
    @DisplayName("Validation fails if contributor has overlapping positions - year-month-day dates")
    void overlappingPositions() {
        final var role1 = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position1 = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate("2020-01-01")
                .endDate("2021-12-31");

        final var position2 = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate("2021-06-01")
                .endDate("2023-06-01");

        final var contributor1 = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role1))
                .position(List.of(position1, position2))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor1, 0)).thenReturn(List.of(
                new ValidationFailure()
                        .fieldId("contributor[0].position[1].startDate")
                        .errorType("invalidValue")
                        .message("Contributors can only hold one position at any given time. This position conflicts with contributor[0].position[0]")
        ));

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
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(List.of(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verify(orcidValidator).validate(contributor, 0);
    }

    @Test
    @DisplayName("Validation passes with valid orcid")
    void validOrcid() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(0));

        verify(orcidValidator).validate(contributor, 0);
        verifyNoInteractions(contributorRepository);
    }

    @Test
    @DisplayName("Validation passes with valid isni")
    void validIsni() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ISNI_ORG_)
                .id(VALID_ISNI)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(isniValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(0));

        verify(isniValidator).validate(contributor, 0);
        verifyNoInteractions(contributorRepository);
    }

    @Test
    @DisplayName("Validation passes with valid contributor with email address")
    void validContributorWithEmail() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .id(VALID_ORCID)
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, empty());

        verify(orcidValidator).validate(contributor, 0);
        verifyNoInteractions(contributorRepository);
    }

    @Test
    @DisplayName("Validation fails with non-existent ORCID")
    void nonExistentOrcid() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(List.of(
                new ValidationFailure()
                        .fieldId("contributor[0].id")
                        .errorType("notFound")
                        .message("This id does not exist")
        ));

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, contains(new ValidationFailure(
                "contributor[0].id",
                "notFound",
                "This id does not exist"
        )));

        verify(orcidValidator).validate(contributor, 0);
    }

    @Test
    @DisplayName("isOrcid matches on schemaUri enum when id does not start with orcid prefix")
    void isOrcidMatchesOnSchemaUri() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id("not-an-orcid-prefix")
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, empty());
        verify(orcidValidator).validate(contributor, 0);
        verifyNoInteractions(isniValidator);
    }

    @Test
    @DisplayName("isIsni matches on schemaUri enum when id does not start with isni prefix")
    void isIsniMatchesOnSchemaUri() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ISNI_ORG_)
                .id("not-an-isni-prefix")
                .role(List.of(role))
                .position(List.of(position))
                .leader(true)
                .contact(true);

        when(isniValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, empty());
        verify(isniValidator).validate(contributor, 0);
        verifyNoInteractions(orcidValidator);
    }

    @Test
    @DisplayName("Validation fails with missing contact")
    void missingContact() {
        final var role = new ContributorRole()
                .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_)
                .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SUPERVISION_);

        final var position = new ContributorPosition()
                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var contributor = new Contributor()
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .id(VALID_ORCID)
                .role(List.of(role))
                .position(List.of(position))
                .leader(true);

        when(orcidValidator.validate(contributor, 0)).thenReturn(Collections.emptyList());

        final var failures = validationService.validate(List.of(contributor));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor")
                        .errorType("notSet")
                        .message("At least one contributor must be flagged as a project contact")
        ));

        verify(orcidValidator).validate(contributor, 0);
    }
}
