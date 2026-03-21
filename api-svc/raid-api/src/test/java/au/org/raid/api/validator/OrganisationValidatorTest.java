package au.org.raid.api.validator;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.OrganisationRole;
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

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationValidatorTest {
    @Mock
    private OrganisationRoleValidator roleValidationService;

    @Mock
    private RorClient rorClient;

    @InjectMocks
    private OrganisationValidator validationService;

    @Test
    @DisplayName("Validation passes with valid organisation")
    void validOrganisation() {
        final var role = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(TestConstants.HTTPS_ROR_ORG)
                .role(List.of(role));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, empty());
        verify(roleValidationService).validate(role, 0, 0);
    }

    @Test
    @DisplayName("Validation fails with duplicate organisations")
    void duplicateOrganisation() {
        final var role = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(TestConstants.HTTPS_ROR_ORG)
                .role(List.of(role));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation, organisation));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation")
                        .errorType("duplicateValue")
                        .message("An organisation can appear only once. There are 2 occurrences of https://ror.org/038sjwq14")
        ));
        verify(roleValidationService).validate(role, 0, 0);
    }

    @Test
    @DisplayName("Validation fails with missing schemaUri")
    void missingIdentifierSchemeUri() {
        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .role(List.of(
                        new OrganisationRole()
                                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                ));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptyIdentifierSchemeUri() {
        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri("")
                .role(List.of(
                        new OrganisationRole()
                                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                ));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Role validation failures are returned")
    void roleValidationFailuresReturned() {
        final var role = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(TestConstants.HTTPS_ROR_ORG)
                .role(List.of(role));

        final var roleError = new ValidationFailure()
                .fieldId("organisation[0].role[0].id")
                .errorType(NOT_SET_TYPE)
                .message(NOT_SET_MESSAGE);

        when(roleValidationService.validate(role, 0, 0))
                .thenReturn(List.of(roleError));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(roleError));

        verify(roleValidationService).validate(role, 0, 0);
    }

    @Test
    @DisplayName("Validation fails with non-existent ROR")
    void existsReturnsFalse() {
        final var role = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(TestConstants.HTTPS_ROR_ORG)
                .role(List.of(role));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(false);

        final var failures = validationService.validate(List.of(organisation));

        final var roleError = new ValidationFailure()
                .fieldId("organisation[0].id")
                .errorType(NOT_FOUND_TYPE)
                .message("This ROR does not exist");


        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(roleError));

        verify(roleValidationService).validate(role, 0, 0);
    }

    @Test
    @DisplayName("Validation fails with invalid schemaUri (not ROR)")
    void invalidSchemaUri() {
        final var role = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri("https://invalid-schema.org/")
                .role(List.of(role));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[0].schemaUri")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_VALUE_MESSAGE)
        ));
    }

    @Test
    @DisplayName("Validation fails when organisation roles have overlapping dates")
    void overlappingOrganisationRoles() {
        final var role1 = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2023-01-01")
                .endDate("2023-12-31");

        final var role2 = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2023-06-01")
                .endDate("2024-06-30");

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(TestConstants.HTTPS_ROR_ORG)
                .role(List.of(role1, role2));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[0].role")
                        .errorType(INVALID_VALUE_TYPE)
                        .message("This contributor has simultaneous roles.")
        ));
    }

    @Test
    @DisplayName("Validation passes when organisation roles have non-overlapping dates")
    void nonOverlappingOrganisationRoles() {
        final var role1 = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2022-01-01")
                .endDate("2022-12-31");

        final var role2 = new OrganisationRole()
                .schemaUri(TestConstants.ORGANISATION_ROLE_SCHEMA_URI)
                .id(TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2023-01-01")
                .endDate("2023-12-31");

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(TestConstants.HTTPS_ROR_ORG)
                .role(List.of(role1, role2));

        when(rorClient.exists(TestConstants.VALID_ROR)).thenReturn(true);

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, empty());
        verify(roleValidationService).validate(role1, 0, 0);
        verify(roleValidationService).validate(role2, 0, 1);
    }

}