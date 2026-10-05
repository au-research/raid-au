package au.org.raid.api.service.webarchive;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Uses a real RestTemplate bound to MockRestServiceServer (RAID-854/RAID-885): the URL encoding
 * happens inside RestTemplate, so a mocked RestTemplate could not see the wire URI.
 */
class WebArchiveServiceTest {
    private static final String FIELD_ID = "relatedObject[0].id";
    private static final String LINK = "https://web.archive.org/web/20220101000000/https://example.com/page";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final WebArchiveService webArchiveService = new WebArchiveService(restTemplate, clock);

    private void expectHead(final String link, final ResponseCreator response) {
        server.expect(once(), requestTo(URI.create(link)))
                .andExpect(method(HttpMethod.HEAD))
                .andRespond(response);
    }

    @Test
    @DisplayName("200 on the playback link is valid")
    void okIsValid() {
        expectHead(LINK, withSuccess());

        assertThat(webArchiveService.validate(LINK, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("302 to the closest capture is valid")
    void redirectIsValid() {
        expectHead(LINK, withStatus(HttpStatus.FOUND)
                .location(URI.create("https://web.archive.org/web/20220102030405/https://example.com/page")));

        assertThat(webArchiveService.validate(LINK, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("404 returns uri not found")
    void notFound() {
        expectHead(LINK, withStatus(HttpStatus.NOT_FOUND));

        final var failures = webArchiveService.validate(LINK, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
        server.verify();
    }

    @Test
    @DisplayName("429 throws ResolverUnavailableException")
    void tooManyRequests() {
        expectHead(LINK, withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertUnavailable(LINK, 429);
    }

    @Test
    @DisplayName("500 throws ResolverUnavailableException")
    void serverError() {
        expectHead(LINK, withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertUnavailable(LINK, 500);
    }

    @Test
    @DisplayName("503 throws ResolverUnavailableException")
    void serviceUnavailable() {
        expectHead(LINK, withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertUnavailable(LINK, 503);
    }

    @Test
    @DisplayName("Timeout throws ResolverUnavailableException with resolver name Web Archive")
    void timeout() {
        expectHead(LINK, request -> {
            throw new ResourceAccessException("Read timed out");
        });

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> webArchiveService.validate(LINK, FIELD_ID));

        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("Web Archive"));
        assertThat(e.getUnavailableResolvers().get(0).getField(), is(FIELD_ID));
        server.verify();
    }

    @Test
    @DisplayName("RAID-854: percent-encoded characters in the link are sent verbatim, not re-encoded")
    void percentEncodingIsNotDoubleEncoded() {
        final var link = "https://web.archive.org/web/20220101000000/https://example.com/a%20b%3Ax%2Fy";
        expectHead(link, withSuccess());

        assertThat(webArchiveService.validate(link, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("301, 307 and 308 are valid")
    void otherRedirectsAreValid() {
        for (final var status : List.of(HttpStatus.MOVED_PERMANENTLY, HttpStatus.TEMPORARY_REDIRECT,
                HttpStatus.PERMANENT_REDIRECT)) {
            server.reset();
            expectHead(LINK, withStatus(status).location(URI.create("https://web.archive.org/web/2/x")));

            assertThat(webArchiveService.validate(LINK, FIELD_ID), empty());
            server.verify();
        }
    }

    @Test
    @DisplayName("Characters illegal in a URI (space, pipe, brace, non-ASCII) are encoded, not rejected")
    void illegalCharactersAreEncoded() {
        final var link = "https://web.archive.org/web/20220101000000/https://example.com/a b|c{d}/M\u00fcnchen";
        expectHead("https://web.archive.org/web/20220101000000/https://example.com/a%20b%7Cc%7Bd%7D/M%C3%BCnchen",
                withSuccess());

        assertThat(webArchiveService.validate(link, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("Existing escapes are untouched and a bare percent becomes %25")
    void encoderHandlesPercent() {
        assertThat(WebArchiveService.encodeIllegalCharacters("/a%3Ab%20c"), is("/a%3Ab%20c"));
        assertThat(WebArchiveService.encodeIllegalCharacters("/100%/x%zz/y%4"), is("/100%25/x%25zz/y%254"));
        assertThat(WebArchiveService.encodeIllegalCharacters("/a%25b"), is("/a%25b"));
        assertThat(WebArchiveService.encodeIllegalCharacters("/\u00fc\ud83d\ude00"), is("/%C3%BC%F0%9F%98%80"));
    }

    @Test
    @DisplayName("A bare percent in the link is sent as %25")
    void barePercentIsEncodedOnTheWire() {
        final var link = "https://web.archive.org/web/20220101000000/https://example.com/100%/x";
        expectHead("https://web.archive.org/web/20220101000000/https://example.com/100%25/x", withSuccess());

        assertThat(webArchiveService.validate(link, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("Malformed URL fails format validation without an HTTP call")
    void malformedUrlFormatFailure() {
        assertThat(webArchiveService.validate("https://web.archive.org/foo/bar", FIELD_ID), is(formatFailure()));
        server.verify();
    }

    @Test
    @DisplayName("Year too old fails without an HTTP call")
    void yearTooOldNoHttpCall() {
        final var failures = webArchiveService.validate(
                "https://web.archive.org/web/14062026010101/https://example.com", FIELD_ID);

        assertThat(failures, is(yearFailure(1406)));
        server.verify();
    }

    @Test
    @DisplayName("Year 1995 is rejected with no HTTP call")
    void year1995Rejected() {
        final var failures = webArchiveService.validate(
                "https://web.archive.org/web/19950101000000/https://example.com", FIELD_ID);

        assertThat(failures, is(yearFailure(1995)));
        server.verify();
    }

    @Test
    @DisplayName("Year in the future fails without an HTTP call")
    void yearInFutureNoHttpCall() {
        final var failures = webArchiveService.validate(
                "https://web.archive.org/web/21000101000000/https://example.com", FIELD_ID);

        assertThat(failures, is(yearFailure(2100)));
        server.verify();
    }

    @Test
    @DisplayName("Year 1996 is the earliest plausible year and passes the year check")
    void year1996Passes() {
        final var link = "https://web.archive.org/web/19960101000000/https://example.com";
        expectHead(link, withSuccess());

        assertThat(webArchiveService.validate(link, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("validateLocally accepts a well-formed link with no HTTP call")
    void validateLocallyValidNoHttpCall() {
        assertThat(webArchiveService.validateLocally(LINK, FIELD_ID), empty());
        server.verify();
    }

    @Test
    @DisplayName("validateLocally reports the format failure with no HTTP call")
    void validateLocallyFormatFailureNoHttpCall() {
        assertThat(webArchiveService.validateLocally("https://web.archive.org/foo/bar", FIELD_ID),
                is(formatFailure()));
        server.verify();
    }

    @Test
    @DisplayName("validateLocally reports an implausible year with no HTTP call")
    void validateLocallyYearFailureNoHttpCall() {
        assertThat(webArchiveService.validateLocally(
                "https://web.archive.org/web/19950101000000/https://example.com", FIELD_ID),
                is(yearFailure(1995)));
        server.verify();
    }

    @Test
    @DisplayName("Over a real connection a HEAD 302 is not followed, is valid, and the path stays single-encoded")
    void realConnectionDoesNotFollowRedirect() throws IOException {
        final var rawRequestUri = new AtomicReference<String>();
        final var requestMethod = new AtomicReference<String>();
        final var httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            rawRequestUri.set(exchange.getRequestURI().getRawPath());

            requestMethod.set(exchange.getRequestMethod());
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:1/never-followed");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        httpServer.start();
        try {
            final var factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(2000);
            final var service = new WebArchiveService(new RestTemplate(factory), clock) {
                @Override
                protected RequestEntity<Void> headRequest(final String resolverUri) {
                    // redirect the archive host to the local server, keeping the path bytes as given
                    final var local = "http://127.0.0.1:" + httpServer.getAddress().getPort()
                            + URI.create(resolverUri).getRawPath();
                    return super.headRequest(local);
                }
            };

            final var failures = service.validate(
                    "https://web.archive.org/web/20220101000000/https://example.com/a%3Ab/M\u00fcnchen", FIELD_ID);

            assertThat(failures, empty());
            assertThat(requestMethod.get(), is("HEAD"));
            assertThat(StandardCharsets.US_ASCII.newEncoder().canEncode(rawRequestUri.get()), is(true));
            assertThat(rawRequestUri.get(), is("/web/20220101000000/https://example.com/a%3Ab/M%C3%BCnchen"));
        } finally {
            httpServer.stop(0);
        }
    }

    private void assertUnavailable(final String link, final int status) {
        final var e = assertThrows(ResolverUnavailableException.class,
                () -> webArchiveService.validate(link, FIELD_ID));

        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("Web Archive"));
        assertThat(e.getUnavailableResolvers().get(0).getDownstreamStatus(), is(status));
        server.verify();
    }

    private static List<ValidationFailure> formatFailure() {
        return List.of(new ValidationFailure()
                .fieldId(FIELD_ID)
                .errorType("invalid")
                .message(WebArchiveService.INVALID_WEB_ARCHIVE_URL_MESSAGE));
    }

    private static List<ValidationFailure> yearFailure(final int year) {
        return List.of(new ValidationFailure()
                .fieldId(FIELD_ID)
                .errorType("invalidValue")
                .message("web archive timestamp year %d is implausible".formatted(year)));
    }
}
