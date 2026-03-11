package au.org.raid.api.repository;

import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.SubjectTypeSchema;
import au.org.raid.db.jooq.tables.records.SubjectTypeSchemaRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    public List<SubjectTypeSchemaRecord> findAllActive() {
        return dslContext.selectFrom(SUBJECT_TYPE_SCHEMA)
                .where(SUBJECT_TYPE_SCHEMA.STATUS.eq(SchemaStatus.active))
                .fetch();
    }

    @Cacheable(value = "subject-type-schema", key = "{#schemaUri}")
    public Optional<SubjectTypeSchemaRecord> findByUri(final String schemaUri) {
        return dslContext.selectFrom(SUBJECT_TYPE_SCHEMA)
                .where(SUBJECT_TYPE_SCHEMA.URI.eq(schemaUri))
                .fetchOptional();
    }
}
