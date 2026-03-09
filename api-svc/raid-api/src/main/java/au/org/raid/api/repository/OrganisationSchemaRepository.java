package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.OrganisationSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.OrganisationSchema.ORGANISATION_SCHEMA;

@Repository
@RequiredArgsConstructor
public class OrganisationSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "organisation-schema", key = "{#uri}")
    public Optional<OrganisationSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(ORGANISATION_SCHEMA)
                .where(ORGANISATION_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<OrganisationSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(ORGANISATION_SCHEMA)
                .where(ORGANISATION_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}
