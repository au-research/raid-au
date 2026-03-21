package au.org.raid.api.validator;

import au.org.raid.api.repository.SubjectTypeRepository;
import au.org.raid.api.repository.SubjectTypeSchemaRepository;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import au.org.raid.db.jooq.tables.records.SubjectTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.Subject;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Component
@RequiredArgsConstructor
public class SubjectValidator {

    private final SubjectTypeRepository subjectTypeRepository;
    private final SubjectTypeSchemaRepository subjectTypeSchemaRepository;
    private final SubjectKeywordValidator subjectKeywordValidator;
    
    public List<ValidationFailure> validate(List<Subject> subjects) {

        final var subjectTypeSchemaRecords = subjectTypeSchemaRepository.findAllActive();

        final var subjectTypeSchemaMap = subjectTypeSchemaRecords
                .stream().collect(Collectors.toUnmodifiableMap(SubjectTypeSchemaRecord::getUri, SubjectTypeSchemaRecord::getId));

        final var subjectTypeSchemaUris = new HashSet<>(subjectTypeSchemaMap.keySet());

        final var subjectIdStartsWithMap = subjectTypeSchemaRecords.stream()
                .collect(Collectors.toUnmodifiableMap(SubjectTypeSchemaRecord::getUri, SubjectTypeSchemaRecord::getIdStartsWith));

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
            }


            if (subject.getSchemaUri() == null) {
                final var failure = new ValidationFailure();
                failure.setFieldId(String.format("subject[%d].schemaUri", subjectIndex));
                failure.setMessage(NOT_SET_MESSAGE);
                failure.setErrorType(NOT_SET_TYPE);

                failures.add(failure);
            } else if (!subjectTypeSchemaUris.contains(subject.getSchemaUri())) {
                final var failure = new ValidationFailure();
                failure.setFieldId(String.format("subject[%d].schemaUri", subjectIndex));
                failure.setMessage(String.format("must be %s.", SchemaValues.SUBJECT_SCHEMA_URI.getUri()));
                failure.setErrorType(INVALID_VALUE_TYPE);

                failures.add(failure);
            } else if (
                    subject.getId() != null && !subject.getId().startsWith(subjectIdStartsWithMap.get(subject.getSchemaUri()))
            ) {
                final var failure = new ValidationFailure();
                failure.setFieldId(String.format("subject[%d].id", subjectIndex));
                failure.setMessage(String.format("%s is not a valid id for schema %s", subject.getId(), subject.getSchemaUri()));
                failure.setErrorType(INVALID_VALUE_TYPE);

                failures.add(failure);
            } else if (subject.getId() != null){
                final var subjectId = subject.getId().substring(subject.getId().lastIndexOf('/') + 1);

                final var schemaId = subjectTypeSchemaMap.get(subject.getSchemaUri());

                if (schemaId != null) {
                    final Optional<SubjectTypeRecord> subjectTypeRecord = subjectTypeRepository.findByIdAndSchemaId(subjectId, schemaId);

                    if (subjectTypeRecord.isEmpty()) {
                        final var failure = new ValidationFailure();
                        failure.setFieldId(String.format("subject[%d].id", subjectIndex));
                        failure.setMessage(String.format("%s is not a valid id for schema %s", subject.getId(), subject.getSchemaUri()));
                        failure.setErrorType(INVALID_VALUE_TYPE);

                        failures.add(failure);
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