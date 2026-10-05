package au.org.raid.api.service.webarchive;

import au.org.raid.api.validator.AbstractUriValidator;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.regex.Pattern;

import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;

/**
 * Existence check for {@code web.archive.org} relatedObject URLs (RAID-788, reworked in RAID-885).
 * <p>
 * The check is a HEAD on the user-supplied playback link itself, via
 * {@link AbstractUriValidator}: 2xx/3xx means the page is in the archive, 404 means it is not
 * ({@code uri not found}), and any other client error, server error or timeout means the archive
 * could not answer, so it is a 503 rather than a verdict on the URI (RAID-809). A playback URL
 * with an inexact timestamp answers 302 to the closest capture, which preserves the "accept the
 * closest snapshot" rule; the validator's {@code RestTemplate} does not follow HEAD redirects, so
 * the 3xx itself is the success signal.
 * <p>
 * This used to call the Wayback availability API ({@code archive.org/wayback/available}). It was
 * abandoned because that API answers {@code HTTP 200 {"archived_snapshots":{}}} both for a page
 * that was never archived and when its own internal CDX lookup times out, so a genuinely archived
 * page intermittently failed with {@code uri not found} (RAID-885). The playback URL has been
 * observed to answer 200/302 for an archived page and 404 for a never-archived one, with the
 * failures we saw surfacing as error statuses (429, 5xx) or timeouts, all of which are 503 here.
 * That is observed behaviour, not something the Internet Archive documents. Two consequences: an
 * exact capture that was itself recorded as a 404 replays as 404 and so is treated as not found,
 * and excluded URLs or captures recorded as 5xx would surface as 503.
 * <p>
 * What stays local, before any HTTP call, is the format check (with its web-archive specific
 * message) and the plausibility check on the capture timestamp's year. Links are percent-encoded
 * before sending, see {@link #encodeIllegalCharacters}.
 */
public class WebArchiveService extends AbstractUriValidator {
    protected static final Pattern WEB_ARCHIVE_URL_PATTERN =
            Pattern.compile("https://web\\.archive\\.org/web/\\d{14}/https?://.+");

    protected static final String INVALID_WEB_ARCHIVE_URL_MESSAGE =
            "Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)";

    /* first 14 chars of the Wayback path are the capture timestamp, yyyyMMddHHmmss */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("/web/(\\d{14})/");
    private static final int EARLIEST_PLAUSIBLE_YEAR = 1996;

    /* ASCII characters legal in a URI as supplied: unreserved plus reserved, minus [] (illegal outside a host) */
    private static final String LEGAL_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#@!$&'()*+,;=";

    private final RestTemplate restTemplate;
    private final Clock clock;

    public WebArchiveService(final RestTemplate restTemplate, final Clock clock) {
        this.restTemplate = restTemplate;
        this.clock = clock;
    }

    @Override
    protected String getRegex() {
        return WEB_ARCHIVE_URL_PATTERN.pattern();
    }

    @Override
    protected RestTemplate getRestTemplate() {
        return restTemplate;
    }

    @Override
    protected String resolverName() {
        return "Web Archive";
    }

    /**
     * The format check (with its web-archive specific message and errorType, rather than the
     * generic "should match <regex>" one) and the capture-year plausibility check, neither of
     * which makes an HTTP call. {@link AbstractUriValidator#validate} runs this before the HEAD,
     * and RAID-935 runs it on its own for a link already confirmed against the archive.
     */
    @Override
    public List<ValidationFailure> validateLocally(final String uri, final String fieldId) {
        if (!hasValidFormat(uri)) {
            return List.of(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType("invalid")
                    .message(INVALID_WEB_ARCHIVE_URL_MESSAGE));
        }

        final var yearFailure = checkPlausibleYear(extractTimestamp(uri), fieldId);
        if (yearFailure != null) {
            return List.of(yearFailure);
        }

        return List.of();
    }

    /**
     * Sends the HEAD to a {@link URI} rather than a String (RAID-854). The user's link may
     * contain percent-encoded characters (e.g. {@code %3A} in the archived url), and
     * {@code RestTemplate}'s String overloads, {@code RequestEntity.head(String)} included, treat
     * their argument as a URI <em>template</em> and encode it again, turning {@code %3A} into
     * {@code %253A}. The archive would then be asked about a url it has never seen and answer
     * 404. Handing over a {@code URI} skips template expansion and sends the bytes as supplied.
     */
    @Override
    protected RequestEntity<Void> headRequest(final String resolverUri) {
        return RequestEntity.head(URI.create(encodeIllegalCharacters(resolverUri))).build();
    }

    /**
     * True when the link matches {@link #WEB_ARCHIVE_URL_PATTERN} and, once encoded, can be
     * parsed as a URI. The parse is a defensive guard so an unforeseen character is a format
     * failure rather than a 500 from {@code URI.create}.
     */
    protected boolean hasValidFormat(final String uri) {
        if (!WEB_ARCHIVE_URL_PATTERN.matcher(uri).matches()) {
            return false;
        }
        try {
            URI.create(encodeIllegalCharacters(uri));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Percent-encodes (UTF-8) every character that is not legal in a URI - non-ASCII (the JDK
     * would otherwise send raw UTF-8 bytes, which the archive answers with 400) and
     * {@code space " < > \ ^ ` { | } [ ]} - while leaving existing {@code %XX} escapes
     * untouched. A {@code %} not followed by two hex digits becomes {@code %25}. Idempotent.
     */
    protected static String encodeIllegalCharacters(final String uri) {
        final var out = new StringBuilder(uri.length());
        for (int i = 0; i < uri.length(); ) {
            final int cp = uri.codePointAt(i);
            final int len = Character.charCount(cp);
            if (cp == '%') {
                final boolean escape = i + 2 < uri.length()
                        && isHex(uri.charAt(i + 1)) && isHex(uri.charAt(i + 2));
                out.append(escape ? "%" : "%25");
            } else if (cp < 128 && LEGAL_CHARS.indexOf(cp) >= 0) {
                out.append((char) cp);
            } else {
                for (final byte b : new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8)) {
                    out.append('%').append(String.format("%02X", b & 0xFF));
                }
            }
            i += len;
        }
        return out.toString();
    }

    private static boolean isHex(final char c) {
        return Character.digit(c, 16) >= 0 && c < 128;
    }

    /**
     * Extracts the 14-digit Wayback capture timestamp from an already format-validated
     * (see {@link #WEB_ARCHIVE_URL_PATTERN}) URI, e.g. "20220101000000" from
     * "https://web.archive.org/web/20220101000000/https://example.com".
     * <p>
     * Precondition: only call this after the uri has matched {@link #WEB_ARCHIVE_URL_PATTERN}
     * (as {@link #validateLocally} does before calling it). That guarantees {@code matcher.find()}
     * succeeds, so the result isn't checked here.
     */
    protected String extractTimestamp(final String uri) {
        final var matcher = TIMESTAMP_PATTERN.matcher(uri);
        matcher.find();
        return matcher.group(1);
    }

    /**
     * Rejects capture timestamps whose year is implausible - before the Wayback Machine existed
     * (1996) or after today - without making any HTTP call. Returns null when the year is
     * plausible.
     */
    protected ValidationFailure checkPlausibleYear(final String timestamp, final String fieldId) {
        final var year = Integer.parseInt(timestamp.substring(0, 4));
        final var currentYear = Year.now(clock).getValue();

        if (year < EARLIEST_PLAUSIBLE_YEAR || year > currentYear) {
            return new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message("web archive timestamp year %d is implausible".formatted(year));
        }

        return null;
    }
}
