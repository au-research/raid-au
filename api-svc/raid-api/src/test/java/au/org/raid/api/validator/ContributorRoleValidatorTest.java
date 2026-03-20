package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorRoleRepository;
import au.org.raid.api.repository.ContributorRoleSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.ContributorRoleRecord;
import au.org.raid.db.jooq.tables.records.ContributorRoleSchemaRecord;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ContributorRoleIdEnum;
import au.org.raid.idl.raidv2.model.ContributorRoleSchemaUriEnum;
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




}