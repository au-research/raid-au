package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorNotFoundException;
import au.org.raid.api.exception.ContributorSchemaNotFoundException;
import au.org.raid.api.factory.ContributorFactory;
import au.org.raid.api.factory.record.ContributorRecordFactory;
import au.org.raid.api.factory.record.RaidContributorRecordFactory;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorSchemaRepository;
import au.org.raid.api.repository.RaidContributorRepository;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.db.jooq.tables.records.ContributorSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidContributorRecord;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ContributorSchemaUriEnum;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorServiceTest {
    @Mock
    private ContributorRepository contributorRepository;
    @Mock
    private RaidContributorRepository raidContributorRepository;
    @Mock
    private ContributorRecordFactory contributorRecordFactory;
    @Mock
    private RaidContributorRecordFactory raidContributorRecordFactory;
    @Mock
    private ContributorPositionService contributorPositionService;
    @Mock
    private ContributorRoleService contributorRoleService;
    @Mock
    private ContributorFactory contributorFactory;
    @Mock
    private ContributorSchemaRepository contributorSchemaRepository;
    @InjectMocks
    private ContributorService contributorService;

    @Test
    @DisplayName("create() saves all contributors")
    void create() {
        final var handle = "_handle";
        final var schemaId = 123;
        final var schemaUri = ContributorSchemaUriEnum.HTTPS_ORCID_ORG_;
        final var id = 234;
        final var uri = "_uri";
        final var raidContributorId = 345;
        final var positions = List.of(new ContributorPosition());
        final var roles = List.of(new ContributorRole());

        final var contributor = new Contributor()
                .id(uri)
                .schemaUri(schemaUri)
                .position(positions)
                .role(roles);

        final var contributorSchemaRecord = new ContributorSchemaRecord()
                .setId(schemaId);

        final var contributorRecordForCreate = new ContributorRecord();
        final var savedContributorRecord = new ContributorRecord()
                .setId(id);
        final var raidContributorRecordForCreate = new RaidContributorRecord();
        final var savedRaidContributorRecord = new RaidContributorRecord()
                .setId(raidContributorId);

        when(contributorSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.of(contributorSchemaRecord));
        when(contributorRecordFactory.create(contributor, schemaId)).thenReturn(contributorRecordForCreate);
        when(contributorRepository.updateOrCreate(contributorRecordForCreate)).thenReturn(savedContributorRecord);

        when(raidContributorRecordFactory.create(contributor, id, handle)).thenReturn(raidContributorRecordForCreate);
        when(raidContributorRepository.create(raidContributorRecordForCreate)).thenReturn(savedRaidContributorRecord);

        contributorService.create(List.of(contributor), handle);

        verify(contributorPositionService).create(positions, raidContributorId);
        verify(contributorRoleService).create(roles, raidContributorId);
    }

    @Test
    @DisplayName("create() does not save when contributors is null")
    void createWithNullContributors() {
        final var handle = "_handle";

        try {
            contributorService.create(null, handle);
        } catch (Exception e) {
            fail("No exception expected");
        }

        verifyNoInteractions(contributorSchemaRepository);
        verifyNoInteractions(raidContributorRepository);
        verifyNoInteractions(raidContributorRecordFactory);
        verifyNoInteractions(contributorRepository);
        verifyNoInteractions(contributorRecordFactory);
        verifyNoInteractions(contributorPositionService);
        verifyNoInteractions(contributorRoleService);
    }

    @Test
    @DisplayName("create() throws ContributorSchemaNotFoundException")
    void createThrowsContributorSchemaNotFoundException() {
        final var handle = "_handle";
        final var schemaUri = ContributorSchemaUriEnum.HTTPS_ORCID_ORG_;
        final var uri = "_uri";
        final var positions = List.of(new ContributorPosition());
        final var roles = List.of(new ContributorRole());

        final var contributor = new Contributor()
                .id(uri)
                .schemaUri(schemaUri)
                .position(positions)
                .role(roles);

        when(contributorSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.empty());

        assertThrows(ContributorSchemaNotFoundException.class,
                () -> contributorService.create(List.of(contributor), handle));

        verifyNoInteractions(raidContributorRepository);
        verifyNoInteractions(raidContributorRecordFactory);
        verifyNoInteractions(contributorRepository);
        verifyNoInteractions(contributorRecordFactory);
        verifyNoInteractions(contributorPositionService);
        verifyNoInteractions(contributorRoleService);
    }

    @Test
    @DisplayName("findAllByHandle() returns all contributors for handle")
    void findAllByHandle() {
        final var handle = "_handle";
        final var contributorId = 123;
        final var uri = "_uri";
        final var schemaId = 234;
        final var raidContributorId = 345;
        final var schemaUri = "schema-uri";
        final var positions = List.of(new ContributorPosition());
        final var roles = List.of(new ContributorRole());
        final var leader = true;
        final var contact = true;

        final var raidContributorRecord = new RaidContributorRecord()
                .setContributorId(contributorId)
                .setId(raidContributorId)
                .setLeader(leader)
                .setContact(contact);

        final var contributorRecord = new ContributorRecord()
                .setSchemaId(schemaId)
                .setPid(uri);

        final var contributorSchemaRecord = new ContributorSchemaRecord()
                .setUri(schemaUri);

        final var contributor = new Contributor();

        when(raidContributorRepository.findAllByHandle(handle)).thenReturn(List.of(raidContributorRecord));
        when(contributorRepository.findById(contributorId)).thenReturn(Optional.of(contributorRecord));
        when(contributorSchemaRepository.findById(schemaId)).thenReturn(Optional.of(contributorSchemaRecord));
        when(contributorPositionService.findAllByRaidContributorId(raidContributorId)).thenReturn(positions);
        when(contributorRoleService.findAllByRaidContributorId(raidContributorId)).thenReturn(roles);
        when(contributorFactory.create(uri, schemaUri, leader, contact, positions, roles)).thenReturn(contributor);

        assertThat(contributorService.findAllByHandle(handle), is(List.of(contributor)));
    }

    @Test
    @DisplayName("findAllByHandle() returns empty list if none found")
    void findAllByHandleReturnsEmptyList() {
        final var handle = "_handle";

        when(raidContributorRepository.findAllByHandle(handle)).thenReturn(Collections.emptyList());

        assertThat(contributorService.findAllByHandle(handle), is(Collections.emptyList()));

        verifyNoInteractions(contributorRepository);
        verifyNoInteractions(contributorSchemaRepository);
        verifyNoInteractions(contributorPositionService);
        verifyNoInteractions(contributorRoleService);
        verifyNoInteractions(contributorFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws ContributorNotFoundException")
    void findAllByHandleThrowsContributorNotFoundException() {
        final var handle = "_handle";
        final var contributorId = 123;
        final var raidContributorId = 345;
        final var leader = true;
        final var contact = true;

        final var raidContributorRecord = new RaidContributorRecord()
                .setContributorId(contributorId)
                .setId(raidContributorId)
                .setLeader(leader)
                .setContact(contact);

        when(raidContributorRepository.findAllByHandle(handle)).thenReturn(List.of(raidContributorRecord));
        when(contributorRepository.findById(contributorId)).thenReturn(Optional.empty());

        assertThrows(ContributorNotFoundException.class, () -> contributorService.findAllByHandle(handle));

        verifyNoInteractions(contributorSchemaRepository);
        verifyNoInteractions(contributorPositionService);
        verifyNoInteractions(contributorRoleService);
        verifyNoInteractions(contributorFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws ContributorSchemaNotFoundException")
    void findAllByHandleThrowsContributorSchemaNotFoundException() {
        final var handle = "_handle";
        final var contributorId = 123;
        final var uri = "_uri";
        final var schemaId = 234;
        final var raidContributorId = 345;
        final var leader = true;
        final var contact = true;

        final var raidContributorRecord = new RaidContributorRecord()
                .setContributorId(contributorId)
                .setId(raidContributorId)
                .setLeader(leader)
                .setContact(contact);

        final var contributorRecord = new ContributorRecord()
                .setSchemaId(schemaId)
                .setPid(uri);

        when(raidContributorRepository.findAllByHandle(handle)).thenReturn(List.of(raidContributorRecord));
        when(contributorRepository.findById(contributorId)).thenReturn(Optional.of(contributorRecord));
        when(contributorSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(ContributorSchemaNotFoundException.class, () -> contributorService.findAllByHandle(handle));

        verifyNoInteractions(contributorPositionService);
        verifyNoInteractions(contributorRoleService);
        verifyNoInteractions(contributorFactory);
    }

    @Test
    @DisplayName("update() deletes existing contributors and re-inserts")
    void update() {
        final var handle = "_handle";
        final var schemaId = 123;
        final var schemaUri = ContributorSchemaUriEnum.HTTPS_ORCID_ORG_;
        final var id = 234;
        final var uri = "_uri";
        final var raidContributorId = 345;
        final var positions = List.of(new ContributorPosition());
        final var roles = List.of(new ContributorRole());

        final var contributor = new Contributor()
                .id(uri)
                .schemaUri(schemaUri)
                .position(positions)
                .role(roles);

        final var contributorSchemaRecord = new ContributorSchemaRecord()
                .setId(schemaId);

        final var contributorRecordForCreate = new ContributorRecord();
        final var savedContributorRecord = new ContributorRecord()
                .setId(id);
        final var raidContributorRecordForCreate = new RaidContributorRecord();
        final var savedRaidContributorRecord = new RaidContributorRecord()
                .setId(raidContributorId);

        when(contributorSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.of(contributorSchemaRecord));
        when(contributorRecordFactory.create(contributor, schemaId)).thenReturn(contributorRecordForCreate);
        when(contributorRepository.updateOrCreate(contributorRecordForCreate)).thenReturn(savedContributorRecord);

        when(raidContributorRecordFactory.create(contributor, id, handle)).thenReturn(raidContributorRecordForCreate);
        when(raidContributorRepository.create(raidContributorRecordForCreate)).thenReturn(savedRaidContributorRecord);

        contributorService.update(List.of(contributor), handle);

        verify(raidContributorRepository).deleteAllByHandle(handle);
        verify(contributorPositionService).create(positions, raidContributorId);
        verify(contributorRoleService).create(roles, raidContributorId);
    }
}