package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RelatedObjectCategoryRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.RelatedObjectCategory.RELATED_OBJECT_CATEGORY;

@Repository
@RequiredArgsConstructor
public class RelatedObjectCategoryRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "related-object-category", key = "{#uri, #schemaId}")
    public Optional<RelatedObjectCategoryRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(RELATED_OBJECT_CATEGORY)
                .where(RELATED_OBJECT_CATEGORY.URI.eq(uri)
                        .and(RELATED_OBJECT_CATEGORY.SCHEMA_ID.eq(schemaId)))
                .fetchOptional();
    }

    public Optional<RelatedObjectCategoryRecord> findById(final Integer id) {
        return dslContext.selectFrom(RELATED_OBJECT_CATEGORY)
                .where(RELATED_OBJECT_CATEGORY.ID.eq(id))
                .fetchOptional();
    }
}