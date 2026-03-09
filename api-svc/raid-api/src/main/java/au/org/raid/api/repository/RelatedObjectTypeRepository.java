package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RelatedObjectTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedObjectType.RELATED_OBJECT_TYPE;

@Repository
@RequiredArgsConstructor
public class RelatedObjectTypeRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "related-object-type", key = "{#uri, #schemaId}")
    public Optional<RelatedObjectTypeRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(RELATED_OBJECT_TYPE)
                .where(RELATED_OBJECT_TYPE.URI.eq(uri).and(RELATED_OBJECT_TYPE.SCHEMA_ID.eq(schemaId))).
                fetchOptional();
    }

    public Optional<RelatedObjectTypeRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_OBJECT_TYPE)
                .where(RELATED_OBJECT_TYPE.ID.eq(id))
                .fetchOptional();
    }
}
