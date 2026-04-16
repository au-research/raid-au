package au.org.raid.api.service;

import au.org.raid.api.factory.HandleFactory;
import au.org.raid.api.factory.RaidRecordFactory;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static au.org.raid.api.util.TestRaid.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaidIngestServiceTest {
    @Mock
    TitleService titleService;
    @Mock
    DescriptionService descriptionService;
    @Mock
    ContributorService contributorService;
    @Mock
    OrganisationService organisationService;
    @Mock
    RelatedObjectService relatedObjectService;
    @Mock
    AlternateIdentifierService alternateIdentifierService;
    @Mock
    AlternateUrlService alternateUrlService;
    @Mock
    RelatedRaidService relatedRaidService;
    @Mock
    SubjectService subjectService;
    @Mock
    TraditionalKnowledgeLabelService traditionalKnowledgeLabelService;
    @Mock
    SpatialCoverageService spatialCoverageService;
    @Mock
    RaidRepository raidRepository;
    @Mock
    RaidRecordFactory raidRecordFactory;
    @Mock
    AccessService accessService;
    @Mock
    LanguageService languageService;
    @Mock
    HandleFactory handleFactory;
    @Mock
    CacheableRaidService cacheableRaidService;
    @Mock
    RaidHistoryService raidHistoryService;
    @Mock
    RaidDtoReadService raidDtoReadService;
    @InjectMocks
    RaidIngestService raidIngestService;

    @Test
    @DisplayName("create() saves raid and relations")
    void create() {
        final var handle = new Handle(RAID_DTO.getIdentifier().getId());
        final var accessTypeId = 123;
        final var accessStatementLanguageId = 234;
        final var registrationAgencyOrganisationId = 345;
        final var ownerOrganisationId = 456;

        final var raidRecord = new RaidRecord();

        when(handleFactory.create(RAID_DTO.getIdentifier().getId())).thenReturn(handle);
        when(accessService.findAccessTypeId(RAID_DTO.getAccess())).thenReturn(accessTypeId);
        when(languageService.findLanguageId(RAID_DTO.getAccess().getStatement().getLanguage()))
                .thenReturn(accessStatementLanguageId);
        when(organisationService.findOrCreate(REGISTRATION_AGENCY_ID, ROR_SCHEMA_URI))
                .thenReturn(registrationAgencyOrganisationId);
        when(organisationService.findOrCreate(OWNER_ID, ROR_SCHEMA_URI))
                .thenReturn(ownerOrganisationId);

        when(raidRecordFactory.create(RAID_DTO, accessTypeId, accessStatementLanguageId, registrationAgencyOrganisationId, ownerOrganisationId))
                .thenReturn(raidRecord);

        raidIngestService.create(RAID_DTO);

        verify(raidRepository).insert(raidRecord);
        verify(titleService).create(RAID_DTO.getTitle(), HANDLE);
        verify(descriptionService).create(RAID_DTO.getDescription(), HANDLE);
        verify(contributorService).create(RAID_DTO.getContributor(), HANDLE);
        verify(organisationService).create(RAID_DTO.getOrganisation(), HANDLE);
        verify(relatedObjectService).create(RAID_DTO.getRelatedObject(), HANDLE);
        verify(alternateIdentifierService).create(RAID_DTO.getAlternateIdentifier(), HANDLE);
        verify(alternateUrlService).create(RAID_DTO.getAlternateUrl(), HANDLE);
        verify(relatedRaidService).create(RAID_DTO.getRelatedRaid(), HANDLE);
        verify(subjectService).create(RAID_DTO.getSubject(), HANDLE);
        verify(spatialCoverageService).create(RAID_DTO.getSpatialCoverage(), HANDLE);
    }

    @Test
    @DisplayName("findAllByServicePointId() delegates resolution to RaidDtoReadService")
    void findAllByServicePointId() {
        final var servicePointId = 123L;
        final var raidRecord = new RaidRecord().setHandle(HANDLE);

        when(raidRepository.findAllByServicePointId(servicePointId)).thenReturn(List.of(raidRecord));
        when(raidDtoReadService.toRaidDto(raidRecord)).thenReturn(Optional.of(RAID_DTO));

        final var result = raidIngestService.findAllByServicePointId(servicePointId);

        assertThat(result, is(List.of(RAID_DTO)));
    }

    @Test
    @DisplayName("findAllByServicePointId() falls back to cacheableRaidService when RaidDtoReadService returns empty")
    void findAllByServicePointIdFallsBackToCacheableRaid() {
        final var servicePointId = 123L;
        final var raidRecord = new RaidRecord().setHandle(HANDLE);

        when(raidRepository.findAllByServicePointId(servicePointId)).thenReturn(List.of(raidRecord));
        when(raidDtoReadService.toRaidDto(raidRecord)).thenReturn(Optional.empty());
        when(cacheableRaidService.build(raidRecord)).thenReturn(RAID_DTO);

        final var result = raidIngestService.findAllByServicePointId(servicePointId);

        assertThat(result, is(List.of(RAID_DTO)));
    }

    @Test
    @DisplayName("findByHandle() returns raid from handle")
    void findByHandle() {
        when(raidHistoryService.findByHandle(HANDLE)).thenReturn(Optional.of(RAID_DTO));

        assertThat(raidIngestService.findByHandle(HANDLE), is(Optional.of(RAID_DTO)));
    }

    @Test
    @DisplayName("findByHandle() returns empty Optional if none found")
    void findByHandleReturnsEmptyOptional() {
        when(raidHistoryService.findByHandle(HANDLE)).thenReturn(Optional.empty());
        assertThat(raidIngestService.findByHandle(HANDLE), is(Optional.empty()));
    }

    @Test
    @DisplayName("update() updates raid and dependencies")
    void update() {
        final var accessTypeId = 123;
        final var accessStatementLanguageId = 234;
        final var registrationAgencyOrganisationId = 345;
        final var ownerOrganisationId = 345;

        final var handle = new Handle(RAID_DTO.getIdentifier().getId());
        final var raidRecord = new RaidRecord();

        when(handleFactory.create(RAID_DTO.getIdentifier().getId())).thenReturn(handle);

        when(raidRepository.findByHandle(HANDLE)).thenReturn(Optional.of(raidRecord));

        when(accessService.findAccessTypeId(ACCESS)).thenReturn(accessTypeId);
        when(languageService.findLanguageId(LANGUAGE)).thenReturn(accessStatementLanguageId);
        when(organisationService.findOrCreate(REGISTRATION_AGENCY_ID, ROR_SCHEMA_URI))
                .thenReturn(registrationAgencyOrganisationId);
        when(organisationService.findOrCreate(OWNER_ID, ROR_SCHEMA_URI))
                .thenReturn(ownerOrganisationId);

        when(raidRecordFactory.create(RAID_DTO, accessTypeId, accessStatementLanguageId,
                registrationAgencyOrganisationId, ownerOrganisationId)).thenReturn(raidRecord);

        assertThat(raidIngestService.update(RAID_DTO), is(RAID_DTO));

        verify(raidRepository).update(raidRecord);
        verify(titleService).update(TITLES, HANDLE);
        verify(descriptionService).update(DESCRIPTIONS, HANDLE);
        verify(contributorService).update(CONTRIBUTORS, HANDLE);
        verify(organisationService).update(ORGANISATIONS, HANDLE);
        verify(relatedObjectService).update(RELATED_OBJECTS, HANDLE);
        verify(alternateIdentifierService).update(ALTERNATE_IDENTIFIERS, HANDLE);
        verify(alternateUrlService).update(ALTERNATE_URLS, HANDLE);
        verify(relatedRaidService).update(RELATED_RAIDS, HANDLE);
        verify(subjectService).update(SUBJECTS, HANDLE);
        verify(spatialCoverageService).update(SPATIAL_COVERAGES, HANDLE);
    }

}
