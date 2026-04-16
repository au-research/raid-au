package au.org.raid.api.validator;

import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessValidatorTest {
    @Mock
    private AccessStatementValidator accessStatementValidator;

    @Mock
    private AccessTypeValidator accessTypeValidator;

    @InjectMocks
    private AccessValidator validator;

    @Test
    @DisplayName("Validation passes on embargoed raid with correct fields")
    void embargoedValidationSucceeds() {
        final var type = new AccessType()
                .id(TestConstants.EMBARGOED_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);

        final var access = new Access()
                .type(type)
                .statement(new AccessStatement().text("Embargoed"))
                .embargoExpiry(LocalDate.now());

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures, empty());
    }


    @Test
    @DisplayName("Validation fails with closed raid")
    void missingAccessStatement() {
        final var type = new AccessType()
                .id(TestConstants.CLOSED_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);

        final var access = new Access()
                .type(type);

        final var failure = new ValidationFailure()
                .fieldId("access.type.id")
                .errorType("invalidValue")
                .message("Creating closed Raids is no longer supported");

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures, is(List.of(failure)));
    }

    @Test
    @DisplayName("Validation fails with blank accessStatement on embargoed raid")
    void blankStatementEmbargoed() {
        final var type = new AccessType()
                .id(TestConstants.EMBARGOED_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);

        final var accessStatement = new AccessStatement().text("");

        final var access = new Access()
                .type(type)
                .statement(accessStatement)
                .embargoExpiry(LocalDate.now());

        final var failure = new ValidationFailure()
                .fieldId("access.accessStatement")
                .errorType("notSet")
                .message("field must be set");

        when(accessStatementValidator.validate(accessStatement)).thenReturn(List.of(failure));

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures.size(), is(1));
        assertThat(failures, hasItem(failure));
    }

    @Test
    @DisplayName("Validation fails with missing type")
    void missingType() {
        final var access = new Access();

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures.size(), is(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("access.type")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails with missing embargoExpiry on embargoed raid")
    void missingEmbargoExpiry() {
        final var type = new AccessType()
                .id(TestConstants.EMBARGOED_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);

        final var access = new Access()
                .type(type)
                .statement(new AccessStatement().text("access statement"));

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures.size(), is(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("access.embargoExpiry")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails on open raid with invalid access statement")
    void openRaidWithInvalidAccessStatement() {
        final var type = new AccessType()
                .id(TestConstants.OPEN_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);

        final var accessStatement = new AccessStatement()
                .language(new Language()
                        .id("eng")
                        .schemaUri("blah"));

        final var access = new Access()
                .type(type)
                .statement(accessStatement);

        final var failure = new ValidationFailure();

        when(accessStatementValidator.validate(accessStatement)).thenReturn(List.of(failure));

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures, is(List.of(failure)));
        verify(accessTypeValidator).validate(type);
        verify(accessStatementValidator).validate(accessStatement);
    }

    @Test
    @DisplayName("Validation passes on open raid without access statement")
    void openRaidNoAccessStatement() {
        final var type = new AccessType()
                .id(TestConstants.OPEN_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);


        final var access = new Access()
                .type(type);

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures, empty());
        verify(accessTypeValidator).validate(type);
        verifyNoInteractions(accessStatementValidator);
    }

    @Test
    @DisplayName("Validation fails on embargoed raid with embargo expiry over 18 month in future")
    void embargoedInvalidExpiry() {
        final var type = new AccessType()
                .id(TestConstants.EMBARGOED_ACCESS_TYPE_ID)
                .schemaUri(TestConstants.ACCESS_TYPE_SCHEMA_URI);

        final var access = new Access()
                .type(type)
                .statement(new AccessStatement().text("Embargoed"))
                .embargoExpiry(LocalDate.now().plusMonths(19));

        final List<ValidationFailure> failures = validator.validate(access);

        assertThat(failures, is(List.of(new ValidationFailure()
                .fieldId("access.embargoExpiry")
                .errorType("invalidValue")
                .message("Embargo expiry cannot be more than 18 months in the future")
        )));
    }
}