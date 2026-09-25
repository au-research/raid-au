package au.org.raid.api.service.ark;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.validator.UriValidator;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;
import static au.org.raid.api.endpoint.message.ValidationMessage.URI_DOES_NOT_EXIST;
import static au.org.raid.api.exception.ResolverUnavailableException.toUnavailableResolver;

/**
 * Existence check for {@code https://arks.org/} relatedObject URLs (RAID-793).
 * <p>
 * ARK resolution moved from n2t.net to arks.org. n2t.net now blindly 302-redirects every
 * syntactically-valid ARK (real or fake) to arks.org, so it can no longer be used as an
 * existence check. arks.org itself gives a clean NAAN-registry signal instead:
 * <ul>
 *     <li>A <b>registered</b> NAAN: a no-follow GET to {@code https://arks.org/ark:NAAN/name}
 *     302s with a {@code Location} whose host is a real Name Mapping Authority - i.e. NOT
 *     arks.org (e.g. ezid.cdlib.org, ark.bnf.fr).</li>
 *     <li>An <b>unregistered</b> NAAN: the 302 {@code Location} is a self-loop back to arks.org
 *     (or one of its subdomains), including relative/protocol-relative Locations resolved
 *     against the request URI.</li>
 * </ul>
 * This deliberately implements {@link UriValidator} directly rather than extending
 * {@link au.org.raid.api.validator.AbstractUriValidator}: the base class's HEAD/404 machinery
 * doesn't fit the resolver semantics here - the existence signal is which host a 302 redirects
 * to, not the HTTP status of the resolver call itself, and following the redirect (as the
 * shared {@code uriValidatorRestTemplate} does) would hide that signal. What is reused is the
 * {@link UriValidator} contract and {@link ResolverUnavailableException} for the fail-closed
 * RAID-809 pattern.
 * <p>
 * Requires a {@link RestTemplate} configured with redirect-following DISABLED
 * (see {@code arkResolverRestTemplate} in {@code Api.java}) so the {@code Location} header of a
 * 302 response is visible rather than transparently followed.
 */
@Slf4j
public class ArkService implements UriValidator {
    /*
     * https://arks.org/ark:/NAAN/name[/qualifier], with an optional slash after "ark:" (both
     * ark:12148/x and ark:/12148/x resolve). NAAN is 5-9 digits (ARK Alliance spec). The host
     * must be arks.org - bare/unqualified ARKs are not accepted, the submitted id must be a
     * fully-qualified resolvable URL.
     *
     * Anchored with \z (not $) so a trailing newline can't sneak past the end-of-input check -
     * Java's $ matches immediately before a trailing line terminator, so "...\n" would otherwise
     * pass.
     */
    protected static final Pattern ARK_URL_PATTERN =
            Pattern.compile("^https://arks\\.org/ark:/?\\d{5,9}/\\S+\\z");

    protected static final String INVALID_ARK_URL_MESSAGE =
            INVALID_VALUE_MESSAGE + " - " + "must be a valid ARK URL (e.g. https://arks.org/ark:/12148/cc9wq2rq)";

    private final RestTemplate restTemplate;

    public ArkService(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    protected String resolverName() {
        return "ARK";
    }

    @Override
    public List<ValidationFailure> validate(final String uri, final String fieldId) {
        if (!ARK_URL_PATTERN.matcher(uri).matches()) {
            return List.of(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message(INVALID_ARK_URL_MESSAGE));
        }

        try {
            final var response = restTemplate.exchange(URI.create(uri), HttpMethod.GET, HttpEntity.EMPTY, Void.class);
            return validateResponse(response, uri, fieldId);
        } catch (RestClientException e) {
            // Covers HttpClientErrorException/HttpServerErrorException (4xx/5xx from arks.org
            // itself) and ResourceAccessException (connect/read timeout, DNS failure, connection
            // refused). Any of these means the resolver, not the ARK, is at fault (RAID-809) -
            // we can't determine registration from a resolver that isn't working.
            log.error("External resolver check failed during ARK URI validation of {}", uri, e);
            throw new ResolverUnavailableException(
                    List.of(toUnavailableResolver(fieldId, uri, resolverName(), e)));
        } catch (IllegalArgumentException e) {
            // URI.create(uri) throws IllegalArgumentException (not a RestClientException) for a
            // syntactically-invalid URI that nonetheless matched ARK_URL_PATTERN (e.g. a name
            // containing an unencoded '{'/'}'). Fail closed the same way as a resolver failure
            // rather than let it escape as an uncaught 500.
            log.error("Malformed ARK URI could not be resolved: {}", uri, e);
            throw new ResolverUnavailableException(List.of(new UnavailableResolver()
                    .field(fieldId)
                    .value(uri)
                    .resolver(resolverName())
                    .downstreamMessage("%s resolve %s -> malformed URI".formatted(resolverName(), uri))));
        }
    }

    private List<ValidationFailure> validateResponse(
            final ResponseEntity<Void> response, final String uri, final String fieldId) {
        final var status = response.getStatusCode();

        if (!status.is3xxRedirection()) {
            // Not a redirect at all - including an unexpected 2xx from arks.org itself - means we
            // could not determine registration from a working resolver.
            log.error("Unexpected non-redirect response ({}) from arks.org during ARK URI validation of {}",
                    status, uri);
            throw new ResolverUnavailableException(List.of(
                    unavailableResolver(fieldId, uri, status.value(), "unexpected response status")));
        }

        try {
            // HttpHeaders.getLocation() (and URI.resolve() against a malformed value) throws
            // IllegalArgumentException - NOT a RestClientException - on an unparseable Location
            // header, so this must be guarded separately from the RestClientException catch around
            // the HTTP call - otherwise it escapes as an uncaught 500 instead of the fail-closed
            // 503 every other resolver failure gets here.
            final var location = response.getHeaders().getLocation();

            if (location == null) {
                log.error("Redirect response ({}) with no Location header during ARK URI validation of {}",
                        status, uri);
                throw new ResolverUnavailableException(List.of(
                        unavailableResolver(fieldId, uri, status.value(), "redirect with no Location header")));
            }

            if (isUnregistered(uri, location)) {
                return List.of(new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType(INVALID_VALUE_TYPE)
                        .message(URI_DOES_NOT_EXIST));
            }

            return List.of();
        } catch (IllegalArgumentException e) {
            log.error("Redirect response ({}) with an unparseable/unresolvable Location header during ARK URI validation of {}",
                    status, uri, e);
            throw new ResolverUnavailableException(List.of(
                    unavailableResolver(fieldId, uri, status.value(), "unparseable Location header")));
        }
    }

    /**
     * An unregistered NAAN redirects back to arks.org itself (a self-loop, possibly via a
     * relative or protocol-relative Location, or a www./other arks.org subdomain). A registered
     * NAAN redirects to a genuinely different host - the Name Mapping Authority that owns the
     * NAAN.
     * <p>
     * The Location header is resolved against the submitted request URI before inspecting its
     * host, so a relative Location (host == null on the raw header value) is correctly treated as
     * a same-host self-loop rather than false-ACCEPTed. A null resolved host (which should not be
     * possible once resolved against an absolute base) is treated as unregistered - fail-safe
     * rather than a silent accept.
     */
    private boolean isUnregistered(final String uri, final URI location) {
        final var resolved = URI.create(uri).resolve(location);
        final var host = resolved.getHost();

        return host == null || "arks.org".equalsIgnoreCase(host) || host.toLowerCase().endsWith(".arks.org");
    }

    private UnavailableResolver unavailableResolver(
            final String fieldId, final String uri, final int status, final String reason) {
        return new UnavailableResolver()
                .field(fieldId)
                .value(uri)
                .resolver(resolverName())
                .downstreamStatus(status)
                .downstreamMessage("%s resolve %s -> %d (%s)".formatted(resolverName(), uri, status, reason));
    }
}
