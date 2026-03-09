package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.TitleTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.TitleType.TITLE_TYPE;

@Repository
@RequiredArgsConstructor
public class TitleTypeRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "title-type", key = "{#uri, #schemaId}")
    public Optional<TitleTypeRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext
                .selectFrom(TITLE_TYPE)
                .where(TITLE_TYPE.URI.eq(uri)
                        .and(TITLE_TYPE.SCHEMA_ID.eq(schemaId)))
                .fetchOptional();
    }

    public Optional<TitleTypeRecord> findById(final Integer id) {
        return dslContext.selectFrom(TITLE_TYPE)
                .where(TITLE_TYPE.ID.eq(id))
                .fetchOptional();
    }
}