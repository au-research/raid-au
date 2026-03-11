package au.org.raid.api.validator;

import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.AccessStatement;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessStatementValidatorTest {
    @Mock
    private LanguageValidator languageValidator;

    @InjectMocks
    private AccessStatementValidator validationService;

    @Test
    @DisplayName("Valid access statement returns no errors")
    void validAccessStatement() {
        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(TestConstants.LANGUAGE_SCHEMA_URI);

        when(languageValidator.validate(language, "access.statement"))
                .thenReturn(Collections.emptyList());

        final var accessStatement = new AccessStatement()
                .text("Embargoed")
                .language(language);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, empty());
        verify(languageValidator).validate(language, "access.statement");
    }

    @Test
    @DisplayName("Fails validation with null statement")
    void nullAccessStatement() {

        final var failures = validationService.validate(null);

        assertThat(failures, is(
                List.of(new ValidationFailure()
                        .fieldId("access.statement")
                        .errorType("notSet")
                        .message("field must be set"))));
        verifyNoInteractions(languageValidator);
    }

    @Test
    @DisplayName("Fails validation with null statement")
    void nullStatement() {
        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(TestConstants.LANGUAGE_SCHEMA_URI);

        when(languageValidator.validate(language, "access.statement"))
                .thenReturn(Collections.emptyList());

        final var accessStatement = new AccessStatement()
                .language(language);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, is(
                List.of(new ValidationFailure()
                        .fieldId("access.statement.text")
                        .errorType("notSet")
                        .message("field must be set"))));
        verify(languageValidator).validate(language, "access.statement");
    }

    @Test
    @DisplayName("Language validation failures are returned")
    void invalidLanguage() {
        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(TestConstants.LANGUAGE_SCHEMA_URI);

        final var failure = new ValidationFailure()
                .fieldId("access.statement.language.id")
                .errorType("notSet")
                .message("field must be set");

        when(languageValidator.validate(language, "access.statement"))
                .thenReturn(List.of(failure));

        final var accessStatement = new AccessStatement()
                .text("Embargoed")
                .language(language);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, is(List.of(failure)));
        verify(languageValidator).validate(language, "access.statement");
    }

    @Test
    @DisplayName("No failures when language is null")
    void nullLanguage() {
        when(languageValidator.validate(null, "access.statement"))
                .thenReturn(Collections.emptyList());

        final var accessStatement = new AccessStatement()
                .text("Embargoed")
                .language(null);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, empty());
        verify(languageValidator).validate(null, "access.statement");
    }

    @Test
    @DisplayName("Validates language when id is null but schemaUri is set")
    void nullLanguageId() {
        final var language = new Language()
                .schemaUri(TestConstants.LANGUAGE_SCHEMA_URI);

        final var failure = new ValidationFailure()
                .fieldId("access.statement.language.id")
                .errorType("notSet")
                .message("field must be set");

        when(languageValidator.validate(language, "access.statement"))
                .thenReturn(List.of(failure));

        final var accessStatement = new AccessStatement()
                .text("Embargoed")
                .language(language);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, is(List.of(failure)));
        verify(languageValidator).validate(language, "access.statement");
    }

    @Test
    @DisplayName("Validates language when id is blank")
    void blankLanguageId() {
        final var language = new Language()
                .id("")
                .schemaUri(TestConstants.LANGUAGE_SCHEMA_URI);

        final var failure = new ValidationFailure()
                .fieldId("access.statement.language.id")
                .errorType("notSet")
                .message("field must be set");

        when(languageValidator.validate(language, "access.statement"))
                .thenReturn(List.of(failure));

        final var accessStatement = new AccessStatement()
                .text("Embargoed")
                .language(language);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, is(List.of(failure)));
        verify(languageValidator).validate(language, "access.statement");
    }

    // With:
    @Test
    @DisplayName("Validates language when language is empty object")
    void emptyLanguageObject() {
        final var language = new Language();

        final var failure = new ValidationFailure()
                .fieldId("access.statement.language.id")
                .errorType("notSet")
                .message("field must be set");

        when(languageValidator.validate(language, "access.statement"))
                .thenReturn(List.of(failure));

        final var accessStatement = new AccessStatement()
                .text("Embargoed")
                .language(language);

        final var failures = validationService.validate(accessStatement);

        assertThat(failures, is(List.of(failure)));
        verify(languageValidator).validate(language, "access.statement");
    };
}