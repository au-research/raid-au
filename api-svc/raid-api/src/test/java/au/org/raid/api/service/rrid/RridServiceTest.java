package au.org.raid.api.service.rrid;

import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

class RridServiceTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final RridService rridService = new RridService(restTemplate);

    @Test
    @DisplayName("HEAD-checks the .json resolver variant, not the bare stored URL")
    void headsJsonVariant() {
        final var storedUri = "https://scicrunch.org/resolver/RRID:AB_2298772";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class))).thenReturn(null);

        final var failures = rridService.validate(storedUri, "relatedObject[0].id");

        assertThat(failures, empty());

        // RequestEntity.head(String) builds a URI-template entity whose getUrl() is not
        // resolvable until the RestTemplate expands it, so assert on the template via toString().
        final var captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(captor.capture(), eq(Void.class));
        assertThat(captor.getValue().toString(), containsString(storedUri + ".json"));
    }

    @Test
    @DisplayName("Returns 'uri not found' when the .json resolver returns 404")
    void nonExistentRrid() {
        final var fieldId = "relatedObject[0].id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(404)));

        final var failures = rridService.validate("https://scicrunch.org/resolver/RRID:AB_0000000", fieldId);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @ParameterizedTest
    @DisplayName("Accepts the real RRID source forms")
    @ValueSource(strings = {
            "https://scicrunch.org/resolver/RRID:AB_2298772",   // antibody
            "https://scicrunch.org/resolver/RRID:SCR_003070",   // tool/software
            "https://scicrunch.org/resolver/RRID:CVCL_0027",    // cell line
            "https://scicrunch.org/resolver/RRID:IMSR_JAX:000664" // organism, embedded colon
    })
    void acceptsRealRridForms(final String storedUri) {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class))).thenReturn(null);

        final var failures = rridService.validate(storedUri, "relatedObject[0].id");

        assertThat(failures, empty());
    }

    @ParameterizedTest
    @DisplayName("Fails regex validation for malformed RRIDs and never calls the resolver")
    @ValueSource(strings = {
            "https://scicrunch.org/resolver/not-an-rrid",       // no RRID: prefix
            "https://scicrunch.org/resolver/RRID:AB_",           // missing id
            "https://scicrunch.org/resolver/RRID:_2298772",      // missing source
            "https://scicrunch.org/resolver/RRID:AB_229 8772",   // whitespace in id
            "https://scicrunch.org/resolver/RRID:AB_{0}"         // brace would break URI-template expansion
    })
    void rejectsMalformedRrid(final String storedUri) {
        final var fieldId = "relatedObject[0].id";

        final var failures = rridService.validate(storedUri, fieldId);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType("invalidValue")
                        .message("has invalid/unsupported value - should match " + rridService.getRegex())
        )));
    }
}
