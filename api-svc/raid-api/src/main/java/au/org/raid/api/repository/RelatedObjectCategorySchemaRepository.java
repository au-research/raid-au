package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RelatedObjectCategorySchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedObjectCategorySchema.RELATED_OBJECT_CATEGORY_SCHEMA;

@Repository
@RequiredArgsConstructor
public class RelatedObjectCategorySchemaRepository {
    private final DSLContext dslContext;

    public Optional<RelatedObjectCategorySchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(RELATED_OBJECT_CATEGORY_SCHEMA)
                .where(RELATED_OBJECT_CATEGORY_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<RelatedObjectCategorySchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_OBJECT_CATEGORY_SCHEMA)
                .where(RELATED_OBJECT_CATEGORY_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}