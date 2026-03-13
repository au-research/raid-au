package au.org.raid.api.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DateRangeValidatorTest {

    /**
     * Simple test record to use with the generic hasOverlaps method
     */
    record DateRange(String startDate, String endDate) {}

    @Nested
    @DisplayName("hasOverlaps with null or small lists")
    class NullAndSmallLists {

        @Test
        @DisplayName("Returns false for null list")
        void nullList() {
            var result = DateRangeValidator.hasOverlaps(
                    null,
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Returns false for empty list")
        void emptyList() {
            var result = DateRangeValidator.hasOverlaps(
                    Collections.emptyList(),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Returns false for single item list")
        void singleItem() {
            var range = new DateRange("2023-01-01", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }
    }

    @Nested
    @DisplayName("hasOverlaps with non-overlapping ranges")
    class NonOverlappingRanges {

        @Test
        @DisplayName("Returns false for sequential non-overlapping ranges")
        void sequentialRanges() {
            var range1 = new DateRange("2022-01-01", "2022-12-31");
            var range2 = new DateRange("2023-01-01", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Returns false for ranges with gap between them")
        void rangesWithGap() {
            var range1 = new DateRange("2022-01-01", "2022-06-30");
            var range2 = new DateRange("2023-01-01", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Returns false for multiple non-overlapping ranges")
        void multipleNonOverlappingRanges() {
            var range1 = new DateRange("2021-01-01", "2021-12-31");
            var range2 = new DateRange("2022-01-01", "2022-12-31");
            var range3 = new DateRange("2023-01-01", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2, range3),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Returns false when ranges provided out of order")
        void outOfOrderRanges() {
            var range1 = new DateRange("2023-01-01", "2023-12-31");
            var range2 = new DateRange("2021-01-01", "2021-12-31");
            var range3 = new DateRange("2022-01-01", "2022-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2, range3),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }
    }

    @Nested
    @DisplayName("hasOverlaps with overlapping ranges")
    class OverlappingRanges {

        @Test
        @DisplayName("Returns true for clearly overlapping ranges")
        void clearlyOverlappingRanges() {
            var range1 = new DateRange("2023-01-01", "2023-12-31");
            var range2 = new DateRange("2023-06-01", "2024-06-30");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Returns true when one range is completely within another")
        void containedRange() {
            var range1 = new DateRange("2023-01-01", "2023-12-31");
            var range2 = new DateRange("2023-03-01", "2023-06-30");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Returns true when ranges share exact same dates")
        void identicalRanges() {
            var range1 = new DateRange("2023-01-01", "2023-12-31");
            var range2 = new DateRange("2023-01-01", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Returns true when end date equals start date of next range")
        void adjacentDatesSameDay() {
            var range1 = new DateRange("2023-01-01", "2023-06-30");
            var range2 = new DateRange("2023-06-30", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Returns true when only one pair overlaps among multiple ranges")
        void onePairOverlaps() {
            var range1 = new DateRange("2021-01-01", "2021-12-31");
            var range2 = new DateRange("2023-01-01", "2023-12-31");
            var range3 = new DateRange("2023-06-01", "2024-06-30");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2, range3),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }
    }

    @Nested
    @DisplayName("hasOverlaps with different date formats")
    class DateFormatNormalization {

        @Test
        @DisplayName("Handles YYYY format dates")
        void yearOnlyFormat() {
            var range1 = new DateRange("2022", "2022");
            var range2 = new DateRange("2023", "2023");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Handles YYYY-MM format dates")
        void yearMonthFormat() {
            var range1 = new DateRange("2023-01", "2023-06");
            var range2 = new DateRange("2023-07", "2023-12");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }

        @Test
        @DisplayName("Detects overlap with YYYY-MM format dates")
        void yearMonthFormatOverlap() {
            var range1 = new DateRange("2023-01", "2023-06");
            var range2 = new DateRange("2023-06", "2023-12");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Handles mixed date formats")
        void mixedFormats() {
            var range1 = new DateRange("2022", "2022-12-31");
            var range2 = new DateRange("2023-01", "2023-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(false));
        }
    }

    @Nested
    @DisplayName("hasOverlaps with null dates")
    class NullDates {

        @Test
        @DisplayName("Null end date treated as far future - overlaps with subsequent range")
        void nullEndDateOverlaps() {
            var range1 = new DateRange("2023-01-01", null);
            var range2 = new DateRange("2024-01-01", "2024-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Empty end date treated as far future - overlaps with subsequent range")
        void emptyEndDateOverlaps() {
            var range1 = new DateRange("2023-01-01", "");
            var range2 = new DateRange("2024-01-01", "2024-12-31");

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }

        @Test
        @DisplayName("Two ranges with null end dates overlap")
        void bothNullEndDates() {
            var range1 = new DateRange("2023-01-01", null);
            var range2 = new DateRange("2024-01-01", null);

            var result = DateRangeValidator.hasOverlaps(
                    List.of(range1, range2),
                    DateRange::startDate,
                    DateRange::endDate
            );

            assertThat(result, is(true));
        }
    }
}
