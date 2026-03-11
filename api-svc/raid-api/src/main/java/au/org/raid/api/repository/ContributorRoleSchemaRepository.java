package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.ContributorRoleSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.ContributorRoleSchema.CONTRIBUTOR_ROLE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class ContributorRoleSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "contributor-role-schema", key = "{#uri}")
    public Optional<ContributorRoleSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(CONTRIBUTOR_ROLE_SCHEMA)
                .where(CONTRIBUTOR_ROLE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<ContributorRoleSchemaRecord> findActiveByUri(final String uri) {
        return dslContext.selectFrom(CONTRIBUTOR_ROLE_SCHEMA)
                .where(CONTRIBUTOR_ROLE_SCHEMA.URI.eq(uri))
                .and(CONTRIBUTOR_ROLE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetchOptional();
    }

    public Optional<ContributorRoleSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(CONTRIBUTOR_ROLE_SCHEMA)
                .where(CONTRIBUTOR_ROLE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}