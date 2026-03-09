package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.OrganisationRoleRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.OrganisationRole.ORGANISATION_ROLE;

@Repository
@RequiredArgsConstructor
public class OrganisationRoleRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "organisation-role", key = "{#uri, #schemaId}")
    public Optional<OrganisationRoleRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(ORGANISATION_ROLE)
                .where(ORGANISATION_ROLE.URI.eq(uri)
                        .and(ORGANISATION_ROLE.SCHEMA_ID.eq(schemaId)))
                .fetchOptional();
    }

    public Optional<OrganisationRoleRecord> findById(final Integer id) {
        return dslContext.selectFrom(ORGANISATION_ROLE)
                .where(ORGANISATION_ROLE.ID.eq(id))
                .fetchOptional();
    }
}