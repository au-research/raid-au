package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RelatedObjectSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedObjectSchema.RELATED_OBJECT_SCHEMA;

@Repository
@RequiredArgsConstructor
public class RelatedObjectSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "related-object-schema", key = "{#uri}")
    public Optional<RelatedObjectSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(RELATED_OBJECT_SCHEMA)
                .where(RELATED_OBJECT_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<RelatedObjectSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_OBJECT_SCHEMA)
                .where(RELATED_OBJECT_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}
