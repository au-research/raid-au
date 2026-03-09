package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.ContributorRoleRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.ContributorRole.CONTRIBUTOR_ROLE;

@Repository
@RequiredArgsConstructor
public class ContributorRoleRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "contributor-role", key = "{#uri, #schemaId}")
    public Optional<ContributorRoleRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(CONTRIBUTOR_ROLE)
                .where(CONTRIBUTOR_ROLE.URI.eq(uri)
                        .and(CONTRIBUTOR_ROLE.SCHEMA_ID.eq(schemaId)))
                .fetchOptional();
    }

    public Optional<ContributorRoleRecord> findById(final Integer id) {
        return dslContext.selectFrom(CONTRIBUTOR_ROLE)
                .where(CONTRIBUTOR_ROLE.ID.eq(id))
                .fetchOptional();
    }
}