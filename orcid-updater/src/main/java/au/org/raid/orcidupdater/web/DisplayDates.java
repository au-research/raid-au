package au.org.raid.orcidupdater.web;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Dates as researchers read them in emails and pages, e.g. "9 November 2026" (UTC). */
public final class DisplayDates {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("en-AU")).withZone(ZoneOffset.UTC);

    private DisplayDates() {
    }

    public static String format(final Instant instant) {
        return FORMAT.format(instant);
    }
}
