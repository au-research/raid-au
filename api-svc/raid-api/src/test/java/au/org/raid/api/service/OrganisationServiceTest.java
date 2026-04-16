package au.org.raid.api.service;

import au.org.raid.api.exception.OrganisationNotFoundException;
import au.org.raid.api.exception.OrganisationSchemaNotFoundException;
import au.org.raid.api.factory.OrganisationFactory;
import au.org.raid.api.factory.record.OrganisationRecordFactory;
import au.org.raid.api.factory.record.RaidOrganisationRecordFactory;
import au.org.raid.api.repository.OrganisationRepository;
import au.org.raid.api.repository.OrganisationSchemaRepository;
import au.org.raid.api.repository.RaidOrganisationRepository;
import au.org.raid.db.jooq.tables.records.OrganisationRecord;
import au.org.raid.db.jooq.tables.records.OrganisationSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidOrganisationRecord;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private OrganisationSchemaRepository organisationSchemaRepository;
    @Mock
    private RaidOrganisationRepository raidOrganisationRepository;
    @Mock
    private OrganisationRecordFactory organisationRecordFactory;
    @Mock
    private RaidOrganisationRecordFactory raidOrganisationRecordFactory;
    @Mock
    private OrganisationRoleService organisationRoleService;
    @Mock
    private OrganisationFactory organisationFactory;
    @InjectMocks
    private OrganisationService organisationService;
    @Test
    @DisplayName("create() saves organisation")
    void create() {

        final var handle = "_handle";
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var id = 234;
        final var raidOrganisationId = 345;

        final var role = new OrganisationRole();
        final var organisation = new Organisation()
                .role(List.of(role))
                .schemaUri(schemaUri);

        final var organisationSchemaRecord = new OrganisationSchemaRecord()
                .setId(schemaId);

        final var organisationRecord = new OrganisationRecord();
        final var savedOrganisation = new OrganisationRecord()
                .setId(id);

        final var raidOrganisationRecord = new RaidOrganisationRecord();
        final var savedRaidOrganisationRecord = new RaidOrganisationRecord()
                .setId(raidOrganisationId);

        when(organisationSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(organisationSchemaRecord));
        when(organisationRecordFactory.create(organisation, schemaId)).thenReturn(organisationRecord);
        when(organisationRepository.findOrCreate(organisationRecord)).thenReturn(savedOrganisation);
        when(raidOrganisationRecordFactory.create(id, handle)).thenReturn(raidOrganisationRecord);
        when(raidOrganisationRepository.create(raidOrganisationRecord)).thenReturn(savedRaidOrganisationRecord);

        organisationService.create(List.of(organisation), handle);

        verify(organisationRoleService).create(role, raidOrganisationId);
    }

    @Test
    @DisplayName("create() throws OrganisationSchemaNotFoundException")
    void createThrowsOrganisationSchemaNotFoundException() {
        final var handle = "_handle";
        final var schemaUri = "schema-uri";

        final var role = new OrganisationRole();
        final var organisation = new Organisation()
                .role(List.of(role))
                .schemaUri(schemaUri);

        when(organisationSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.empty());

        assertThrows(OrganisationSchemaNotFoundException.class,
                () -> organisationService.create(List.of(organisation), handle));

        verifyNoInteractions(organisationRecordFactory);
        verifyNoInteractions(organisationRepository);
        verifyNoInteractions(raidOrganisationRecordFactory);
        verifyNoInteractions(raidOrganisationRepository);
        verifyNoInteractions(organisationRoleService);
    }

    @Test
    @DisplayName("findOrCreate() finds or creates organisation")
    void findOrCreate() {
        final var pid = "_pid";
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var id = 234;

        final var organisationSchemaRecord = new OrganisationSchemaRecord()
                .setId(schemaId);

        final var organisationRecord = new OrganisationRecord();
        final var savedOrganisationRecord = new OrganisationRecord()
                .setId(id);

        when(organisationSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(organisationSchemaRecord));
        when(organisationRecordFactory.create(pid, schemaId)).thenReturn(organisationRecord);
        when(organisationRepository.findOrCreate(organisationRecord)).thenReturn(savedOrganisationRecord);

        final var result = organisationService.findOrCreate(pid, schemaUri);

        assertThat(result, is(id));
    }

    @Test
    @DisplayName("findOrCreate() throws OrganisationSchemaNotFoundException")
    void findOrCreateThrowsOrganisationSchemaNotFoundException() {
        final var pid = "_pid";
        final var schemaUri = "schema-uri";

        when(organisationSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.empty());

        assertThrows(OrganisationSchemaNotFoundException.class, () -> organisationService.findOrCreate(pid, schemaUri));

        verifyNoInteractions(organisationRecordFactory);
        verifyNoInteractions(organisationRepository);
    }

    @Test
    @DisplayName("findAllByHandle() returns all organisations for a given handle")
    void findAllByHandle() {
        final var handle = "_handle";
        final var organisationId = 123;
        final var schemaId = 234;
        final var schemaUri = "schema-uri";
        final var raidOrganisationId = 456;
        final var pid = "_pid";
        final var roles = List.of(new OrganisationRole());

        final var raidOrganisationRecord = new RaidOrganisationRecord()
                .setId(raidOrganisationId)
                .setOrganisationId(organisationId);

        final var organisationRecord = new OrganisationRecord()
                .setSchemaId(schemaId)
                .setPid(pid);

        final var organisationSchemaRecord = new OrganisationSchemaRecord()
                .setId(organisationId)
                .setUri(schemaUri);

        final var organisation = new Organisation();

        when(raidOrganisationRepository.findAllByHandle(handle)).thenReturn(List.of(raidOrganisationRecord));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisationRecord));
        when(organisationSchemaRepository.findById(schemaId)).thenReturn(Optional.of(organisationSchemaRecord));
        when(organisationRoleService.findAllByRaidOrganisationId(raidOrganisationId)).thenReturn(roles);
        when(organisationFactory.create(pid, schemaUri, roles)).thenReturn(organisation);

        final var result = organisationService.findAllByHandle(handle);

        assertThat(result, is(List.of(organisation)));
    }

    @Test
    @DisplayName("findAllByHandle() throws OrganisationNotFoundException")
    void findAllByHandleThrowsOrganisationNotFoundException() {
        final var handle = "_handle";
        final var organisationId = 123;
        final var raidOrganisationId = 456;

        final var raidOrganisationRecord = new RaidOrganisationRecord()
                .setId(raidOrganisationId)
                .setOrganisationId(organisationId);

        when(raidOrganisationRepository.findAllByHandle(handle)).thenReturn(List.of(raidOrganisationRecord));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.empty());

        assertThrows(OrganisationNotFoundException.class, () -> organisationService.findAllByHandle(handle));

        verifyNoInteractions(organisationSchemaRepository);
        verifyNoInteractions(organisationRoleService);
        verifyNoInteractions(organisationFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws OrganisationSchemaNotFoundException")
    void findAllByHandleThrowsOrganisationSchemaNotFoundException() {
        final var handle = "_handle";
        final var organisationId = 123;
        final var schemaId = 234;
        final var schemaUri = "schema-uri";
        final var raidOrganisationId = 456;
        final var pid = "_pid";
        final var roles = List.of(new OrganisationRole());

        final var raidOrganisationRecord = new RaidOrganisationRecord()
                .setId(raidOrganisationId)
                .setOrganisationId(organisationId);

        final var organisationRecord = new OrganisationRecord()
                .setSchemaId(schemaId)
                .setPid(pid);

        final var organisation = new Organisation();

        when(raidOrganisationRepository.findAllByHandle(handle)).thenReturn(List.of(raidOrganisationRecord));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisationRecord));
        when(organisationSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(OrganisationSchemaNotFoundException.class, () -> organisationService.findAllByHandle(handle));

        verifyNoInteractions(organisationRoleService);
        verifyNoInteractions(organisationFactory);
    }

    @Test
    @DisplayName("findOrganisationUri returns pid of organisation")
    void findOrganisationUri() {
        final var id = 123;
        final var pid = "_pid";

        final var organisationRecord = new OrganisationRecord()
                .setPid(pid);

        when(organisationRepository.findById(id)).thenReturn(Optional.of(organisationRecord));

        assertThat(organisationService.findOrganisationUri(id), is(pid));
    }

    @Test
    @DisplayName("findOrganisationUri throws OrganisationNotFoundException")
    void findOrganisationUriThrowsOrganisationNotFoundException() {
        final var id = 123;

        when(organisationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganisationNotFoundException.class, () -> organisationService.findOrganisationUri(id));
    }

    @Test
    @DisplayName("findOrganisationSchemaUri() returns schema uri for organisation")
    void findOrganisationSchemaUri() {
        final var id = 123;
        final var schemaId = 234;
        final var schemaUri = "schema-uri";

        final var organisationRecord = new OrganisationRecord()
                .setSchemaId(schemaId);

        final var organisationSchemaRecord = new OrganisationSchemaRecord()
                .setUri(schemaUri);

        when(organisationRepository.findById(id)).thenReturn(Optional.of(organisationRecord));
        when(organisationSchemaRepository.findById(schemaId)).thenReturn(Optional.of(organisationSchemaRecord));

        assertThat(organisationService.findOrganisationSchemaUri(id), is(schemaUri));
    }

    @Test
    @DisplayName("findOrganisationSchemaUri() throws OrganisationNotFoundException")
    void findOrganisationSchemaUriThrowsOrganisationNotFoundException() {
        final var id = 123;

        when(organisationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrganisationNotFoundException.class, () -> organisationService.findOrganisationSchemaUri(id));

        verifyNoInteractions(organisationSchemaRepository);
    }

    @Test
    @DisplayName("findOrganisationSchemaUri() throws OrganisationSchemaNotFoundException")
    void findOrganisationSchemaUriThrowsOrganisationSchemaNotFoundException() {
        final var id = 123;
        final var schemaId = 234;

        final var organisationRecord = new OrganisationRecord()
                .setSchemaId(schemaId);

        when(organisationRepository.findById(id)).thenReturn(Optional.of(organisationRecord));
        when(organisationSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(OrganisationSchemaNotFoundException.class,
                () ->  organisationService.findOrganisationSchemaUri(id));
    }

    @Test
    @Disabled
    @DisplayName("update() deletes snd re-inserts organisation and descendants")
    void update() {
        final var handle = "_handle";
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var id = 234;
        final var raidOrganisationId = 345;

        final var role = new OrganisationRole();
        final var organisation = new Organisation()
                .role(List.of(role))
                .schemaUri(schemaUri);

        final var organisationSchemaRecord = new OrganisationSchemaRecord()
                .setId(schemaId);

        final var organisationRecord = new OrganisationRecord();
        final var savedOrganisation = new OrganisationRecord()
                .setId(id);

        final var raidOrganisationRecord = new RaidOrganisationRecord();
        final var savedRaidOrganisationRecord = new RaidOrganisationRecord()
                .setId(raidOrganisationId);

        when(organisationSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(organisationSchemaRecord));
        when(organisationRecordFactory.create(organisation, schemaId)).thenReturn(organisationRecord);
        when(organisationRepository.findOrCreate(organisationRecord)).thenReturn(savedOrganisation);
        when(raidOrganisationRecordFactory.create(id, handle)).thenReturn(raidOrganisationRecord);
        when(raidOrganisationRepository.create(raidOrganisationRecord)).thenReturn(savedRaidOrganisationRecord);

        organisationService.update(List.of(organisation), handle);

        verify(raidOrganisationRepository).deleteAllByHandle(handle);
        verify(organisationRoleService).create(role, raidOrganisationId);
    }
}