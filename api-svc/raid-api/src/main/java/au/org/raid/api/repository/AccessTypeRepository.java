package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.AccessTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.AccessType.ACCESS_TYPE;

@Repository
@RequiredArgsConstructor
public class AccessTypeRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "access-type", key = "{#uri, #schemaId}")
    public Optional<AccessTypeRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(ACCESS_TYPE)
                .where(ACCESS_TYPE.URI.eq(uri)
                        .and(ACCESS_TYPE.SCHEMA_ID.eq(schemaId)))
                .fetchOptional();
    }

    public Optional<AccessTypeRecord> findById(final Integer id) {
        return dslContext.selectFrom(ACCESS_TYPE)
                .where(ACCESS_TYPE.ID.eq(id))
                .fetchOptional();
    }
}