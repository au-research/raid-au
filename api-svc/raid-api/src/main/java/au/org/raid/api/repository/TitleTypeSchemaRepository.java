package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.TitleTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.TitleTypeSchema.TITLE_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class TitleTypeSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "title-type-schema", key = "{#uri}")
    public Optional<TitleTypeSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(TITLE_TYPE_SCHEMA)
                .where(TITLE_TYPE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<TitleTypeSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(TITLE_TYPE_SCHEMA)
                .where(TITLE_TYPE_SCHEMA.URI.eq(uri))
                .and(TITLE_TYPE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<TitleTypeSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(TITLE_TYPE_SCHEMA)
                .where(TITLE_TYPE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}