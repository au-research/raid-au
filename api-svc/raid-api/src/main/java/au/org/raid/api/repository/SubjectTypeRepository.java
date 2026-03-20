package au.org.raid.api.repository;

import au.org.raid.api.repository.dto.SubjectTypeWithSchema;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static au.org.raid.db.jooq.tables.SubjectType.SUBJECT_TYPE;
import static au.org.raid.db.jooq.tables.SubjectTypeSchema.SUBJECT_TYPE_SCHEMA;

@Repository
@RequiredArgsConstructor
public class SubjectTypeRepository {
    private final DSLContext dslContext;

    public Optional<SubjectTypeWithSchema> findById(final Integer subjectId) {
        return dslContext.select(SUBJECT_TYPE.ID, SUBJECT_TYPE.NAME, SUBJECT_TYPE.DESCRIPTION, SUBJECT_TYPE.NOTE, SUBJECT_TYPE_SCHEMA.ID.as("schemaId"), SUBJECT_TYPE_SCHEMA.URI.as("schemaUri"))
                .from(SUBJECT_TYPE)
                .join(SUBJECT_TYPE_SCHEMA)
                .on(SUBJECT_TYPE.SCHEMA_ID.eq(SUBJECT_TYPE_SCHEMA.ID))
                .where(SUBJECT_TYPE.SCHEMA_ID.cast(Integer.class).eq(subjectId))
                .fetchOptionalInto(SubjectTypeWithSchema.class);
    }

    public Optional<SubjectTypeWithSchema> findBySubjectTypeIdAndSchemaUri(final String subjectId, final String schemaUri) {
        return dslContext.select(SUBJECT_TYPE.ID, SUBJECT_TYPE.NAME, SUBJECT_TYPE.DESCRIPTION, SUBJECT_TYPE.NOTE, SUBJECT_TYPE_SCHEMA.ID.as("schemaId"), SUBJECT_TYPE_SCHEMA.URI.as("schemaUri"))
                .from(SUBJECT_TYPE)
                .join(SUBJECT_TYPE_SCHEMA)
                .on(SUBJECT_TYPE.SCHEMA_ID.eq(SUBJECT_TYPE_SCHEMA.ID))
                .where(SUBJECT_TYPE.ID.eq(subjectId).and(SUBJECT_TYPE_SCHEMA.URI.eq(schemaUri)))
                .fetchOptionalInto(SubjectTypeWithSchema.class);
    }

    @Cacheable(value = "subject-type", key = "{#subjectId, #schemaId}")
    public Optional<SubjectTypeRecord> findByIdAndSchemaId(final String subjectId, final int schemaId) {
        return dslContext.selectFrom(SUBJECT_TYPE)
                .where(SUBJECT_TYPE.ID.eq(subjectId))
                .and(SUBJECT_TYPE.SCHEMA_ID.eq(schemaId))
                .fetchOptional();
    }

}