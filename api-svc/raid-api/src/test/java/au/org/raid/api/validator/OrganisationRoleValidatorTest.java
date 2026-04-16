package au.org.raid.api.validator;

import au.org.raid.api.repository.OrganisationRoleRepository;
import au.org.raid.api.repository.OrganisationRoleSchemaRepository;
import au.org.raid.db.jooq.tables.records.OrganisationRoleRecord;
import au.org.raid.db.jooq.tables.records.OrganisationRoleSchemaRecord;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static au.org.raid.api.util.TestConstants.LEAD_RESEARCH_ORGANISATION_ROLE;
import static au.org.raid.api.util.TestConstants.ORGANISATION_ROLE_SCHEMA_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationRoleValidatorTest {
    private static final int ORGANISATION_ROLE_SCHEMA_ID = 1;

    private static final OrganisationRoleSchemaRecord ORGANISATION_ROLE_SCHEMA_RECORD =
            new OrganisationRoleSchemaRecord()
                    .setId(ORGANISATION_ROLE_SCHEMA_ID)
                    .setUri(ORGANISATION_ROLE_SCHEMA_URI);

    private static final OrganisationRoleRecord ORGANISATION_ROLE_RECORD =
            new OrganisationRoleRecord()
                    .setSchemaId(ORGANISATION_ROLE_SCHEMA_ID)
                    .setUri(LEAD_RESEARCH_ORGANISATION_ROLE);

    @Mock
    private OrganisationRoleSchemaRepository contributorRoleSchemaRepository;

    @Mock
    private OrganisationRoleRepository contributorRoleRepository;

    @InjectMocks
    private OrganisationRoleValidator validationService;

    @Test
    @DisplayName("Validation passes with valid OrganisationRole")
    void validOrganisationRole() {
        final var role = new OrganisationRole()
                .id(LEAD_RESEARCH_ORGANISATION_ROLE)
                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                .startDate("2021");

        when(contributorRoleSchemaRepository.findActiveByUri(ORGANISATION_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(ORGANISATION_ROLE_SCHEMA_RECORD));

        when(contributorRoleRepository
                .findByUriAndSchemaId(LEAD_RESEARCH_ORGANISATION_ROLE, ORGANISATION_ROLE_SCHEMA_ID))
                .thenReturn(Optional.of(ORGANISATION_ROLE_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails if end date is before start date")
    void endDateBeforeStartDate() {
        final var role = new OrganisationRole()
                .id(LEAD_RESEARCH_ORGANISATION_ROLE)
                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                .startDate("2021")
                .endDate("2020");

        when(contributorRoleSchemaRepository.findActiveByUri(ORGANISATION_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(ORGANISATION_ROLE_SCHEMA_RECORD));

        when(contributorRoleRepository
                .findByUriAndSchemaId(LEAD_RESEARCH_ORGANISATION_ROLE, ORGANISATION_ROLE_SCHEMA_ID))
                .thenReturn(Optional.of(ORGANISATION_ROLE_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].endDate")
                        .errorType("invalidValue")
                        .message("end date is before start date")
        )));
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullSchemaUri() {
        final var role = new OrganisationRole()
                .id(LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2021");

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptySchemaUri() {
        final var role = new OrganisationRole()
                .schemaUri("")
                .id(LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2021");

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with invalid schemaUri")
    void invalidSchemaUri() {
        final var role = new OrganisationRole()
                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                .id(LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2021");

        when(contributorRoleSchemaRepository.findActiveByUri(ORGANISATION_ROLE_SCHEMA_URI))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));

        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with null role")
    void nullRole() {
        final var role = new OrganisationRole()
                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                .startDate("2021");

        when(contributorRoleSchemaRepository.findActiveByUri(ORGANISATION_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(ORGANISATION_ROLE_SCHEMA_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with empty role type id")
    void emptyRole() {
        final var role = new OrganisationRole()
                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                .id("")
                .startDate("2021");

        when(contributorRoleSchemaRepository.findActiveByUri(ORGANISATION_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(ORGANISATION_ROLE_SCHEMA_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with invalid role")
    void invalidRole() {
        final var role = new OrganisationRole()
                .schemaUri(ORGANISATION_ROLE_SCHEMA_URI)
                .id(LEAD_RESEARCH_ORGANISATION_ROLE)
                .startDate("2021");

        when(contributorRoleSchemaRepository.findActiveByUri(ORGANISATION_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(ORGANISATION_ROLE_SCHEMA_RECORD));

        when(contributorRoleRepository
                .findByUriAndSchemaId(LEAD_RESEARCH_ORGANISATION_ROLE, ORGANISATION_ROLE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("organisation[2].role[3].id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}