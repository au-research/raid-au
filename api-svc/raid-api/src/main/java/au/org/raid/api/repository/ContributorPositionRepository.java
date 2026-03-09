package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.ContributorPositionRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.ContributorPosition.CONTRIBUTOR_POSITION;

@Repository
@RequiredArgsConstructor
public class ContributorPositionRepository {
    private final DSLContext dslContext;

    @Cacheable(value = "contributor-position", key = "{#uri, #schemaId}")
    public Optional<ContributorPositionRecord> findByUriAndSchemaId(final String uri, final int schemaId) {
        return dslContext.selectFrom(CONTRIBUTOR_POSITION)
                .where(CONTRIBUTOR_POSITION.URI.eq(uri)
                        .and(CONTRIBUTOR_POSITION.SCHEMA_ID.eq(schemaId)))
                .fetchOptional();
    }

    public Optional<ContributorPositionRecord> findById(final Integer id) {
        return dslContext.selectFrom(CONTRIBUTOR_POSITION)
                .where(CONTRIBUTOR_POSITION.ID.eq(id))
                .fetchOptional();
    }
}