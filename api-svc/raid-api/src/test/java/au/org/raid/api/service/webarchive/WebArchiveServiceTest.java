package au.org.raid.api.service.webarchive;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebArchiveServiceTest {
    private static final String AVAILABILITY_URL = "https://archive.org/wayback/available";

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebArchiveService webArchiveService =
            new WebArchiveService(restTemplate, clock, AVAILABILITY_URL);

    private static final String FIELD_ID = "relatedObject[0].id";

    private JsonNode json(final String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private void stubResponse(final JsonNode body) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    @Test
    @DisplayName("Year too old (implausible timestamp) fails without an HTTP call")
    void yearTooOldNoHttpCall() {
        final var uri = "https://web.archive.org/web/14062026010101/https://example.com";

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("web archive timestamp year 1406 is implausible")
        )));
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    @DisplayName("Year in the future fails without an HTTP call")
    void yearInFutureNoHttpCall() {
        final var uri = "https://web.archive.org/web/21000101000000/https://example.com";

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("web archive timestamp year 2100 is implausible")
        )));
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    @DisplayName("Year 1996 is the earliest plausible year and passes the year check")
    void year1996Passes() throws Exception {
        final var uri = "https://web.archive.org/web/19960101000000/https://example.com";
        stubResponse(json("""
                {"archived_snapshots":{"closest":{"available":true,"status":"200"}}}
                """));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Year 1995 is rejected with no HTTP call")
    void year1995Rejected() {
        final var uri = "https://web.archive.org/web/19950101000000/https://example.com";

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("web archive timestamp year 1995 is implausible")
        )));
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    @DisplayName("Malformed URL fails format validation without an HTTP call")
    void malformedUrlFormatFailure() {
        final var uri = "https://web.archive.org/foo/bar";

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalid")
                        .message("Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)")
        )));
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(JsonNode.class));
    }

    @Test
    @DisplayName("Valid snapshot passes")
    void validSnapshotPasses() throws Exception {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        stubResponse(json("""
                {"archived_snapshots":{"closest":{"available":true,"status":"200"}}}
                """));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("No snapshot present returns URI_DOES_NOT_EXIST")
    void noSnapshot() throws Exception {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        stubResponse(json("""
                {"archived_snapshots":{}}
                """));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("Snapshot present but status 404 returns URI_DOES_NOT_EXIST")
    void snapshotStatus404() throws Exception {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        stubResponse(json("""
                {"archived_snapshots":{"closest":{"available":true,"status":"404"}}}
                """));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("Snapshot available=false returns URI_DOES_NOT_EXIST")
    void snapshotNotAvailable() throws Exception {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        stubResponse(json("""
                {"archived_snapshots":{"closest":{"available":false,"status":"200"}}}
                """));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("A 3xx snapshot status is treated as existing")
    void threeXxSnapshotStatusPasses() throws Exception {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        stubResponse(json("""
                {"archived_snapshots":{"closest":{"available":true,"status":"302"}}}
                """));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("Timeout throws ResolverUnavailableException with resolver name Web Archive")
    void timeoutThrowsResolverUnavailable() {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(JsonNode.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> webArchiveService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), org.hamcrest.Matchers.hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("Web Archive"));
    }

    @Test
    @DisplayName("5xx response throws ResolverUnavailableException")
    void serverErrorThrowsResolverUnavailable() {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(JsonNode.class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatusCode.valueOf(503), "Service Unavailable", null, null, null));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> webArchiveService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("Web Archive"));
        assertThat(e.getUnavailableResolvers().get(0).getDownstreamStatus(), is(503));
    }

    @Test
    @DisplayName("Non-404 4xx response throws ResolverUnavailableException")
    void nonNotFoundClientErrorThrowsResolverUnavailable() {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(JsonNode.class)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(403)));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> webArchiveService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("Web Archive"));
        assertThat(e.getUnavailableResolvers().get(0).getDownstreamStatus(), is(403));
    }

    @Test
    @DisplayName("The original url query param is URL-encoded when it contains reserved characters")
    void originalUrlIsEncodedInAvailabilityRequest() throws Exception {
        final var originalUrl = "https://example.com/path?a=b&c=d";
        final var uri = "https://web.archive.org/web/20220101000000/" + originalUrl;
        stubResponse(json("""
                {"archived_snapshots":{"closest":{"available":true,"status":"200"}}}
                """));

        webArchiveService.validate(uri, FIELD_ID);

        final var captor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(captor.capture(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(JsonNode.class));
        final var requestUrl = captor.getValue();

        assertThat(requestUrl, is(
                "https://archive.org/wayback/available?url=https%3A%2F%2Fexample.com%2Fpath%3Fa%3Db%26c%3Dd&timestamp=20220101000000"
        ));

        // the reserved characters from the original url must not survive unescaped into the
        // url param's value - only the "?timestamp=" separator we control is allowed through.
        final var urlParamValue = requestUrl.substring(requestUrl.indexOf("url=") + 4, requestUrl.indexOf("&timestamp="));
        assertThat(urlParamValue, not(containsString("?")));
        assertThat(urlParamValue, not(containsString("&")));
        assertThat(urlParamValue, not(containsString("=")));
    }

    @Test
    @DisplayName("A 200 response with a null body is treated as no snapshot found")
    void nullResponseBodyReturnsUriDoesNotExist() {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(null));

        final var failures = webArchiveService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }
}
