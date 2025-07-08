package au.org.raid.api.repository;

import au.org.raid.api.repository.dto.SubjectTypeWithSchema;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.SubjectType.SUBJECT_TYPE;
import static au.org.raid.db.jooq.tables.SubjectTypeSchema.SUBJECT_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class SubjectTypeRepository {
    private final DSLContext dslContext;

    public Optional<SubjectTypeWithSchema> findById(final Integer subjectId) {
        return dslContext.select(SUBJECT_TYPE.ID, SUBJECT_TYPE.SUBJECT_TYPE_ID, SUBJECT_TYPE.NAME, SUBJECT_TYPE.DESCRIPTION, SUBJECT_TYPE.NOTE, SUBJECT_TYPE_SCHEMA.ID.as("schemaId"), SUBJECT_TYPE_SCHEMA.URI.as("schemaUri"))
                .from(SUBJECT_TYPE)
                .join(SUBJECT_TYPE_SCHEMA)
                .on(SUBJECT_TYPE.SCHEMA_ID.eq(SUBJECT_TYPE_SCHEMA.ID))
                .where(SUBJECT_TYPE.ID.eq(subjectId))
                .fetchOptionalInto(SubjectTypeWithSchema.class);
    }

    public Optional<SubjectTypeWithSchema> findBySubjectTypeIdAndSchemaUri(final String subjectId, final String schemaUri) {
        return dslContext.select(SUBJECT_TYPE.ID, SUBJECT_TYPE.SUBJECT_TYPE_ID, SUBJECT_TYPE.NAME, SUBJECT_TYPE.DESCRIPTION, SUBJECT_TYPE.NOTE, SUBJECT_TYPE_SCHEMA.ID.as("schemaId"), SUBJECT_TYPE_SCHEMA.URI.as("schemaUri"))
                .from(SUBJECT_TYPE)
                .join(SUBJECT_TYPE_SCHEMA)
                .on(SUBJECT_TYPE.SCHEMA_ID.eq(SUBJECT_TYPE_SCHEMA.ID))
                .where(SUBJECT_TYPE.SUBJECT_TYPE_ID.eq(subjectId).and(SUBJECT_TYPE_SCHEMA.URI.eq(schemaUri)))
                .fetchOptionalInto(SubjectTypeWithSchema.class);
    }
}