package au.org.raid.orcidupdater.orcid;

import java.util.regex.Pattern;

/**
 * Removes ORCID iDs from text that reaches the log or a stored failure, such as an error message
 * from ORCID or a URL in a connection error. Logs record what happened, not whose record it was.
 */
public final class OrcidIdRedactor {
    static final String REPLACEMENT = "[ORCID iD]";
    private static final Pattern ORCID_ID = Pattern.compile("\\b\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]\\b");

    private OrcidIdRedactor() {
    }

    /** @return the text with every ORCID iD replaced, or null for null */
    public static String redact(final String text) {
        return text == null ? null : ORCID_ID.matcher(text).replaceAll(REPLACEMENT);
    }
}
