package au.org.raid.api.validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class DateRangeValidator {

    /**
     * Validates that no date ranges overlap in the given list
     *
     * @param items List of items to validate
     * @param startDateExtractor Function to extract start date from item
     * @param endDateExtractor Function to extract end date from item
     * @return true if overlaps exist, false otherwise
     */
    public static <T> boolean hasOverlaps(
            List<T> items,
            Function<T, String> startDateExtractor,
            Function<T, String> endDateExtractor) {

        if (items == null || items.size() <= 1) {
            return false;
        }

        // Sort by normalized start date
        List<T> sorted = items.stream()
                .sorted(Comparator.comparing(item ->
                        normalizeDate(startDateExtractor.apply(item))))
                .toList();

        // Check adjacent pairs for overlaps
        for (int i = 0; i < sorted.size() - 1; i++) {
            T current = sorted.get(i);
            T next = sorted.get(i + 1);

            LocalDate currentEnd = normalizeDate(endDateExtractor.apply(current));
            LocalDate nextStart = normalizeDate(startDateExtractor.apply(next));

            // Overlap if current ends on or after next starts
            if (!currentEnd.isBefore(nextStart)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes date strings of form YYYY, YYYY-MM, or YYYY-MM-DD to LocalDate
     * Defaults to first day of month/year if not specified
     */
    private static LocalDate normalizeDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            // If end date is null, assume "far future" to not cause false overlaps
            return LocalDate.MAX;
        }

        String[] parts = dateString.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = (parts.length > 1) ? Integer.parseInt(parts[1]) : 1;
        int day = (parts.length > 2) ? Integer.parseInt(parts[2]) : 1;

        return LocalDate.of(year, month, day);
    }
}