package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AbstractUriValidatorTest {
    private RestTemplate restTemplate = mock(RestTemplate.class);

    private TestUriValidator uriValidator = new TestUriValidator();

    @Test
    @DisplayName("No failures when URI matches regex and no exception thrown in RestTemplate")
    void validUri() {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class))).thenReturn(null);

        final var failures = uriValidator.validate("http://localhost", "field-id");
        assertThat(failures, empty());
    }

    @Test
    @DisplayName("By default the resolver HEAD-checks the stored uri unchanged")
    void resolverUriDefaultsToStoredUri() {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class))).thenReturn(null);

        uriValidator.validate("http://localhost", "field-id");

        final var captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(captor.capture(), eq(Void.class));
        assertThat(captor.getValue().toString(), containsString("HEAD http://localhost,"));
    }

    @Test
    @DisplayName("Failures returned when URI does not match regex")
    void uriFailsRegex() {
        final var fieldId = "field-id";

        final var failures = uriValidator.validate("http://example.org", fieldId);
        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType("invalidValue")
                        .message("has invalid/unsupported value - should match ^http://localhost")
        )));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Returns failure when uri not found")
    void uriNotFound() {
        final var fieldId = "field-id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(404)));

        final var failures = uriValidator.validate("http://localhost", fieldId);
        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("Throws ResolverUnavailableException when request throws a non-404 HttpClientErrorException")
    void requestFails() {
        final var fieldId = "field-id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(500)));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> uriValidator.validate("http://localhost", fieldId));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        final var unavailable = e.getUnavailableResolvers().get(0);
        assertThat(unavailable.getField(), is(fieldId));
        assertThat(unavailable.getValue(), is("http://localhost"));
        assertThat(unavailable.getResolver(), is("TestResolver"));
        assertThat(unavailable.getDownstreamStatus(), is(500));
    }

    @Test
    @DisplayName("Throws ResolverUnavailableException when resolver read times out")
    void requestTimesOut() {
        final var fieldId = "field-id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out")));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> uriValidator.validate("http://localhost", fieldId));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        final var unavailable = e.getUnavailableResolvers().get(0);
        assertThat(unavailable.getField(), is(fieldId));
        assertThat(unavailable.getValue(), is("http://localhost"));
        assertThat(unavailable.getResolver(), is("TestResolver"));
        assertThat(unavailable.getDownstreamStatus(), nullValue());
    }

    @Test
    @DisplayName("Throws ResolverUnavailableException when resolver connection cannot be established")
    void connectionRefused() {
        final var fieldId = "field-id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused", new java.net.ConnectException("Connection refused")));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> uriValidator.validate("http://localhost", fieldId));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        final var unavailable = e.getUnavailableResolvers().get(0);
        assertThat(unavailable.getField(), is(fieldId));
        assertThat(unavailable.getValue(), is("http://localhost"));
        assertThat(unavailable.getResolver(), is("TestResolver"));
        assertThat(unavailable.getDownstreamStatus(), nullValue());
    }

    @Test
    @DisplayName("Throws ResolverUnavailableException when resolver returns a 5xx")
    void resolverServerError() {
        final var fieldId = "field-id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new HttpServerErrorException(HttpStatusCode.valueOf(503)));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> uriValidator.validate("http://localhost", fieldId));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        final var unavailable = e.getUnavailableResolvers().get(0);
        assertThat(unavailable.getField(), is(fieldId));
        assertThat(unavailable.getValue(), is("http://localhost"));
        assertThat(unavailable.getResolver(), is("TestResolver"));
        assertThat(unavailable.getDownstreamStatus(), is(503));
    }

    @Test
    @DisplayName("Throws ResolverUnavailableException for any other RestClientException")
    void otherRestClientException() {
        final var fieldId = "field-id";
        when(restTemplate.exchange(any(RequestEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Unexpected client failure"));

        final var e = assertThrows(ResolverUnavailableException.class,
                () -> uriValidator.validate("http://localhost", fieldId));

        assertThat(e.getUnavailableResolvers(), hasSize(1));
        final var unavailable = e.getUnavailableResolvers().get(0);
        assertThat(unavailable.getField(), is(fieldId));
        assertThat(unavailable.getValue(), is("http://localhost"));
        assertThat(unavailable.getResolver(), is("TestResolver"));
        assertThat(unavailable.getDownstreamStatus(), nullValue());
    }

    private class TestUriValidator extends AbstractUriValidator {
        @Override
        protected String getRegex() {
            return "^http://localhost";
        }

        @Override
        protected RestTemplate getRestTemplate() {
            return AbstractUriValidatorTest.this.restTemplate;
        }

        @Override
        protected String resolverName() {
            return "TestResolver";
        }
    }
}