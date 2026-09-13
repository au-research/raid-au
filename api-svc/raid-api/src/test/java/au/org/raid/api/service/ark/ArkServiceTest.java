package au.org.raid.api.service.ark;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArkServiceTest {
    private static final String FIELD_ID = "relatedObject[0].id";

    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final ArkService arkService = new ArkService(restTemplate);

    private void stubRedirect(final String uri, final HttpStatus status, final String location) {
        final var headers = new HttpHeaders();
        if (location != null) {
            headers.setLocation(URI.create(location));
        }
        final var response = new ResponseEntity<Void>(headers, status);
        when(restTemplate.exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(response);
    }

    @Test
    @DisplayName("A 302 redirect to a different (Name Mapping Authority) host is accepted - registered NAAN")
    void registeredNaanRedirectsToDifferentHostAccepted() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        stubRedirect(uri, HttpStatus.FOUND, "https://ark.bnf.fr/ark:/12148/cc9wq2rq");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("A 302 self-loop redirect back to arks.org is rejected - unregistered NAAN")
    void unregisteredNaanSelfLoopRejected() {
        final var uri = "https://arks.org/ark:/99999/not-found";
        stubRedirect(uri, HttpStatus.FOUND, "https://arks.org/.info/ark:/99999/not-found");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("A 302 to arks.org with a different (non-.info) path is still treated as unregistered")
    void unregisteredNaanArksOrgHostAnyPathRejected() {
        final var uri = "https://arks.org/ark:/99999/not-found";
        stubRedirect(uri, HttpStatus.FOUND, "https://arks.org/ark:/99999/not-found");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("NAAN too short (fewer than 5 digits) fails format validation without an HTTP call")
    void naanTooShortFormatFailure() {
        final var uri = "https://arks.org/ark:/1234/cc9wq2rq";

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0).getFieldId(), is(FIELD_ID));
        assertThat(failures.get(0).getErrorType(), is("invalidValue"));
    }

    @Test
    @DisplayName("A bare ARK with no host fails format validation without an HTTP call")
    void bareArkNoHostFormatFailure() {
        final var uri = "ark:/12148/cc9wq2rq";

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, hasSize(1));
        assertThat(failures.get(0).getFieldId(), is(FIELD_ID));
        assertThat(failures.get(0).getErrorType(), is("invalidValue"));
    }

    @Test
    @DisplayName("A NAAN with no leading slash after ark: is still accepted by the format check")
    void naanWithoutLeadingSlashIsValidFormat() {
        final var uri = "https://arks.org/ark:12148/cc9wq2rq";
        stubRedirect(uri, HttpStatus.FOUND, "https://ark.bnf.fr/ark:12148/cc9wq2rq");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("5xx response from arks.org throws ResolverUnavailableException")
    void serverErrorThrowsResolverUnavailable() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        when(restTemplate.exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatusCode.valueOf(503), "Service Unavailable", null, null, null));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
        assertThat(e.getUnavailableResolvers().get(0).getDownstreamStatus(), is(503));
    }

    @Test
    @DisplayName("4xx response from arks.org throws ResolverUnavailableException")
    void clientErrorThrowsResolverUnavailable() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        when(restTemplate.exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(404)));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
        assertThat(e.getUnavailableResolvers().get(0).getDownstreamStatus(), is(404));
    }

    @Test
    @DisplayName("Timeout throws ResolverUnavailableException with resolver name ARK")
    void timeoutThrowsResolverUnavailable() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        when(restTemplate.exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
    }

    @Test
    @DisplayName("An unexpected 200 response from arks.org (not a redirect) throws ResolverUnavailableException")
    void unexpected200ThrowsResolverUnavailable() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        when(restTemplate.exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
    }

    @Test
    @DisplayName("A redirect with no Location header throws ResolverUnavailableException")
    void redirectWithNoLocationThrowsResolverUnavailable() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        stubRedirect(uri, HttpStatus.FOUND, null);

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
    }

    @Test
    @DisplayName("The submitted URL is never rewritten - the same string that was submitted is the one checked")
    void submittedUrlIsNotRewritten() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq/qualifier";
        stubRedirect(uri, HttpStatus.FOUND, "https://ark.bnf.fr/ark:/12148/cc9wq2rq/qualifier");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
        // stubRedirect's `eq(URI.create(uri))` matcher on restTemplate.exchange already asserts
        // the exact submitted uri was used for the resolver call; verifying no other overload was
        // invoked confirms no rewritten variant was ever sent.
        org.mockito.Mockito.verify(restTemplate)
                .exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class));
    }

    @Test
    @DisplayName("A relative Location header is resolved against the request URI and treated as an unregistered self-loop")
    void relativeLocationTreatedAsUnregistered() {
        final var uri = "https://arks.org/ark:/99999/not-found";
        stubRedirect(uri, HttpStatus.FOUND, "/ark:/99999/not-found");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("A www.arks.org subdomain self-loop Location is rejected as unregistered")
    void wwwSubdomainSelfLoopRejected() {
        final var uri = "https://arks.org/ark:/99999/not-found";
        stubRedirect(uri, HttpStatus.FOUND, "https://www.arks.org/ark:/99999/not-found");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("A protocol-relative Location back to arks.org is rejected as unregistered")
    void protocolRelativeSelfLoopRejected() {
        final var uri = "https://arks.org/ark:/99999/not-found";
        stubRedirect(uri, HttpStatus.FOUND, "//arks.org/ark:/99999/not-found");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("A genuine off-host NMA redirect whose path happens to contain /.info/ is still accepted")
    void offHostRedirectWithInfoInPathAccepted() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        stubRedirect(uri, HttpStatus.FOUND, "https://ark.bnf.fr/.info/ark:/12148/cc9wq2rq");

        final var failures = arkService.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("A malformed/unparseable Location header throws ResolverUnavailableException, not a 500")
    void malformedLocationThrowsResolverUnavailable() {
        final var uri = "https://arks.org/ark:/12148/cc9wq2rq";
        final var headers = mock(HttpHeaders.class);
        when(headers.getLocation()).thenThrow(new IllegalArgumentException("Invalid location header"));
        @SuppressWarnings("unchecked")
        final var response = (ResponseEntity<Void>) mock(ResponseEntity.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.FOUND);
        when(response.getHeaders()).thenReturn(headers);
        when(restTemplate.exchange(eq(URI.create(uri)), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(response);

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
    }

    @Test
    @DisplayName("An ARK name containing '{' is handled without a 500 (resolver-unavailable, not an uncaught exception)")
    void nameWithBraceHandledWithoutServerError() {
        final var uri = "https://arks.org/ark:/12148/{foo}";

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> arkService.validate(uri, FIELD_ID));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("ARK"));
    }
}
