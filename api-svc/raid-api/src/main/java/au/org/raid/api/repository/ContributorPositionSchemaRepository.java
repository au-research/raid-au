package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.ContributorPositionSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.ContributorPositionSchema.CONTRIBUTOR_POSITION_SCHEMA;

@Repository
@RequiredArgsConstructor
public class ContributorPositionSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "contributor-position-schema", key = "{#uri}")
    public Optional<ContributorPositionSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(CONTRIBUTOR_POSITION_SCHEMA)
                .where(CONTRIBUTOR_POSITION_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<ContributorPositionSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(CONTRIBUTOR_POSITION_SCHEMA)
                .where(CONTRIBUTOR_POSITION_SCHEMA.URI.eq(uri))
                .and(CONTRIBUTOR_POSITION_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<ContributorPositionSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(CONTRIBUTOR_POSITION_SCHEMA)
                .where(CONTRIBUTOR_POSITION_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}