package au.org.raid.api.validator;

import au.org.raid.api.repository.SubjectTypeRepository;
import au.org.raid.api.repository.dto.SubjectTypeWithSchema;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import au.org.raid.idl.raidv2.model.Subject;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Component
@RequiredArgsConstructor
public class SubjectValidator {

    private final SubjectTypeRepository subjectTypeRepository;
    private final SubjectKeywordValidator subjectKeywordValidator;
    
    public List<ValidationFailure> validate(List<Subject> subjects) {

        final var failures = new ArrayList<ValidationFailure>();

        if (subjects == null) {
            return failures;
        }

        IntStream.range(0, subjects.size()).forEach(subjectIndex -> {
            final var subject = subjects.get(subjectIndex);

            if (subject.getId() == null) {
                final var failure = new ValidationFailure();
                failure.setFieldId(String.format("subject[%d].id", subjectIndex));
                failure.setMessage(NOT_SET_MESSAGE);
                failure.setErrorType(NOT_SET_TYPE);

                failures.add(failure);
            } else {
                final var subjectId = subject.getId().substring(subject.getId().lastIndexOf('/') + 1);

                if (!subject.getId().startsWith(SchemaValues.SUBJECT_ID_PREFIX.getUri()) || subjectId.matches(".*\\D.*")) {
                    final var failure = new ValidationFailure();
                    failure.setFieldId(String.format("subject[%d].id", subjectIndex));
                    failure.setMessage(String.format("%s is not a valid field of research", subject.getId()));
                    failure.setErrorType(INVALID_VALUE_TYPE);

                    failures.add(failure);
                } else {
                    if (subject.getSchemaUri() == null || !subject.getSchemaUri().getValue().equals(SchemaValues.SUBJECT_SCHEMA_URI.getUri())) {
                        final var failure = new ValidationFailure();
                        failure.setFieldId(String.format("subject[%d].schemaUri", subjectIndex));
                        failure.setMessage(String.format("must be %s.", SchemaValues.SUBJECT_SCHEMA_URI.getUri()));
                        failure.setErrorType(INVALID_VALUE_TYPE);

                        failures.add(failure);
                    } else {
                        final Optional<SubjectTypeWithSchema> subjectTypeRecord = subjectTypeRepository.findBySubjectTypeIdAndSchemaUri(subjectId, subject.getSchemaUri().getValue());

                        if (subjectTypeRecord.isEmpty()) {
                            final var failure = new ValidationFailure();
                            failure.setFieldId(String.format("subject[%d].id", subjectIndex));
                            failure.setMessage(String.format("%s is not a standard FoR code", subject.getId()));
                            failure.setErrorType(INVALID_VALUE_TYPE);

                            failures.add(failure);
                        }
                    }
                }
            }

            if (subject.getKeyword() != null) {
                IntStream.range(0, subject.getKeyword().size()).forEach(keywordIndex -> {
                    final var keyword = subject.getKeyword().get(keywordIndex);
                    failures.addAll(subjectKeywordValidator.validate(keyword, subjectIndex, keywordIndex));
                });
            }
        });

        return failures;
    }
}