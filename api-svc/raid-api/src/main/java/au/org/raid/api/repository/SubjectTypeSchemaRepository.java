package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.SubjectTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.SubjectTypeSchema.SUBJECT_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class SubjectTypeSchemaRepository {
    private final DSLContext dslContext;

    public Optional<SubjectTypeSchemaRecord> findById(final Integer id) {
        return dslContext.selectFrom(SUBJECT_TYPE_SCHEMA)
                .where(SUBJECT_TYPE_SCHEMA.ID.eq(id))
                .fetchOptional();
    }

    public Optional<SubjectTypeSchemaRecord> findByUri(final String uri) {
        return dslContext.selectFrom(SUBJECT_TYPE_SCHEMA)
                .where(SUBJECT_TYPE_SCHEMA.URI.eq(uri))
                .fetchOptional();
    }

}
