package au.org.raid.api.service;

import au.org.raid.api.exception.SpatialCoverageSchemaNotFoundException;
import au.org.raid.api.factory.SpatialCoverageFactory;
import au.org.raid.api.factory.record.RaidSpatialCoverageRecordFactory;
import au.org.raid.api.repository.RaidSpatialCoverageRepository;
import au.org.raid.api.repository.SpatialCoverageSchemaRepository;
import au.org.raid.idl.raidv2.model.SpatialCoverage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SpatialCoverageService {
    private final SpatialCoverageSchemaRepository spatialCoverageSchemaRepository;
    private final RaidSpatialCoverageRepository raidSpatialCoverageRepository;
    private final RaidSpatialCoverageRecordFactory raidSpatialCoverageRecordFactory;
    private final RaidSpatialCoveragePlaceService raidSpatialCoveragePlaceService;
    private final SpatialCoverageFactory spatialCoverageFactory;

    public void create(final List<SpatialCoverage> spatialCoverages, final String handle) {
        if (spatialCoverages == null) {
            return;
        }

        for (final var spatialCoverage : spatialCoverages) {

            final var spatialCoverageSchemaRecord = spatialCoverageSchemaRepository.findByUri(spatialCoverage.getSchemaUri().getValue())
                    .orElseThrow(() -> new SpatialCoverageSchemaNotFoundException(spatialCoverage.getSchemaUri().getValue()));

            final var record = raidSpatialCoverageRepository.create(raidSpatialCoverageRecordFactory.create(
                    spatialCoverage.getId(),
                    handle,
                    spatialCoverageSchemaRecord.getId()));

            raidSpatialCoveragePlaceService.create(spatialCoverage.getPlace(), record.getId());
        }
    }

    public List<SpatialCoverage> findAllByHandle(final String handle) {
        final var spatialCoverages = new ArrayList<SpatialCoverage>();
        final var records = raidSpatialCoverageRepository.findAllByHandle(handle);

        for (final var record : records) {
            final var schemaId = record.getSchemaId();
            final var schemaRecord = spatialCoverageSchemaRepository.findById(schemaId)
                    .orElseThrow(() -> new SpatialCoverageSchemaNotFoundException(schemaId));

            final var places = raidSpatialCoveragePlaceService.findAllByRaidSpatialCoverageId(record.getId());
            spatialCoverages.add(spatialCoverageFactory.create(record.getUri(), schemaRecord.getUri(), places));
        }

        return spatialCoverages;
    }

    public void update(final List<SpatialCoverage> spatialCoverages, final String handle) {
        raidSpatialCoverageRepository.deleteAllByHandle(handle);
        create(spatialCoverages, handle);
    }
}
