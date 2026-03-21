package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorRoleRepository;
import au.org.raid.api.repository.ContributorRoleSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.ContributorRoleRecord;
import au.org.raid.db.jooq.tables.records.ContributorRoleSchemaRecord;
import au.org.raid.idl.raidv2.model.ContributorRole;
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
class ContributorRoleValidatorTest {
    private static final int CONTRIBUTOR_ROLE_TYPE_SCHEMA_ID = 1;

    private static final ContributorRoleSchemaRecord CONTRIBUTOR_ROLE_TYPE_SCHEMA_RECORD =
            new ContributorRoleSchemaRecord()
                    .setId(CONTRIBUTOR_ROLE_TYPE_SCHEMA_ID)
                    .setUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI);

    private static final ContributorRoleRecord CONTRIBUTOR_ROLE_TYPE_RECORD =
            new ContributorRoleRecord()
                    .setSchemaId(CONTRIBUTOR_ROLE_TYPE_SCHEMA_ID)
                    .setUri(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

    @Mock
    private ContributorRoleSchemaRepository contributorRoleSchemaRepository;

    @Mock
    private ContributorRoleRepository contributorRoleRepository;

    @InjectMocks
    private ContributorRoleValidator validationService;

    @Test
    @DisplayName("Validation passes with valid ContributorRole")
    void validContributorRole() {
        final var role = new ContributorRole()
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE)
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI);

        when(contributorRoleSchemaRepository.findActiveByUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_ROLE_TYPE_SCHEMA_RECORD));

        when(contributorRoleRepository
                .findByUriAndSchemaId(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE, CONTRIBUTOR_ROLE_TYPE_SCHEMA_ID))
                .thenReturn(Optional.of(CONTRIBUTOR_ROLE_TYPE_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullSchemaUri() {
        final var role = new ContributorRole()
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].role[3].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with empty schemaUri")
    void emptySchemaUri() {
        final var role = new ContributorRole()
                .schemaUri("")
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].role[3].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with invalid schemaUri")
    void invalidSchemaUri() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        when(contributorRoleSchemaRepository.findActiveByUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].role[3].schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));

        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with null role")
    void nullRole() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI);

        when(contributorRoleSchemaRepository.findActiveByUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_ROLE_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].role[3].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with empty role")
    void emptyRole() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id("");

        when(contributorRoleSchemaRepository.findActiveByUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_ROLE_TYPE_SCHEMA_RECORD));

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].role[3].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));

        verifyNoInteractions(contributorRoleRepository);
    }

    @Test
    @DisplayName("Validation fails with invalid role")
    void invalidRole() {
        final var role = new ContributorRole()
                .schemaUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI)
                .id(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE);

        when(contributorRoleSchemaRepository.findActiveByUri(TestConstants.CONTRIBUTOR_ROLE_SCHEMA_URI))
                .thenReturn(Optional.of(CONTRIBUTOR_ROLE_TYPE_SCHEMA_RECORD));

        when(contributorRoleRepository
                .findByUriAndSchemaId(TestConstants.SUPERVISION_CONTRIBUTOR_ROLE, CONTRIBUTOR_ROLE_TYPE_SCHEMA_ID))
                .thenReturn(Optional.empty());

        final var failures = validationService.validate(role, 2, 3);

        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("contributor[2].role[3].id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        ));
    }
}