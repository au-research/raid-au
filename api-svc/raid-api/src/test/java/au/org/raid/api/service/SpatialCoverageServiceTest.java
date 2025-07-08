package au.org.raid.api.service;

import au.org.raid.api.exception.SpatialCoverageSchemaNotFoundException;
import au.org.raid.api.factory.SpatialCoverageFactory;
import au.org.raid.api.factory.record.RaidSpatialCoverageRecordFactory;
import au.org.raid.api.repository.RaidSpatialCoverageRepository;
import au.org.raid.api.repository.SpatialCoverageSchemaRepository;
import au.org.raid.db.jooq.tables.records.RaidSpatialCoverageRecord;
import au.org.raid.db.jooq.tables.records.SpatialCoverageSchemaRecord;
import au.org.raid.idl.raidv2.model.SpatialCoverage;
import au.org.raid.idl.raidv2.model.SpatialCoveragePlace;
import au.org.raid.idl.raidv2.model.SpatialCoverageSchemaUriEnum;
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

@ExtendWith(MockitoExtension.class)
class SpatialCoverageServiceTest {
    @Mock
    private SpatialCoverageSchemaRepository spatialCoverageSchemaRepository;
    @Mock
    private RaidSpatialCoverageRepository raidSpatialCoverageRepository;
    @Mock
    private RaidSpatialCoverageRecordFactory raidSpatialCoverageRecordFactory;
    @Mock
    private RaidSpatialCoveragePlaceService raidSpatialCoveragePlaceService;
    @Mock
    private SpatialCoverageFactory spatialCoverageFactory;
    @InjectMocks
    private SpatialCoverageService spatialCoverageService;

    @Test
    @DisplayName("create() saves new spatial coverage")
    void create() {
        final var handle = "_handle";
        final var schemaUri = SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_.getValue();
        final var schemaId = 123;
        final var uri = "_uri";
        final var id = 234;
        final var place = new SpatialCoveragePlace();

        final var spatialCoverage = new SpatialCoverage()
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)
                .id(uri)
                .place(List.of(place));

        final var schemaRecord = new SpatialCoverageSchemaRecord()
                .setId(schemaId);

        final var record = new RaidSpatialCoverageRecord();
        final var saved = new RaidSpatialCoverageRecord()
                .setId(id);

        when(spatialCoverageSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(schemaRecord));

        when(raidSpatialCoverageRecordFactory.create(uri, handle, schemaId)).thenReturn(record);

        when(raidSpatialCoverageRepository.create(record)).thenReturn(saved);

        spatialCoverageService.create(List.of(spatialCoverage), handle);

        verify(raidSpatialCoveragePlaceService).create(List.of(place), id);
    }

    @Test
    @DisplayName("create() throws SpatialCoverageSchemaNotFoundException")
    void createThrowsSpatialCoverageSchemaNotFoundException() {
        final var handle = "_handle";
        final var schemaUri = SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_.getValue();
        final var uri = "_uri";
        final var place = new SpatialCoveragePlace();

        final var spatialCoverage = new SpatialCoverage()
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)
                .id(uri)
                .place(List.of(place));

        when(spatialCoverageSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.empty());

        assertThrows(SpatialCoverageSchemaNotFoundException.class, () -> spatialCoverageService.create(List.of(spatialCoverage), handle));

        verifyNoInteractions(raidSpatialCoverageRecordFactory);
        verifyNoInteractions(raidSpatialCoverageRepository);
        verifyNoInteractions(raidSpatialCoveragePlaceService);
    }

    @Test
    @DisplayName("findAllByHandle() returns list of spatial coverages")
    void findAllByHandle() {
        final var handle = "_handle";
        final var schemaId = 123;
        final var schemaUri = "schema-uri";
        final var id = 234;
        final var uri = "_uri";

        final var raidSpatialCoverageRecord = new RaidSpatialCoverageRecord()
                .setSchemaId(schemaId)
                .setId(id)
                .setUri(uri);

        final var spatialCoverageSchemaRecord = new SpatialCoverageSchemaRecord()
                .setUri(schemaUri);

        final var place = new SpatialCoveragePlace();
        final var spatialCoverage = new SpatialCoverage();

        when(raidSpatialCoverageRepository.findAllByHandle(handle)).thenReturn(List.of(raidSpatialCoverageRecord));
        when(spatialCoverageSchemaRepository.findById(schemaId)).thenReturn(Optional.of(spatialCoverageSchemaRecord));
        when(raidSpatialCoveragePlaceService.findAllByRaidSpatialCoverageId(id)).thenReturn(List.of(place));
        when(spatialCoverageFactory.create(uri, schemaUri, List.of(place))).thenReturn(spatialCoverage);

        assertThat(spatialCoverageService.findAllByHandle(handle), is(List.of(spatialCoverage)));
    }

    @Test
    @DisplayName("findAllByHandle() returns empty list")
    void findAllByHandleReturnsEmptyList() {
        final var handle = "_handle";

        when(raidSpatialCoverageRepository.findAllByHandle(handle)).thenReturn(Collections.emptyList());

        assertThat(spatialCoverageService.findAllByHandle(handle), is(Collections.emptyList()));

        verifyNoInteractions(spatialCoverageSchemaRepository);
        verifyNoInteractions(raidSpatialCoveragePlaceService);
        verifyNoInteractions(spatialCoverageFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws SpatialCoverageSchemaNotFoundException")
    void findAllByHandleThrowsSpatialCoverageSchemaNotFoundException() {
        final var handle = "_handle";
        final var schemaId = 123;
        final var id = 234;
        final var uri = "_uri";

        final var raidSpatialCoverageRecord = new RaidSpatialCoverageRecord()
                .setSchemaId(schemaId)
                .setId(id)
                .setUri(uri);

        when(raidSpatialCoverageRepository.findAllByHandle(handle)).thenReturn(List.of(raidSpatialCoverageRecord));
        when(spatialCoverageSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(SpatialCoverageSchemaNotFoundException.class, () -> spatialCoverageService.findAllByHandle(handle));

        verifyNoInteractions(raidSpatialCoveragePlaceService);
        verifyNoInteractions(spatialCoverageFactory);
    }

    @Test
    @DisplayName("update() deletes and re-inserts spatial coverages")
    void update() {
        final var handle = "_handle";
        final var schemaUri = SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_.getValue();
        final var schemaId = 123;
        final var uri = "_uri";
        final var id = 234;
        final var place = new SpatialCoveragePlace();

        final var spatialCoverage = new SpatialCoverage()
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)
                .id(uri)
                .place(List.of(place));

        final var schemaRecord = new SpatialCoverageSchemaRecord()
                .setId(schemaId);

        final var record = new RaidSpatialCoverageRecord();
        final var saved = new RaidSpatialCoverageRecord()
                .setId(id);

        when(spatialCoverageSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(schemaRecord));

        when(raidSpatialCoverageRecordFactory.create(uri, handle, schemaId)).thenReturn(record);

        when(raidSpatialCoverageRepository.create(record)).thenReturn(saved);

        spatialCoverageService.update(List.of(spatialCoverage), handle);

        verify(raidSpatialCoverageRepository).deleteAllByHandle(handle);
        verify(raidSpatialCoveragePlaceService).create(List.of(place), id);
    }
}