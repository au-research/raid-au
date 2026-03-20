package au.org.raid.api.service;

import au.org.raid.api.exception.OrganisationRoleNotFoundException;
import au.org.raid.api.exception.OrganisationRoleSchemaNotFoundException;
import au.org.raid.api.factory.OrganisationRoleFactory;
import au.org.raid.api.factory.record.RaidOrganisationRoleRecordFactory;
import au.org.raid.api.repository.OrganisationRoleRepository;
import au.org.raid.api.repository.OrganisationRoleSchemaRepository;
import au.org.raid.api.repository.RaidOrganisationRoleRepository;
import au.org.raid.db.jooq.tables.records.OrganisationRoleRecord;
import au.org.raid.db.jooq.tables.records.OrganisationRoleSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidOrganisationRoleRecord;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.OrganizationRoleIdEnum;
import au.org.raid.idl.raidv2.model.OrganizationRoleSchemaUriEnum;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganisationRoleServiceTest {
    @Mock
    private OrganisationRoleRepository organisationRoleRepository;
    @Mock
    private OrganisationRoleSchemaRepository organisationRoleSchemaRepository;
    @Mock
    private RaidOrganisationRoleRepository raidOrganisationRoleRepository;
    @Mock
    private RaidOrganisationRoleRecordFactory raidOrganisationRoleRecordFactory;
    @Mock
    private OrganisationRoleFactory organisationRoleFactory;
    @InjectMocks
    private OrganisationRoleService organisationRoleService;

    @Test
    @DisplayName("create() saves organisation")
    void createSavesOrganisation() {
        final var schemaUri = OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359.getValue();
        final var raidOrganisationId = 123;
        final var uri = OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182.getValue();
        final var id = 234;
        final var schemaId = 345;
        final var startDate = "2021";
        final var endDate = "2022";

        final var role = new OrganisationRole()
                .schemaUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359)
                .id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)
                .startDate(startDate)
                .endDate(endDate);

        final var organisationRoleSchemaRecord = new OrganisationRoleSchemaRecord()
                .setUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359.getValue())
                .setId(schemaId);

        final var organisationRoleRecord = new OrganisationRoleRecord()
                .setId(id);

        final var raidOrganisationRoleRecord = new RaidOrganisationRoleRecord();

        when(organisationRoleSchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.of(organisationRoleSchemaRecord));

        when(organisationRoleRepository.findByUriAndSchemaId(uri, schemaId))
                .thenReturn(Optional.of(organisationRoleRecord));

        when(raidOrganisationRoleRecordFactory.create(raidOrganisationId, id, startDate, endDate))
                .thenReturn(raidOrganisationRoleRecord);

        organisationRoleService.create(role, raidOrganisationId);

        verify(raidOrganisationRoleRepository).create(raidOrganisationRoleRecord);
    }

    @Test
    @DisplayName("create() throws OrganisationRoleSchemaNotFoundException")
    void createSavesOrganisationThrowsOrganisationRoleSchemaNotFoundException() {
        final var schemaUri = OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359;
        final var raidOrganisationId = 123;
        final var uri = "_id";
        final var startDate = "2021";
        final var endDate = "2022";

        final var role = new OrganisationRole()
                .schemaUri(schemaUri)
                .id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)
                .startDate(startDate)
                .endDate(endDate);

        when(organisationRoleSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.empty());

        assertThrows(OrganisationRoleSchemaNotFoundException.class,
                () -> organisationRoleService.create(role, raidOrganisationId));

        verifyNoInteractions(organisationRoleRepository);
        verifyNoInteractions(raidOrganisationRoleRecordFactory);
        verifyNoInteractions(raidOrganisationRoleRepository);
    }

    @Test
    @DisplayName("create() saves organisation throws OrganisationRoleNotFoundException")
    void createSavesOrganisationThrowsOrganisationRoleNotFoundException() {
        final var schemaUri = OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359;
        final var raidOrganisationId = 123;
        final var uri = OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182;
        final var schemaId = 345;
        final var startDate = "2021";
        final var endDate = "2022";

        final var role = new OrganisationRole()
                .schemaUri(schemaUri)
                .id(uri)
                .startDate(startDate)
                .endDate(endDate);

        final var organisationRoleSchemaRecord = new OrganisationRoleSchemaRecord()
                .setUri(schemaUri.getValue())
                .setId(schemaId);

        when(organisationRoleSchemaRepository.findByUri(schemaUri.getValue()))
                .thenReturn(Optional.of(organisationRoleSchemaRecord));

        when(organisationRoleRepository.findByUriAndSchemaId(uri.getValue(), schemaId))
                .thenReturn(Optional.empty());

        assertThrows(OrganisationRoleNotFoundException.class,
                () -> organisationRoleService.create(role, raidOrganisationId));

        verifyNoInteractions(raidOrganisationRoleRecordFactory);
        verifyNoInteractions(raidOrganisationRoleRepository);
    }

    @Test
    @DisplayName("findAllByRaidOrganisationId() returns all roles for a given raid organisation")
    void findAllByRaidOrganisationId() {
        final var raidOrganisationId = 123;
        final var organisationRoleId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var startDate = "2021";
        final var endDate = "2022";

        final var raidOrganisationRoleRecord = new RaidOrganisationRoleRecord()
                .setOrganisationRoleId(organisationRoleId)
                .setStartDate(startDate)
                .setEndDate(endDate);

        final var organisationRoleRecord = new OrganisationRoleRecord()
                .setSchemaId(schemaId)
                .setUri(uri);

        final var organisationRoleSchemaRecord = new OrganisationRoleSchemaRecord()
                .setUri(schemaUri);

        final var organisationRole = new OrganisationRole();

        when(raidOrganisationRoleRepository.findAllByRaidOrganisationId(raidOrganisationId))
                .thenReturn(List.of(raidOrganisationRoleRecord));

        when(organisationRoleRepository.findById(organisationRoleId)).thenReturn(Optional.of(organisationRoleRecord));

        when(organisationRoleSchemaRepository.findById(schemaId)).thenReturn(Optional.of(organisationRoleSchemaRecord));

        when(organisationRoleFactory.create(uri, schemaUri, startDate, endDate)).thenReturn(organisationRole);

        assertThat(organisationRoleService.findAllByRaidOrganisationId(raidOrganisationId),
                is(List.of(organisationRole)));
    }

    @Test
    @DisplayName("findAllByRaidOrganisationId() throws OrganisationRoleNotFoundException")
    void findAllByRaidOrganisationIdThrowsOrganisationRoleNotFoundException() {
        final var raidOrganisationId = 123;
        final var organisationRoleId = 234;
        final var startDate = "2021";
        final var endDate = "2022";

        final var raidOrganisationRoleRecord = new RaidOrganisationRoleRecord()
                .setOrganisationRoleId(organisationRoleId)
                .setStartDate(startDate)
                .setEndDate(endDate);

        when(raidOrganisationRoleRepository.findAllByRaidOrganisationId(raidOrganisationId))
                .thenReturn(List.of(raidOrganisationRoleRecord));

        when(organisationRoleRepository.findById(organisationRoleId)).thenReturn(Optional.empty());

        assertThrows(OrganisationRoleNotFoundException.class,
                () -> organisationRoleService.findAllByRaidOrganisationId(raidOrganisationId));

        verifyNoInteractions(organisationRoleSchemaRepository);
        verifyNoInteractions(organisationRoleFactory);
    }

    @Test
    @DisplayName("findAllByRaidOrganisationId() throws OrganisationRoleSchemaNotFoundException")
    void findAllByRaidOrganisationIdThrowsOrganisationRoleSchemaNotFoundException() {
        final var raidOrganisationId = 123;
        final var organisationRoleId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var startDate = "2021";
        final var endDate = "2022";

        final var raidOrganisationRoleRecord = new RaidOrganisationRoleRecord()
                .setOrganisationRoleId(organisationRoleId)
                .setStartDate(startDate)
                .setEndDate(endDate);

        final var organisationRoleRecord = new OrganisationRoleRecord()
                .setSchemaId(schemaId)
                .setUri(uri);

        when(raidOrganisationRoleRepository.findAllByRaidOrganisationId(raidOrganisationId))
                .thenReturn(List.of(raidOrganisationRoleRecord));

        when(organisationRoleRepository.findById(organisationRoleId)).thenReturn(Optional.of(organisationRoleRecord));

        when(organisationRoleSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(OrganisationRoleSchemaNotFoundException.class,
                () -> organisationRoleService.findAllByRaidOrganisationId(raidOrganisationId));

        verifyNoInteractions(organisationRoleFactory);
    }
}