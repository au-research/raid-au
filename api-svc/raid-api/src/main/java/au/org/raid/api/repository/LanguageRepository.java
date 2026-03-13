package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.LanguageRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.Language.LANGUAGE;

@Repository
@RequiredArgsConstructor
public class LanguageRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "language", key = "{#code, #schemaId}")
    public Optional<LanguageRecord> findByIdAndSchemaId(final String code, final int schemaId) {
        return dslContext
                .selectFrom(LANGUAGE)
                .where(LANGUAGE.CODE.eq(code))
                .and(LANGUAGE.SCHEMA_ID.eq(schemaId))
                .fetchOptional();
    }

    public Optional<LanguageRecord> findById(final Integer id) {
        return dslContext.selectFrom(LANGUAGE)
                .where(LANGUAGE.ID.eq(id))
                .fetchOptional();
    }
}
