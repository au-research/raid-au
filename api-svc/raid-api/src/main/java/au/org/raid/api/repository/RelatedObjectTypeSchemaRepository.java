package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedObjectTypeSchema.RELATED_OBJECT_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class RelatedObjectTypeSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "related-object-type-schema", key = "{#uri}")
    public Optional<RelatedObjectTypeSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(RELATED_OBJECT_TYPE_SCHEMA)
                .where(RELATED_OBJECT_TYPE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<RelatedObjectTypeSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(RELATED_OBJECT_TYPE_SCHEMA)
                .where(RELATED_OBJECT_TYPE_SCHEMA.URI.eq(uri))
                .and(RELATED_OBJECT_TYPE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<RelatedObjectTypeSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_OBJECT_TYPE_SCHEMA)
                .where(RELATED_OBJECT_TYPE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}