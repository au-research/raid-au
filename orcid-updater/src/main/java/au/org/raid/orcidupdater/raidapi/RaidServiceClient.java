package au.org.raid.orcidupdater.raidapi;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.serviceauth.ServiceTokenClient;
import au.org.raid.orcidupdater.status.ContributorStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Calls the RA's RAiD API as the updater itself, with its own Keycloak client holding
 * contributor-writer (RAID-1005): reads any RAiD, embargoed ones included, and sets contributors'
 * status. Minters' requests go through {@link RaidApiClient} with their own token instead.
 */
@Slf4j
@Component
public class RaidServiceClient {
    private final RestClient restClient;
    private final ServiceTokenClient tokenClient;

    public RaidServiceClient(final RestClient.Builder builder, final OrcidUpdaterProperties properties,
                             final ServiceTokenClient tokenClient) {
        this.restClient = builder.baseUrl(properties.getRaidApi().getBaseUrl()).build();
        this.tokenClient = tokenClient;
    }

    /**
     * @return the RAiD as the API returns it, or empty when the API has no RAiD with this handle
     * @throws RaidApiUnavailableException the API, or Keycloak for the updater's token, failed
     */
    public Optional<JsonNode> findRaid(final String handle) {
        final var parts = handle.split("/", 2);
        return call("reading " + handle, token -> restClient.get()
                .uri("/raid/{prefix}/{suffix}", parts[0], parts[1])
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class));
    }

    /** Handles of every RAiD naming this iD as a contributor. */
    public List<String> findHandlesByContributor(final String orcidId) {
        // the API matches the bare iD after adding its own ORCID URL prefix
        final var raids = call("listing RAiDs for " + orcidId, token -> restClient.get()
                .uri(uri -> uri.path("/raid/")
                        .queryParam("contributor.id", OrcidMemberClient.bareId(orcidId))
                        .queryParam("includeFields", "identifier")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class));
        final var handles = new ArrayList<String>();
        raids.ifPresent(list -> list.forEach(raid -> handle(raid).ifPresent(handles::add)));
        return handles;
    }

    /**
     * Sets the contributor's status on the RAiD API's shared contributor row (RAID-976).
     *
     * @return false when the API has no contributor with this iD
     */
    public boolean updateContributorStatus(final String orcidId, final ContributorStatus status) {
        return call("setting the status of " + orcidId, token -> {
            restClient.put()
                    .uri("/contributor/status")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("id", orcidId, "status", status.name()))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        }).isPresent();
    }

    /** Empty on 404. One retry with a fresh token on 401, in case the cached one was revoked. */
    private <T> Optional<T> call(final String action, final Function<String, T> request) {
        for (var attempt = 1; ; attempt++) {
            try {
                return Optional.ofNullable(request.apply(tokenClient.accessToken()));
            } catch (final HttpStatusCodeException e) {
                if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                    return Optional.empty();
                }
                if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED) && attempt == 1) {
                    tokenClient.reset();
                    continue;
                }
                throw new RaidApiUnavailableException("RAiD API returned %d %s".formatted(e.getStatusCode().value(), action));
            } catch (final ResourceAccessException e) {
                throw new RaidApiUnavailableException("RAiD API unreachable " + action, e);
            }
        }
    }

    private static Optional<String> handle(final JsonNode raid) {
        final var id = raid.path("identifier").path("id").asText(null);
        if (id == null) {
            return Optional.empty();
        }
        final var path = URI.create(id).getPath();
        return path == null || path.length() < 2 ? Optional.empty() : Optional.of(path.substring(1));
    }
}
