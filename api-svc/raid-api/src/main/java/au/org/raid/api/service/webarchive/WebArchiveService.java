package au.org.raid.api.service.webarchive;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.validator.UriValidator;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.regex.Pattern;

import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;
import static au.org.raid.api.endpoint.message.ValidationMessage.URI_DOES_NOT_EXIST;
import static au.org.raid.api.exception.ResolverUnavailableException.toUnavailableResolver;

/**
 * Existence check for {@code web.archive.org} relatedObject URLs against the Wayback Machine
 * "availability" API (RAID-788).
 * <p>
 * This deliberately implements {@link UriValidator} directly rather than extending
 * {@link au.org.raid.api.validator.AbstractUriValidator}. The base class's shared machinery is
 * built around a HEAD request that either succeeds, 404s, or errors - i.e. HTTP status carries
 * the existence signal. The Wayback availability API is different in every respect that matters
 * here: it's a GET, it returns a JSON body (not a bare status), and it always responds
 * HTTP 200 even when there's no archived snapshot (an empty {@code archived_snapshots} object
 * means "not found"). None of AbstractUriValidator's HEAD/404/503 machinery applies, so
 * reusing it would mean overriding away most of what it does. What is reused is the
 * {@link UriValidator} contract, the shared {@code uriValidatorRestTemplate} bean, and
 * {@link ResolverUnavailableException} for the fail-closed RAID-809 pattern.
 */
@Slf4j
public class WebArchiveService implements UriValidator {
    protected static final Pattern WEB_ARCHIVE_URL_PATTERN =
            Pattern.compile("https://web\\.archive\\.org/web/\\d{14}/https?://.+");

    protected static final String INVALID_WEB_ARCHIVE_URL_MESSAGE =
            "Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)";

    /* first 14 chars of the Wayback path are the capture timestamp, yyyyMMddHHmmss */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("/web/(\\d{14})/");
    private static final Pattern ORIGINAL_URL_PATTERN = Pattern.compile("/web/\\d{14}/(.+)$");
    private static final int EARLIEST_PLAUSIBLE_YEAR = 1996;

    private final RestTemplate restTemplate;
    private final Clock clock;
    private final String availabilityUrl;

    public WebArchiveService(final RestTemplate restTemplate, final Clock clock, final String availabilityUrl) {
        this.restTemplate = restTemplate;
        this.clock = clock;
        this.availabilityUrl = availabilityUrl;
    }

    protected String resolverName() {
        return "Web Archive";
    }

    @Override
    public List<ValidationFailure> validate(final String uri, final String fieldId) {
        if (!WEB_ARCHIVE_URL_PATTERN.matcher(uri).matches()) {
            return List.of(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType("invalid")
                    .message(INVALID_WEB_ARCHIVE_URL_MESSAGE));
        }

        final var timestamp = extractTimestamp(uri);
        final var yearFailure = checkPlausibleYear(timestamp, fieldId);
        if (yearFailure != null) {
            return List.of(yearFailure);
        }

        final var originalUrl = extractOriginalUrl(uri);
        final var requestUrl = buildAvailabilityUrl(originalUrl);

        try {
            final var response = restTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class);
            final var body = response.getBody();

            if (snapshotExists(body)) {
                return List.of();
            }

            return List.of(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message(URI_DOES_NOT_EXIST));
        } catch (HttpClientErrorException e) {
            // The Wayback availability API itself returning a client error means the resolver,
            // not the URI, is at fault (RAID-809) - it doesn't use 404 to mean "no snapshot"
            // (that's communicated via an empty archived_snapshots in a 200 body instead).
            log.error("Request failed during Web Archive URI validation", e);
            throw new ResolverUnavailableException(
                    List.of(toUnavailableResolver(fieldId, uri, resolverName(), e)));
        } catch (RestClientException e) {
            // Covers ResourceAccessException (connect/read timeout, DNS failure, connection
            // refused), HttpServerErrorException (5xx) and, defensively, any other
            // RestClientException.
            log.error("External resolver check failed during Web Archive URI validation of {}", uri, e);
            throw new ResolverUnavailableException(
                    List.of(toUnavailableResolver(fieldId, uri, resolverName(), e)));
        }
    }

    /**
     * Builds the availability request as a {@link URI} rather than a String (RAID-854).
     * <p>
     * The archived url has to be percent-encoded to survive as a single query parameter value,
     * but {@code RestTemplate}'s String overloads treat their argument as a URI <em>template</em>
     * and encode it again, turning {@code %3A} into {@code %253A}. The Wayback availability API
     * then sees a url it has no snapshot of and answers 200 with an empty
     * {@code archived_snapshots}, which this validator reported as "uri not found" for every
     * genuinely archived page. Handing {@code RestTemplate} a {@code URI} skips template
     * expansion and sends the encoding built here verbatim.
     * <p>
     * No {@code timestamp} parameter is sent (RAID-854). The question this validator asks is
     * "is this page in the archive at all", not "is there a capture at exactly this instant" -
     * it already accepts whatever snapshot the API calls {@code closest}, however far away, and
     * the capture timestamp's plausibility is checked locally in
     * {@link #checkPlausibleYear}. Passing the timestamp only narrowed the lookup, and it
     * narrowed it wrongly: when the requested timestamp lands on a {@code warc/revisit} record
     * (a de-duplication pointer to an identical earlier capture, carrying no status code of its
     * own) the API answers 200 with an empty {@code archived_snapshots} - reporting "not found"
     * for a page it demonstrably holds. That is exactly the case in HELP-3170: the CDX index
     * lists the reported capture as {@code warc/revisit} with statuscode {@code -}, the
     * timestamped query returns nothing, and the untimestamped query returns a valid snapshot.
     */
    private URI buildAvailabilityUrl(final String originalUrl) {
        final var encodedUrl = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
        return URI.create("%s?url=%s".formatted(availabilityUrl, encodedUrl));
    }

    private boolean snapshotExists(final JsonNode body) {
        if (body == null) {
            return false;
        }

        final var closest = body.path("archived_snapshots").path("closest");

        if (closest.isMissingNode() || closest.isEmpty()) {
            return false;
        }

        final var available = closest.path("available").asBoolean(false);
        final var status = closest.path("status").asText("");

        return available && (status.startsWith("2") || status.startsWith("3"));
    }

    /**
     * Extracts the 14-digit Wayback capture timestamp from an already format-validated
     * (see {@link #WEB_ARCHIVE_URL_PATTERN}) URI, e.g. "20220101000000" from
     * "https://web.archive.org/web/20220101000000/https://example.com".
     * <p>
     * Precondition: only call this after the uri has matched {@link #WEB_ARCHIVE_URL_PATTERN}
     * (as {@link #validate} does before calling it). That guarantees {@code matcher.find()}
     * succeeds, so the result isn't checked here.
     */
    protected String extractTimestamp(final String uri) {
        final var matcher = TIMESTAMP_PATTERN.matcher(uri);
        matcher.find();
        return matcher.group(1);
    }

    /**
     * Extracts the original (archived) URL from an already format-validated uri, e.g.
     * "https://example.com" from
     * "https://web.archive.org/web/20220101000000/https://example.com".
     * <p>
     * Precondition: only call this after the uri has matched {@link #WEB_ARCHIVE_URL_PATTERN}
     * (as {@link #validate} does before calling it). That guarantees {@code matcher.find()}
     * succeeds, so the result isn't checked here.
     */
    protected String extractOriginalUrl(final String uri) {
        final var matcher = ORIGINAL_URL_PATTERN.matcher(uri);
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
