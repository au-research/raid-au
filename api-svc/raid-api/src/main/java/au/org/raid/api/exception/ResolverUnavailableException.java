package au.org.raid.api.exception;

import au.org.raid.idl.raidv2.model.UnavailableResolver;
import lombok.Getter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.List.copyOf;

/**
 * Thrown when one or more external identifier resolvers (ROR, ORCID, ISNI, DOI, Handle, RRID,
 * GeoNames, OpenStreetMap) could not be reached or returned something other than a clean
 * 404/200 - i.e. the resolver's availability, not the identifier's validity, is in question.
 * Maps to an HTTP 503 (see RaidExceptionHandler#handleResolverUnavailable) so callers know to
 * retry rather than treating this as a validation failure of their input. See RAID-809.
 */
@Getter
public class ResolverUnavailableException extends RaidApiException {
    private static final String TITLE = "Resolver unavailable";
    private static final int STATUS = 503;
    private final List<UnavailableResolver> unavailableResolvers;

    public ResolverUnavailableException(Collection<UnavailableResolver> unavailableResolvers) {
        super();
        this.unavailableResolvers = copyOf(unavailableResolvers);
    }

    public String getTitle() {
        return TITLE;
    }

    public int getStatus() {
        return STATUS;
    }

    public String getDetail() {
        return "%d external identifier resolver(s) were unavailable. Please retry."
                .formatted(unavailableResolvers.size());
    }

    /**
     * RaidApiException#getType() defaults to "...#%s".formatted(getClass().getSimpleName()),
     * which would yield "...#ResolverUnavailableException". We deliberately diverge from that
     * default here so the wire type is the more readable "...#ResolverUnavailable".
     */
    @Override
    public String getType() {
        return "https://raid.org.au/errors#ResolverUnavailable";
    }

    /**
     * For logs only - not understandable enough to present to real users
     * (mirrors ValidationException#getMessage()).
     */
    @Override
    public String getMessage() {
        return unavailableResolvers.stream()
                .map(i -> "%s -> %s(%s)".formatted(i.getField(), i.getResolver(), i.getDownstreamStatus()))
                .collect(Collectors.joining(","));
    }

    /**
     * Builds a sanitised UnavailableResolver entry from a caught RestClientException. Never
     * calls e.getResponseBodyAsString() - the downstream body may contain arbitrary content
     * that shouldn't be echoed back to the caller or into logs.
     */
    public static UnavailableResolver toUnavailableResolver(
            final String field, final String value, final String resolver, final RestClientException e) {
        final Integer status = (e instanceof HttpStatusCodeException h) ? h.getStatusCode().value() : null;

        return new UnavailableResolver()
                .field(field)
                .value(value)
                .resolver(resolver)
                .downstreamStatus(status)
                .downstreamMessage("%s resolve %s -> %s".formatted(
                        resolver, value, status != null ? status : "connection failed"));
    }
}
