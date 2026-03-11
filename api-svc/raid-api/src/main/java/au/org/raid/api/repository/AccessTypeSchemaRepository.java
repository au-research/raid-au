package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.AccessTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.AccessTypeSchema.ACCESS_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class AccessTypeSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "access-type-schema", key = "{#uri}")
    public Optional<AccessTypeSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(ACCESS_TYPE_SCHEMA)
                .where(ACCESS_TYPE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<AccessTypeSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(ACCESS_TYPE_SCHEMA)
                .where(ACCESS_TYPE_SCHEMA.URI.eq(uri))
                .and(ACCESS_TYPE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<AccessTypeSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(ACCESS_TYPE_SCHEMA)
                .where(ACCESS_TYPE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}