package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.DescriptionTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.DescriptionType.DESCRIPTION_TYPE;

@Repository
@RequiredArgsConstructor
public class DescriptionTypeRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "description-type", key = "{#uri, #schemaId}")
    public Optional<DescriptionTypeRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext
                .selectFrom(DESCRIPTION_TYPE)
                .where(DESCRIPTION_TYPE.URI.eq(uri))
                .and(DESCRIPTION_TYPE.SCHEMA_ID.eq(schemaId))
                .fetchOptional();
    }

    public Optional<DescriptionTypeRecord> findById(final Integer id) {
        return dslContext.selectFrom(DESCRIPTION_TYPE)
                .where(DESCRIPTION_TYPE.ID.eq(id))
                .fetchOptional();
    }
}