package au.org.raid.orcidupdater.raidapi;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reads from the RA's RAiD API on behalf of a minter, with the minter's own access token, so the
 * RAiD API's existing authorisation decides what the minter may see and invite for (RAID-1002).
 */
@Slf4j
@Component
public class RaidApiClient {
    static final String PRIMARY_TITLE_TYPE = "https://vocabulary.raid.org/title.type.schema/5";

    private final RestClient restClient;

    public RaidApiClient(final RestClient.Builder builder, final OrcidUpdaterProperties properties) {
        this.restClient = builder.baseUrl(properties.getRaidApi().getBaseUrl()).build();
    }

    /**
     * The details an invitation needs, after checking the minter can edit the RAiD.
     *
     * @throws RaidNotFoundException        the RAiD API has no RAiD with this handle
     * @throws MinterNotPermittedException  the minter can't edit it, or the RAiD API refused the token
     * @throws RaidApiUnavailableException  the RAiD API could not be reached or failed
     */
    public RaidSummary findEditableRaid(final String handle, final String minterToken) {
        final var parts = handle.split("/", 2);
        final var prefix = parts[0];
        final var suffix = parts[1];

        final var permissions = call(handle, () -> restClient.get()
                .uri("/raid/{prefix}/{suffix}/permissions", prefix, suffix)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + minterToken)
                .retrieve()
                .body(JsonNode.class));
        if (permissions == null || !permissions.path("write").asBoolean(false)) {
            throw new MinterNotPermittedException(handle);
        }

        final var raid = call(handle, () -> restClient.get()
                .uri("/raid/{prefix}/{suffix}", prefix, suffix)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + minterToken)
                .retrieve()
                .body(JsonNode.class));
        if (raid == null) {
            throw new RaidApiUnavailableException("empty RAiD response for " + handle);
        }

        final var servicePointId = raid.path("identifier").path("owner").path("servicePoint").asText();
        final var servicePoint = call(handle, () -> restClient.get()
                .uri("/service-point/{id}", servicePointId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + minterToken)
                .retrieve()
                .body(JsonNode.class));

        return new RaidSummary(handle, primaryTitle(raid), servicePointName(servicePoint), contributorIds(raid));
    }

    private <T> T call(final String handle, final Supplier<T> request) {
        try {
            return request.get();
        } catch (final HttpStatusCodeException e) {
            final HttpStatusCode status = e.getStatusCode();
            if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new RaidNotFoundException(handle);
            }
            if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
                throw new MinterNotPermittedException(handle);
            }
            throw new RaidApiUnavailableException("RAiD API returned %s for %s".formatted(status.value(), handle));
        } catch (final ResourceAccessException e) {
            throw new RaidApiUnavailableException("RAiD API unreachable for " + handle, e);
        }
    }

    private static String primaryTitle(final JsonNode raid) {
        for (final var title : raid.path("title")) {
            if (PRIMARY_TITLE_TYPE.equals(title.path("type").path("id").asText())) {
                return title.path("text").asText();
            }
        }
        // the RAiD API requires a primary title, so this is a defensive fallback only
        return raid.path("title").path(0).path("text").asText("");
    }

    /** The name only: a service point's contact emails must never reach a researcher. */
    private static String servicePointName(final JsonNode servicePoint) {
        return servicePoint == null ? "" : servicePoint.path("name").asText("");
    }

    private static List<String> contributorIds(final JsonNode raid) {
        final var ids = new ArrayList<String>();
        raid.path("contributor").forEach(contributor -> {
            final var id = contributor.path("id").asText(null);
            if (id != null) {
                ids.add(id);
            }
        });
        return ids;
    }
}
