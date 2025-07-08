package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorRoleNotFoundException;
import au.org.raid.api.exception.ContributorRoleSchemaNotFoundException;
import au.org.raid.api.factory.ContributorRoleFactory;
import au.org.raid.api.factory.record.RaidContributorRoleRecordFactory;
import au.org.raid.api.repository.ContributorRoleRepository;
import au.org.raid.api.repository.ContributorRoleSchemaRepository;
import au.org.raid.api.repository.RaidContributorRoleRepository;
import au.org.raid.db.jooq.tables.records.ContributorRoleRecord;
import au.org.raid.db.jooq.tables.records.ContributorRoleSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidContributorRoleRecord;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ContributorRoleIdEnum;
import au.org.raid.idl.raidv2.model.ContributorRoleSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ContributorRoleServiceTest {
    @Mock
    private RaidContributorRoleRepository raidContributorRoleRepository;
    @Mock
    private RaidContributorRoleRecordFactory raidContributorRoleRecordFactory;
    @Mock
    private ContributorRoleFactory contributorRoleFactory;
    @Mock
    private ContributorRoleRepository contributorRoleRepository;
    @Mock
    private ContributorRoleSchemaRepository contributorRoleSchemaRepository;
    @InjectMocks
    private ContributorRoleService contributorRoleService;

    @Test
    @DisplayName("findAllByRaidContributorId() returns all contributor roles")
    void findAllByRaidContributorId() {
        final var raidContributorId = 123;
        final var contributorRoleId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var contributorRoleRecord = new ContributorRoleRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        final var raidContributorRoleRecord = new RaidContributorRoleRecord()
                .setContributorRoleId(contributorRoleId);

        final var contributorRoleSchemaRecord = new ContributorRoleSchemaRecord()
                .setUri(schemaUri);

        final var contributorRole = new ContributorRole();

        when(raidContributorRoleRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(List.of(raidContributorRoleRecord));

        when(contributorRoleRepository.findById(contributorRoleId))
                .thenReturn(Optional.of(contributorRoleRecord));

        when(contributorRoleSchemaRepository.findById(schemaId))
                .thenReturn(Optional.of(contributorRoleSchemaRecord));

        when(contributorRoleFactory.create(uri, schemaUri))
                .thenReturn(contributorRole);

        assertThat(contributorRoleService.findAllByRaidContributorId(raidContributorId),
                is(List.of(contributorRole)));
    }

    @Test
    @DisplayName("findAllByRaidContributorId() returns empty list when no roles")
    void findAllByRaidContributorIdReturnsEmptyList() {
        final var raidContributorId = 123;

        when(raidContributorRoleRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(Collections.emptyList());

        assertThat(contributorRoleService.findAllByRaidContributorId(raidContributorId),
                is(Collections.emptyList()));

        verifyNoInteractions(contributorRoleRepository);
        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleFactory);
    }


    @Test
    @DisplayName("findAllByRaidContributorId() throws ContributorRoleNotFoundException")
    void findAllByRaidContributorIdThrowsContributorRoleNotFoundException() {
        final var raidContributorId = 123;
        final var contributorRoleId = 234;

        final var raidContributorRoleRecord = new RaidContributorRoleRecord()
                .setContributorRoleId(contributorRoleId);

        when(raidContributorRoleRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(List.of(raidContributorRoleRecord));

        when(contributorRoleRepository.findById(contributorRoleId))
                .thenReturn(Optional.empty());

        assertThrows(ContributorRoleNotFoundException.class,
                () -> contributorRoleService.findAllByRaidContributorId(raidContributorId));

        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleFactory);
    }

    @Test
    @DisplayName("findAllByRaidContributorId() throws ContributorRoleSchemaNotFoundException")
    void findAllByRaidContributorIdThrowsContributorRoleSchemaNotFoundException() {
        final var raidContributorId = 123;
        final var contributorRoleId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var contributorRoleRecord = new ContributorRoleRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        final var raidContributorRoleRecord = new RaidContributorRoleRecord()
                .setContributorRoleId(contributorRoleId);

        when(raidContributorRoleRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(List.of(raidContributorRoleRecord));

        when(contributorRoleRepository.findById(contributorRoleId))
                .thenReturn(Optional.of(contributorRoleRecord));

        when(contributorRoleSchemaRepository.findById(schemaId))
                .thenReturn(Optional.empty());

        assertThrows(ContributorRoleSchemaNotFoundException.class,
                () -> contributorRoleService.findAllByRaidContributorId(raidContributorId));

        verifyNoInteractions(contributorRoleFactory);
    }


    @Test
    @DisplayName("create() saves all contributor roles")
    void create() {
        final var raidContributorId = 123;
        final var uri = ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_CONCEPTUALIZATION_;
        final var schemaUri = ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_;
        final var schemaId = 234;
        final var contributorRole = new ContributorRole()
                .id(uri)
                .schemaUri(schemaUri);
        final var contributorRoleId = 345;

        final var contributorRoleSchemaRecord = new ContributorRoleSchemaRecord()
                .setId(schemaId);

        final var contributorRoleRecord = new ContributorRoleRecord()
                .setId(contributorRoleId);

        final var raidContributorRoleRecord = new RaidContributorRoleRecord();

        when(contributorRoleSchemaRepository.findByUri(schemaUri.getValue()))
                .thenReturn(Optional.of(contributorRoleSchemaRecord));

        when(contributorRoleRepository.findByUriAndSchemaId(uri.getValue(), schemaId))
                .thenReturn(Optional.of(contributorRoleRecord));

        when(raidContributorRoleRecordFactory.create(raidContributorId, contributorRoleId))
                .thenReturn(raidContributorRoleRecord);

        contributorRoleService.create(List.of(contributorRole), raidContributorId);

        verify(raidContributorRoleRepository).create(raidContributorRoleRecord);
    }

    @Test
    @DisplayName("create() with null contributor roles does not save")
    void createWithNullContributorRoles() {
        final var raidContributorId = 123;

        contributorRoleService.create(null, raidContributorId);

        verifyNoInteractions(contributorRoleSchemaRepository);
        verifyNoInteractions(contributorRoleRepository);
        verifyNoInteractions(raidContributorRoleRecordFactory);
        verifyNoInteractions(raidContributorRoleRepository);
    }

    @Test
    @DisplayName("create() throws ContributorRoleSchemaNotFoundException")
    void createThrowsContributorRoleSchemaNotFoundException() {
        final var raidContributorId = 123;
        final var schemaUri = ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_;
        final var contributorRole = new ContributorRole()
                .schemaUri(schemaUri);

        when(contributorRoleSchemaRepository.findByUri(schemaUri.getValue()))
                .thenReturn(Optional.empty());

        assertThrows(ContributorRoleSchemaNotFoundException.class, () ->
                contributorRoleService.create(List.of(contributorRole), raidContributorId));

        verifyNoInteractions(contributorRoleRepository);
        verifyNoInteractions(raidContributorRoleRecordFactory);
        verifyNoInteractions(raidContributorRoleRepository);
    }

    @Test
    @DisplayName("create() throws ContributorRoleNotFoundException")
    void createThrowsContributorRoleNotFoundException() {
        final var raidContributorId = 123;
        final var uri = ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_CONCEPTUALIZATION_;
        final var schemaUri = ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_;
        final var schemaId = 234;
        final var contributorRole = new ContributorRole()
                .id(uri)
                .schemaUri(schemaUri);

        final var contributorRoleSchemaRecord = new ContributorRoleSchemaRecord()
                .setId(schemaId);

        when(contributorRoleSchemaRepository.findByUri(schemaUri.getValue()))
                .thenReturn(Optional.of(contributorRoleSchemaRecord));

        when(contributorRoleRepository.findByUriAndSchemaId(uri.getValue(), schemaId))
                .thenReturn(Optional.empty());

        assertThrows(ContributorRoleNotFoundException.class,
                () -> contributorRoleService.create(List.of(contributorRole), raidContributorId));

        verifyNoInteractions(raidContributorRoleRecordFactory);
        verifyNoInteractions(raidContributorRoleRepository);
    }
    

}