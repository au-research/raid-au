package au.org.raid.api.validator;

import au.org.raid.api.repository.SubjectTypeRepository;
import au.org.raid.api.repository.SubjectTypeSchemaRepository;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import au.org.raid.db.jooq.tables.records.SubjectTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.Subject;
import au.org.raid.idl.raidv2.model.SubjectKeyword;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectValidatorTest {
    @Mock
    private SubjectTypeRepository subjectTypeRepository;

    @Mock
    private SubjectTypeSchemaRepository subjectTypeSchemaRepository;

    @Mock
    private SubjectKeywordValidator keywordValidator;

    @InjectMocks
    private SubjectValidator validationService;

    @Test
    void noFailuresWithValidCode() {
        final var idStartsWith = "https://linked.data.gov.au/def/anzsrc-for/2020/";
        final var subjectId = "222222";
        final var id = idStartsWith.concat(subjectId);
        final var keyword = new SubjectKeyword();
        final var schemaId = 1;

        final var subject = new Subject()
                .id(id)
                .schemaUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri())
                .keyword(List.of(new SubjectKeyword()));

        final var subjectTypeSchemaRecord = new SubjectTypeSchemaRecord();
        subjectTypeSchemaRecord.setId(schemaId);
        subjectTypeSchemaRecord.setUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri());
        subjectTypeSchemaRecord.setIdStartsWith(idStartsWith);

        when(keywordValidator.validate(keyword,0,0)).thenReturn(Collections.emptyList());
        when(subjectTypeRepository.findByIdAndSchemaId(subjectId, schemaId)).thenReturn(Optional.of(new SubjectTypeRecord()));
        when(subjectTypeSchemaRepository.findAllActive()).thenReturn(List.of(subjectTypeSchemaRecord));

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures, empty());
    }

    @Test
    void noFailuresIfSubjectBlockIsNull() {
        final List<ValidationFailure> validationFailures = validationService.validate(null);
        assertThat(validationFailures, empty());
    }

    @Test
    void returnsFailureWithInvalidUrlPrefix() {
        final var idStartsWith = "https://linked.data.gov.au/def/anzsrc-for/2020/";
        final var subjectId = "222222";
        final var id = "https://data.gov.au/def/anzsrc-for/2020/".concat(subjectId);
        final var schemaId = 1;
        final var keyword = new SubjectKeyword();

        final var subject = new Subject()
                .id(id)
                .schemaUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri())
                .keyword(List.of(new SubjectKeyword()));

        final var subjectTypeSchemaRecord = new SubjectTypeSchemaRecord();
        subjectTypeSchemaRecord.setId(schemaId);
        subjectTypeSchemaRecord.setUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri());
        subjectTypeSchemaRecord.setIdStartsWith(idStartsWith);

        when(keywordValidator.validate(keyword,0,0)).thenReturn(Collections.emptyList());
        when(subjectTypeSchemaRepository.findAllActive()).thenReturn(List.of(subjectTypeSchemaRecord));

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].id")
                        .errorType("invalidValue")
                        .message("https://data.gov.au/def/anzsrc-for/2020/222222 is not a valid id for schema https://vocabs.ardc.edu.au/viewById/316")
        )));
    }

    @Test
    void returnsFailureIfCodeNotFound() {
        final var idStartsWith = "https://linked.data.gov.au/def/anzsrc-for/2020/";
        final var subjectId = "222222";
        final var id = idStartsWith.concat(subjectId);
        final var keyword = new SubjectKeyword();
        final var schemaId = 1;

        final var subject = new Subject()
                .id(id)
                .schemaUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri())
                .keyword(List.of(new SubjectKeyword()));

        final var subjectTypeSchemaRecord = new SubjectTypeSchemaRecord();
        subjectTypeSchemaRecord.setId(schemaId);
        subjectTypeSchemaRecord.setUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri());
        subjectTypeSchemaRecord.setIdStartsWith(idStartsWith);

        when(keywordValidator.validate(keyword,0,0)).thenReturn(Collections.emptyList());
        when(subjectTypeRepository.findByIdAndSchemaId(subjectId, schemaId)).thenReturn(Optional.empty());
        when(subjectTypeSchemaRepository.findAllActive()).thenReturn(List.of(subjectTypeSchemaRecord));

        final List<ValidationFailure> failures = validationService.validate(Collections.singletonList(subject));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].id")
                        .errorType("invalidValue")
                        .message("https://linked.data.gov.au/def/anzsrc-for/2020/222222 is not a valid id for schema https://vocabs.ardc.edu.au/viewById/316")
        )));
    }

    @Test
    void addsFailureWithInvalidMissingSubjectSchemeUri() {
        final var idStartsWith = "https://linked.data.gov.au/def/anzsrc-for/2020/";
        final var subjectId = "222222";
        final var id = idStartsWith.concat(subjectId);
        final var keyword = new SubjectKeyword();
        final var schemaId = 1;

        final var subject = new Subject()
                .id(id)
                .keyword(List.of(new SubjectKeyword()));

        final var subjectTypeSchemaRecord = new SubjectTypeSchemaRecord();
        subjectTypeSchemaRecord.setId(schemaId);
        subjectTypeSchemaRecord.setUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri());
        subjectTypeSchemaRecord.setIdStartsWith(idStartsWith);

        when(keywordValidator.validate(keyword,0,0)).thenReturn(Collections.emptyList());
        when(subjectTypeSchemaRepository.findAllActive()).thenReturn(List.of(subjectTypeSchemaRecord));

        final List<ValidationFailure> failures = validationService.validate(Collections.singletonList(subject));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        )));
    }

    @Test
    void addsFailureWithInvalidInvalidSubjectSchemeUri() {
        final var idStartsWith = "https://linked.data.gov.au/def/anzsrc-for/2020/";
        final var subjectId = "222222";
        final var id = idStartsWith.concat(subjectId);
        final var keyword = new SubjectKeyword();
        final var schemaId = 1;

        final var subject = new Subject()
                .id(id)
                .schemaUri("invalid")
                .keyword(List.of(new SubjectKeyword()));

        final var subjectTypeSchemaRecord = new SubjectTypeSchemaRecord();
        subjectTypeSchemaRecord.setId(schemaId);
        subjectTypeSchemaRecord.setUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri());
        subjectTypeSchemaRecord.setIdStartsWith(idStartsWith);

        when(keywordValidator.validate(keyword,0,0)).thenReturn(Collections.emptyList());
        when(subjectTypeSchemaRepository.findAllActive()).thenReturn(List.of(subjectTypeSchemaRecord));

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures.size(), is(1));
        assertThat(validationFailures.get(0).getMessage(), is("must be https://vocabs.ardc.edu.au/viewById/316."));
        assertThat(validationFailures.get(0).getErrorType(), is("invalidValue"));
        assertThat(validationFailures.get(0).getFieldId(), is("subject[0].schemaUri"));
    }

    @Test
    @DisplayName("Keyword validation failures are returned")
    void addsKeywordFailures() {
        final var idStartsWith = "https://linked.data.gov.au/def/anzsrc-for/2020/";
        final var subjectId = "222222";
        final var id = idStartsWith.concat(subjectId);
        final var keyword = new SubjectKeyword();
        final var schemaId = 1;

        final var subject = new Subject()
                .id(id)
                .schemaUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri())
                .keyword(List.of(new SubjectKeyword()));

        final var subjectTypeSchemaRecord = new SubjectTypeSchemaRecord();
        subjectTypeSchemaRecord.setId(schemaId);
        subjectTypeSchemaRecord.setUri(SchemaValues.SUBJECT_SCHEMA_URI.getUri());
        subjectTypeSchemaRecord.setIdStartsWith(idStartsWith);

        final var failure = new ValidationFailure();

        when(keywordValidator.validate(keyword,0,0)).thenReturn(List.of(failure));
        when(subjectTypeRepository.findByIdAndSchemaId(subjectId, schemaId)).thenReturn(Optional.of(new SubjectTypeRecord()));
        when(subjectTypeSchemaRepository.findAllActive()).thenReturn(List.of(subjectTypeSchemaRecord));

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures, is(List.of(failure)));
    }
}