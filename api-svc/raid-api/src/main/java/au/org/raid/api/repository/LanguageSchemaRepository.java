package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.LanguageSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.LanguageSchema.LANGUAGE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class LanguageSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "language-schema", key = "{#uri}")
    public Optional<LanguageSchemaRecord> findByUri(final String uri) {
        return dslContext
                .selectFrom(LANGUAGE_SCHEMA)
                .where(LANGUAGE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<LanguageSchemaRecord> findActiveByUri(final String uri) {
        return dslContext
                .selectFrom(LANGUAGE_SCHEMA)
                .where(LANGUAGE_SCHEMA.URI.eq(uri))
                .and(LANGUAGE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<LanguageSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(LANGUAGE_SCHEMA)
                .where(LANGUAGE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}
