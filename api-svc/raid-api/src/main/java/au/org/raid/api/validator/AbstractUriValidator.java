package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
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
import static au.org.raid.api.exception.ResolverUnavailableException.toUnavailableResolver;

@Slf4j
public abstract class AbstractUriValidator implements UriValidator {
    protected abstract String getRegex();

    protected abstract RestTemplate getRestTemplate();

    /**
     * The name reported in UnavailableResolver.resolver when this validator can't confirm a
     * URI because the resolver itself is unreachable/erroring (RAID-809), e.g. "DOI", "ORCID".
     */
    protected abstract String resolverName();

    /**
     * The URL to HEAD-check for existence, derived from the (already regex-validated) stored uri.
     * Defaults to the stored uri itself. Subclasses override this when the canonical stored form
     * is not directly resolvable server-side (e.g. RRID: the bare scicrunch.org/resolver/ URL sits
     * behind a Cloudflare challenge that returns 403 to all non-browser clients, so we check the
     * ".json" resolver variant instead, which returns a clean 200/404).
     */
    protected String resolverUri(final String uri) {
        return uri;
    }

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
            final var resolverUri = resolverUri(uri);
            final var requestEntity = RequestEntity.head(resolverUri).build();
            try {
                final var start = Instant.now();
                getRestTemplate().exchange(requestEntity, Void.class);
                final var end = Instant.now();
                Duration duration = Duration.between(start, end);
                log.info("request to {} took {}.{} seconds", resolverUri, duration.getSeconds(), duration.getNano());
            } catch (HttpClientErrorException e) {

                if (e.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                    failures.add(new ValidationFailure()
                            .fieldId(fieldId)
                            .errorType(INVALID_VALUE_TYPE)
                            .message(URI_DOES_NOT_EXIST)
                    );
                } else {
                    // Non-404 client error (e.g. 401/403 - the ORCID member-API case). The
                    // resolver, not the URI, is at fault, so this is a 503 (RAID-809), not a
                    // validation failure.
                    log.error("Request failed during URI validation", e);
                    throw new ResolverUnavailableException(
                            List.of(toUnavailableResolver(fieldId, uri, resolverName(), e)));
                }
            } catch (RestClientException e) {
                // Covers ResourceAccessException (connect/read timeout, DNS failure,
                // connection refused), HttpServerErrorException (5xx) and, defensively,
                // any other RestClientException. The resolver could not confirm the URI, so
                // raise a 503 (RAID-809) rather than a validation failure or an opaque HTTP 500.
                log.error("External resolver check failed during URI validation of {}", uri, e);
                throw new ResolverUnavailableException(
                        List.of(toUnavailableResolver(fieldId, uri, resolverName(), e)));
            }
        }

        return failures;
    }
}
