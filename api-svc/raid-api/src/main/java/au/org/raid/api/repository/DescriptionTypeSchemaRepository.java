package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.DescriptionTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.DescriptionTypeSchema.DESCRIPTION_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class DescriptionTypeSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "description-type-schema", key = "{#uri}")
    public Optional<DescriptionTypeSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(DESCRIPTION_TYPE_SCHEMA)
                .where(DESCRIPTION_TYPE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }
    public Optional<DescriptionTypeSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(DESCRIPTION_TYPE_SCHEMA)
                .where(DESCRIPTION_TYPE_SCHEMA.URI.eq(uri))
                .and(DESCRIPTION_TYPE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<DescriptionTypeSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(DESCRIPTION_TYPE_SCHEMA)
                .where(DESCRIPTION_TYPE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}