package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.OrganisationRoleSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.OrganisationRoleSchema.ORGANISATION_ROLE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class OrganisationRoleSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "organisation-role-schema", key = "{#uri}")
    public Optional<OrganisationRoleSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(ORGANISATION_ROLE_SCHEMA)
                .where(ORGANISATION_ROLE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<OrganisationRoleSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(ORGANISATION_ROLE_SCHEMA)
                .where(ORGANISATION_ROLE_SCHEMA.URI.eq(uri))
                .and(ORGANISATION_ROLE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<OrganisationRoleSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(ORGANISATION_ROLE_SCHEMA)
                .where(ORGANISATION_ROLE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}