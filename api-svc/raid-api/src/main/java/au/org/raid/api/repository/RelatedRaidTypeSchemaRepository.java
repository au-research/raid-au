package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedRaidTypeSchema.RELATED_RAID_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class RelatedRaidTypeSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "related-raid-type-schema", key = "{#uri}")
    public Optional<RelatedRaidTypeSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(RELATED_RAID_TYPE_SCHEMA)
                .where(RELATED_RAID_TYPE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<RelatedRaidTypeSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(RELATED_RAID_TYPE_SCHEMA)
                .where(RELATED_RAID_TYPE_SCHEMA.URI.eq(uri))
                .and(RELATED_RAID_TYPE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<RelatedRaidTypeSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_RAID_TYPE_SCHEMA)
                .where(RELATED_RAID_TYPE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}