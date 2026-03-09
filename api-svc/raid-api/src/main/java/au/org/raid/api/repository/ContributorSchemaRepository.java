package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.ContributorSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.ContributorSchema.CONTRIBUTOR_SCHEMA;

@Repository
@RequiredArgsConstructor
public class ContributorSchemaRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "contributor-schema", key = "{#uri}")
    public Optional<ContributorSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(CONTRIBUTOR_SCHEMA)
                .where(CONTRIBUTOR_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

    public Optional<ContributorSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(CONTRIBUTOR_SCHEMA)
                .where(CONTRIBUTOR_SCHEMA.ID.eq(id))
                .fetchOptional();
    }
}
