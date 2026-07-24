package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.Date;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class DateValidatorTest {
    private final DateValidator validator = new DateValidator();

    @Test
    @DisplayName("Validation passes with valid date - year only")
    void validWithYearOnly() {
        final var date = new Date()
                .startDate("2021")
                .endDate("2022");

        final var failures = validator.validate(date);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation passes with valid date - year and month")
    void validWithYearAndMonth() {
        final var date = new Date()
                .startDate("2021-03")
                .endDate("2022-09");

        final var failures = validator.validate(date);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation passes with valid date - year, month and day")
    void validWithYearMonthAndDay() {
        final var date = new Date()
                .startDate("2021-03-01")
                .endDate("2022-09-21");

        final var failures = validator.validate(date);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation passes with missing end date")
    void validMissingEndDate() {
        final var date = new Date()
                .startDate("2021-03-01");

        final var failures = validator.validate(date);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation passes with empty string end date")
    void validEmptyStringEndDate() {
        final var date = new Date()
                .startDate("2021-03-01")
                .endDate("");

        final var failures = validator.validate(date);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Validation fails if date null")
    void notSet() {
        final var failures = validator.validate(null);
        assertThat(failures, is(List.of(new ValidationFailure()
                .fieldId("date")
                .errorType("notSet")
                .message("field must be set")
        )));
    }

    @Test
    @DisplayName("Validation fails if end date is before start date")
    void endBeforeStart() {
        final var date = new Date()
                .startDate("2022-03-01")
                .endDate("2021-09-21");

        final var failures = validator.validate(date);

        assertThat(failures, is(List.of(new ValidationFailure()
                .fieldId("date.endDate")
                .errorType("invalidValue")
                .message("end date is before start date")
        )));
    }
}