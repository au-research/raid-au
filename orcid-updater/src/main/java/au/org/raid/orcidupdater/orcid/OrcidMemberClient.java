package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Membership calls to ORCID's member API with the researcher's own token: POST to create,
 * PUT and DELETE by put code (ORCID-Source tutorial affiliations.md). Tokens are never logged.
 */
@Component
public class OrcidMemberClient {
    static final MediaType ORCID_JSON = MediaType.parseMediaType("application/vnd.orcid+json");
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OrcidMemberClient(final RestClient.Builder builder, final OrcidUpdaterProperties properties,
                             final ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(properties.getOrcid().getApiUrl()).build();
        this.objectMapper = objectMapper;
    }

    /** @return the put code ORCID gave the new entry */
    public long create(final String orcidId, final String accessToken, final Map<String, Object> membership) {
        final var response = call(() -> restClient.post()
                .uri("/{id}/membership", bareId(orcidId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(ORCID_JSON)
                .accept(ORCID_JSON)
                .body(membership)
                .retrieve()
                .toBodilessEntity());
        return putCode(response.getHeaders().getLocation());
    }

    public void update(final String orcidId, final String accessToken, final long putCode,
                       final Map<String, Object> membership) {
        call(() -> restClient.put()
                .uri("/{id}/membership/{putCode}", bareId(orcidId), putCode)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(ORCID_JSON)
                .accept(ORCID_JSON)
                .body(membership)
                .retrieve()
                .toBodilessEntity());
    }

    public void delete(final String orcidId, final String accessToken, final long putCode) {
        call(() -> restClient.delete()
                .uri("/{id}/membership/{putCode}", bareId(orcidId), putCode)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(ORCID_JSON)
                .retrieve()
                .toBodilessEntity());
    }

    /** The member API takes the bare iD, e.g. 0000-0002-1825-0097, not the iD URL. */
    static String bareId(final String orcidId) {
        return orcidId.substring(orcidId.lastIndexOf('/') + 1);
    }

    static long putCode(final URI location) {
        if (location == null) {
            throw new OrcidWriteException(Kind.REJECTED, "ORCID created the entry but sent no Location header");
        }
        final var path = location.getPath();
        try {
            return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        } catch (final NumberFormatException e) {
            throw new OrcidWriteException(Kind.REJECTED, "ORCID's Location header has no put code: " + location);
        }
    }

    private <T> T call(final Supplier<T> request) {
        try {
            return request.get();
        } catch (final HttpStatusCodeException e) {
            final var status = e.getStatusCode();
            final var message = "ORCID returned " + status.value() + developerMessage(e.getResponseBodyAsString());
            if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                throw new OrcidWriteException(Kind.TOKEN_REJECTED, message);
            }
            if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new OrcidWriteException(Kind.NOT_FOUND, message);
            }
            if (status.is5xxServerError() || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                throw new OrcidWriteException(Kind.RETRYABLE, message);
            }
            throw new OrcidWriteException(Kind.REJECTED, message);
        } catch (final ResourceAccessException e) {
            throw new OrcidWriteException(Kind.RETRYABLE, "ORCID could not be reached: " + e.getMessage());
        }
    }

    /**
     * ORCID's errors carry a developer-message explaining the refusal. Only that field is kept,
     * shortened, so an unexpected body can't fill the failure column.
     */
    private String developerMessage(final String body) {
        try {
            final JsonNode error = objectMapper.readTree(body);
            final var message = error == null ? "" : error.path("developer-message").asText("");
            if (message.isBlank()) {
                return "";
            }
            return ": " + (message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) + "..." : message);
        } catch (final IOException e) {
            return "";
        }
    }
}
