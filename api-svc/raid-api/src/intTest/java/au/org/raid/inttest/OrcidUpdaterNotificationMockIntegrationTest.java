package au.org.raid.inttest;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the API tells the registration agency's ORCID updater about every minted and updated
 * RAiD, by finding the request the local MockServer recorded for {@code POST /raid-changes}. The dev
 * profile points the ORCID integration at MockServer. Skipped when MockServer isn't running, as in
 * deployed environments.
 */
public class OrcidUpdaterNotificationMockIntegrationTest extends AbstractIntegrationTest {

    private static final String MOCKSERVER_HOST = "localhost";
    private static final int MOCKSERVER_PORT = 1080;
    private static final int MOCKSERVER_CONNECT_TIMEOUT_MILLIS = 2000;
    private static final String MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL =
            "http://" + MOCKSERVER_HOST + ":" + MOCKSERVER_PORT + "/mockserver/retrieve?type=REQUESTS";
    // The API notifies the updater in the background after the transaction commits.
    private static final long NOTIFICATION_WAIT_MILLIS = 10_000;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void checkMockServerIsReachable() {
        Assumptions.assumeTrue(isMockServerReachable(),
                "mockserver not reachable - skipping ORCID updater notification test");
    }

    private static boolean isMockServerReachable() {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(MOCKSERVER_HOST, MOCKSERVER_PORT), MOCKSERVER_CONNECT_TIMEOUT_MILLIS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    @DisplayName("Minting a RAiD sends its handle to the ORCID updater with the API key")
    void mintNotifiesUpdater() throws InterruptedException {
        createRequest.getTitle().get(0).setText("orcid-updater-mint-" + UUID.randomUUID());

        final var raid = raidApi.mintRaid(createRequest).getBody();
        assertThat(raid).isNotNull();

        final var handle = handleOf(raid.getIdentifier().getId());
        final var notification = awaitNotification(handle, 1);

        assertThat(notification).as("Expected a POST /raid-changes for %s", handle).isPresent();
        assertThat(notification.get().path("headers").toString()).containsIgnoringCase("X-Api-Key");
    }

    @Test
    @DisplayName("Updating a RAiD sends its handle to the ORCID updater again")
    void updateNotifiesUpdater() throws InterruptedException {
        createRequest.getTitle().get(0).setText("orcid-updater-update-" + UUID.randomUUID());

        final var raid = raidApi.mintRaid(createRequest).getBody();
        assertThat(raid).isNotNull();
        final var handle = handleOf(raid.getIdentifier().getId());

        raid.getTitle().get(0).setText("orcid-updater-updated-" + UUID.randomUUID());
        final var handleParts = handle.split("/");
        raidApi.updateRaid(handleParts[0], handleParts[1], raidUpdateRequestFactory.create(raid));

        assertThat(awaitNotification(handle, 2))
                .as("Expected a second POST /raid-changes for %s after the update", handle)
                .isPresent();
    }

    private static String handleOf(final String identifier) {
        final var parts = identifier.split("/");
        return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }

    /**
     * Waits until MockServer has recorded at least {@code count} notifications for the handle and
     * returns the latest.
     */
    private Optional<JsonNode> awaitNotification(final String handle, final int count) throws InterruptedException {
        final var deadline = System.currentTimeMillis() + NOTIFICATION_WAIT_MILLIS;

        while (true) {
            final var matching = recordedRaidChanges().stream()
                    .filter(request -> handle.equals(extractJsonBody(request).path("handle").asText()))
                    .toList();

            if (matching.size() >= count) {
                return Optional.of(matching.get(matching.size() - 1));
            }
            if (System.currentTimeMillis() > deadline) {
                return Optional.empty();
            }
            Thread.sleep(250);
        }
    }

    private List<JsonNode> recordedRaidChanges() {
        final var matcher = objectMapper.createObjectNode();
        matcher.put("path", "/raid-changes");
        matcher.put("method", "POST");

        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var response = restTemplate.exchange(
                MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL,
                HttpMethod.PUT,
                new HttpEntity<>(matcher.toString(), headers),
                JsonNode.class);

        final var body = response.getBody();
        final var requests = new ArrayList<JsonNode>();
        if (body != null && body.isArray()) {
            body.forEach(requests::add);
        }
        return requests;
    }

    private JsonNode extractJsonBody(final JsonNode recordedRequest) {
        final var bodyNode = recordedRequest.path("body");

        if (bodyNode.has("json")) {
            return bodyNode.get("json");
        }

        if (bodyNode.has("string")) {
            try {
                return objectMapper.readTree(bodyNode.get("string").asText());
            } catch (IOException e) {
                return objectMapper.missingNode();
            }
        }

        return bodyNode;
    }
}
