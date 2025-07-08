package au.org.raid.api.validator;

import au.org.raid.api.repository.SubjectTypeRepository;
import au.org.raid.api.repository.dto.SubjectTypeWithSchema;
import au.org.raid.idl.raidv2.model.Subject;
import au.org.raid.idl.raidv2.model.SubjectKeyword;
import au.org.raid.idl.raidv2.model.SubjectSchemaURIEnum;
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
    private static final SubjectSchemaURIEnum SCHEMA_URI = SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316;

    @Mock
    private SubjectTypeRepository subjectTypeRepository;

    @Mock
    private SubjectKeywordValidator keywordValidator;

    @InjectMocks
    private SubjectValidator validationService;

    @Test
    void noFailuresWithValidCode() {
        final var id = "https://linked.data.gov.au/def/anzsrc-for/2020/222222";
        final var keyword = new SubjectKeyword();

        final var subject = new Subject()
                .id(id)
                .schemaUri(SCHEMA_URI)
                .keyword(List.of(new SubjectKeyword()));

        when(keywordValidator.validate(keyword,0,0)).thenReturn(Collections.emptyList());
        when(subjectTypeRepository.findBySubjectTypeIdAndSchemaUri("222222", SCHEMA_URI.getValue()))
                .thenReturn(Optional.of(new SubjectTypeWithSchema()));

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures, empty());
    }

    @Test
    void noFailuresIfSubjectBlockIsNull() {
        final List<ValidationFailure> validationFailures = validationService.validate(null);
        assertThat(validationFailures, empty());
    }

    @Test
    void returnsFailureWithAlphabeticCharactersInId() {
        final var id = "https://linked.data.gov.au/def/anzsrc-for/2020/22a222";

        final var subject = new Subject()
                .id(id)
                .schemaUri(SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316);

        final List<ValidationFailure> failures = validationService.validate(Collections.singletonList(subject));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].id")
                        .errorType("invalidValue")
                        .message(String.format("%s is not a valid field of research", id))
        )));

        verifyNoInteractions(subjectTypeRepository);
    }

    @Test
    void returnsFailureWithInvalidUrlPrefix() {
        final var id = "https://data.gov.au/def/anzsrc-for/2020/222222";

        final var subject = new Subject()
                .id(id)
                .schemaUri(SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316);

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].id")
                        .errorType("invalidValue")
                        .message(String.format("%s is not a valid field of research", id))
        )));
        verifyNoInteractions(subjectTypeRepository);
    }

    @Test
    void returnsFailureIfCodeNotFound() {
        final var id = "https://linked.data.gov.au/def/anzsrc-for/2020/222222";

        final var subject = new Subject()
                .id(id)
                .schemaUri(SCHEMA_URI);

        when(subjectTypeRepository.findBySubjectTypeIdAndSchemaUri("222222", SCHEMA_URI.getValue())).thenReturn(Optional.empty());

        final List<ValidationFailure> failures = validationService.validate(Collections.singletonList(subject));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].id")
                        .errorType("invalidValue")
                        .message(String.format("%s is not a standard FoR code", id))
        )));
    }

    @Test
    void addsFailureWithInvalidMissingSubjectSchemeUri() {
        final var id = "https://linked.data.gov.au/def/anzsrc-for/2020/222222";

        final var subject = new Subject()
                .id(id);

        final List<ValidationFailure> failures = validationService.validate(Collections.singletonList(subject));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("subject[0].schemaUri")
                        .errorType("invalidValue")
                        .message("must be https://vocabs.ardc.edu.au/viewById/316.")
        )));

        verifyNoInteractions(subjectTypeRepository);
    }

    @Test
    @DisplayName("Keyword validation failures are returned")
    void addsKeywordFailures() {
        final var id = "https://linked.data.gov.au/def/anzsrc-for/2020/222222";
        final var keyword = new SubjectKeyword();

        final var subject = new Subject()
                .id(id)
                .schemaUri(SCHEMA_URI)
                .keyword(List.of(new SubjectKeyword()));

        final var failure = new ValidationFailure();

        when(keywordValidator.validate(keyword,0,0)).thenReturn(List.of(failure));
        when(subjectTypeRepository.findBySubjectTypeIdAndSchemaUri("222222", SCHEMA_URI.getValue()))
                .thenReturn(Optional.of(new SubjectTypeWithSchema()));

        final List<ValidationFailure> validationFailures = validationService.validate(Collections.singletonList(subject));

        assertThat(validationFailures, is(List.of(failure)));
    }
}