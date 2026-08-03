package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Slf4j
public abstract class AbstractUriValidator implements UriValidator {
    protected abstract String getRegex();

    protected abstract RestTemplate getRestTemplate();

    public List<ValidationFailure> validate(final String uri, final String fieldId) {
        final var failures = new ArrayList<ValidationFailure>();

        final var regex = getRegex();

        if (!uri.matches(regex)) {
            failures.add(
                    new ValidationFailure()
                            .fieldId(fieldId)
                            .errorType(INVALID_VALUE_TYPE)
                            .message(INVALID_VALUE_MESSAGE + " - should match %s".formatted(regex))
            );

        } else {
            final var requestEntity = RequestEntity.head(uri).build();
            try {
                final var start = Instant.now();
                getRestTemplate().exchange(requestEntity, Void.class);
                final var end = Instant.now();
                Duration duration = Duration.between(start, end);
                log.info("request to {} took {}.{} seconds", uri, duration.getSeconds(), duration.getNano());
            } catch (HttpClientErrorException e) {

                if (e.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                    failures.add(new ValidationFailure()
                            .fieldId(fieldId)
                            .errorType(INVALID_VALUE_TYPE)
                            .message(URI_DOES_NOT_EXIST)
                    );
                } else {
                    log.error("Request failed during URI validation", e);
                    failures.add(serverError(fieldId));
                }
            } catch (RestClientException e) {
                // Covers ResourceAccessException (connect/read timeout, DNS failure,
                // connection refused), HttpServerErrorException (5xx) and, defensively,
                // any other RestClientException. The resolver could not confirm the URI,
                // so return a clean validation failure rather than propagating an HTTP 500.
                log.error("External resolver check failed during URI validation of {}", uri, e);
                failures.add(serverError(fieldId));
            }
        }

        return failures;
    }

    private ValidationFailure serverError(final String fieldId) {
        return new ValidationFailure()
                .fieldId(fieldId)
                .errorType(INVALID_VALUE_TYPE)
                .message(SERVER_ERROR);
    }
}
