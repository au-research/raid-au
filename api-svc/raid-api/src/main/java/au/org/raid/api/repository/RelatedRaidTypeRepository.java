package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RelatedRaidTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedRaidType.RELATED_RAID_TYPE;

@Repository
@RequiredArgsConstructor
public class RelatedRaidTypeRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "related-raid-type", key = "{#uri, #schemaId}")
    public Optional<RelatedRaidTypeRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(RELATED_RAID_TYPE)
                .where(RELATED_RAID_TYPE.URI.eq(uri).and(RELATED_RAID_TYPE.SCHEMA_ID.eq(schemaId))).
                fetchOptional();
    }

    public Optional<RelatedRaidTypeRecord> findByUri(final String uri) {
        return dslContext.selectFrom(RELATED_RAID_TYPE)
                .where(RELATED_RAID_TYPE.URI.eq(uri)).
                fetchOptional();
    }

    public Optional<RelatedRaidTypeRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_RAID_TYPE)
                .where(RELATED_RAID_TYPE.ID.eq(id))
                .fetchOptional();
    }
}