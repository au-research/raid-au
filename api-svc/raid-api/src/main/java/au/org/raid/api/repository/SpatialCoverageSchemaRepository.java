package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.SpatialCoverageSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.SpatialCoverageSchema.SPATIAL_COVERAGE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class SpatialCoverageSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "spatial-coverage-schema", key = "{#uri}")
    public Optional<SpatialCoverageSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(SPATIAL_COVERAGE_SCHEMA)
                .where(SPATIAL_COVERAGE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<SpatialCoverageSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(SPATIAL_COVERAGE_SCHEMA)
                .where(SPATIAL_COVERAGE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}
