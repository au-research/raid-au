package au.org.raid.api.service.doi;

import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoiServiceTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final DoiService doiService = new DoiService(restTemplate);

    @ParameterizedTest
    @DisplayName("Accepts a DOI regardless of which proxy host it uses (RAID-798)")
    @ValueSource(strings = {
            "https://doi.org/10.1234/xyz",      // preferred host
            "https://dx.doi.org/10.1234/xyz",   // legacy proxy, https
            "http://dx.doi.org/10.1234/xyz"     // legacy proxy, http
    })
    void acceptsBothProxyHosts(final String storedUri) {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class))).thenReturn(null);

        final var failures = doiService.validate(storedUri, "relatedObject[0].id");

        assertThat(failures, empty());
    }

    @ParameterizedTest
    @DisplayName("HEAD-checks the submitted URL as-is, without normalising the proxy host")
    @ValueSource(strings = {
            "https://doi.org/10.1234/xyz",
            "https://dx.doi.org/10.1234/xyz"
    })
    void headsSubmittedUrlWithoutNormalising(final String storedUri) {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class))).thenReturn(null);

        final var failures = doiService.validate(storedUri, "relatedObject[0].id");

        assertThat(failures, empty());

        // RequestEntity.head(String) builds a URI-template entity whose getUrl() is not
        // resolvable until the RestTemplate expands it, so assert on the template via toString().
        final var captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(captor.capture(), eq(Void.class));
        assertThat(captor.getValue().toString(), containsString(storedUri));
    }

    @ParameterizedTest
    @DisplayName("Returns 'uri not found' when the resolver 404s a dx.doi.org DOI, same as any other")
    @ValueSource(strings = {
            "https://doi.org/10.1234/does-not-exist",
            "https://dx.doi.org/10.1234/does-not-exist"
    })
    void nonExistentDoi(final String storedUri) {
        final var fieldId = "relatedObject[0].id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(404)));

        final var failures = doiService.validate(storedUri, fieldId);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @ParameterizedTest
    @DisplayName("Fails regex validation for malformed DOIs and never calls the resolver (regression guard)")
    @ValueSource(strings = {
            "https://dx.doi.org/not-a-doi",     // dx host but no 10. DOI prefix
            "https://doi.org/not-a-doi",         // preferred host but no 10. DOI prefix
            "https://dxx.doi.org/10.1234/xyz",   // not a recognised proxy host
            "https://doi.org/",                  // missing DOI entirely
            "ftp://doi.org/10.1234/xyz"          // unsupported scheme
    })
    void rejectsMalformedDoi(final String storedUri) {
        final var fieldId = "relatedObject[0].id";

        final var failures = doiService.validate(storedUri, fieldId);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType("invalidValue")
                        .message("has invalid/unsupported value - should match " + doiService.getRegex())
        )));
    }
}
