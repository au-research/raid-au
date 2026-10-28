package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.AccessStatement;
import au.org.raid.idl.raidv2.model.AccessType;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import au.org.raid.idl.raidv2.model.AccessTypeSchemaUriEnum;
import au.org.raid.inttest.service.Handle;
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
import java.util.UUID;

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the full app against the local DataCite MockServer and checks the DOI state event the API
 * sends for embargoed RAiDs (RAID-1012). A RAiD changed from open to embargoed must send "hide", the
 * only event that moves a findable DOI back to registered. A RAiD embargoed at mint keeps "register".
 *
 * <p>Like {@link DataciteRelatedRaidMockIntegrationTest}, it leaves MockServer's shared expectations
 * alone and finds its own requests among the recorded traffic by the freshly minted RAiD's handle.
 */
public class DataciteEmbargoEventMockIntegrationTest extends AbstractIntegrationTest {

    private static final String MOCKSERVER_HOST = "localhost";
    private static final int MOCKSERVER_PORT = 1080;
    private static final int MOCKSERVER_CONNECT_TIMEOUT_MILLIS = 2000;
    private static final String MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL =
            "http://" + MOCKSERVER_HOST + ":" + MOCKSERVER_PORT + "/mockserver/retrieve?type=REQUESTS";

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void checkMockServerIsReachable() {
        Assumptions.assumeTrue(isMockServerReachable(),
                "mockserver not reachable - skipping DataCite embargo event mock test");
    }

    private static boolean isMockServerReachable() {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(MOCKSERVER_HOST, MOCKSERVER_PORT), MOCKSERVER_CONNECT_TIMEOUT_MILLIS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static AccessType accessType(final String accessTypeId) {
        return new AccessType()
                .id(AccessTypeIdEnum.fromValue(accessTypeId))
                .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI));
    }

    @Test
    @DisplayName("Changing an open RAiD to embargoed sends the DataCite 'hide' event (RAID-1012)")
    void openToEmbargoedSendsHide() {
        createRequest.getTitle().get(0).setText("RAID-1012-open-to-embargoed-" + UUID.randomUUID());
        createRequest.getAccess()
                .type(accessType(OPEN_ACCESS_TYPE))
                .embargoExpiry(null);

        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assertThat(mintedRaid).isNotNull();

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        assertThat(latestEvent(HttpMethod.POST, "/dois", handle.toString()))
                .as("open RAiD is minted as findable")
                .isEqualTo("publish");

        final var updateRequest = raidUpdateRequestFactory.create(mintedRaid);
        updateRequest.getAccess()
                .type(accessType(EMBARGOED_ACCESS_TYPE))
                .embargoExpiry(today.plusMonths(1))
                .statement(new AccessStatement().text("Embargoed"));

        try {
            raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest);
        } catch (final Exception e) {
            failOnError(e);
        }

        assertThat(latestEvent(HttpMethod.PUT, "/dois/" + handle, handle.toString()))
                .as("embargo update hides the findable DOI")
                .isEqualTo("hide");
    }

    @Test
    @DisplayName("Minting an embargoed RAiD still sends the DataCite 'register' event (RAID-1012)")
    void mintEmbargoedSendsRegister() {
        createRequest.getTitle().get(0).setText("RAID-1012-embargoed-at-mint-" + UUID.randomUUID());
        createRequest.getAccess()
                .type(accessType(EMBARGOED_ACCESS_TYPE))
                .embargoExpiry(today.plusMonths(1))
                .statement(new AccessStatement().text("Embargoed"));

        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assertThat(mintedRaid).isNotNull();

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        assertThat(latestEvent(HttpMethod.POST, "/dois", handle.toString()))
                .isEqualTo("register");
    }

    /**
     * The "event" of the most recent DataCite request recorded for the given method and path whose
     * body references the handle.
     */
    private String latestEvent(final HttpMethod method, final String path, final String handle) {
        final var matchingRequestBody = retrieveRecordedRequestBodies(method, path).stream()
                .filter(body -> body != null && body.toString().contains(handle))
                .reduce((first, second) -> second)
                .orElse(null);

        assertThat(matchingRequestBody)
                .as("Expected a recorded DataCite %s %s request referencing handle %s", method, path, handle)
                .isNotNull();

        return matchingRequestBody.path("data").path("attributes").path("event").asText();
    }

    private List<JsonNode> retrieveRecordedRequestBodies(final HttpMethod method, final String path) {
        final var matcher = objectMapper.createObjectNode();
        matcher.put("path", path);
        matcher.put("method", method.name());

        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var response = restTemplate.exchange(
                MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL,
                HttpMethod.PUT,
                new HttpEntity<>(matcher.toString(), headers),
                JsonNode.class);

        final var body = response.getBody();
        if (body == null || !body.isArray()) {
            return List.of();
        }

        final var requestBodies = new ArrayList<JsonNode>();
        body.forEach(recordedRequest -> requestBodies.add(extractJsonBody(recordedRequest)));
        return requestBodies;
    }

    private JsonNode extractJsonBody(final JsonNode recordedRequest) {
        final var bodyNode = recordedRequest.path("body");

        if (bodyNode.isMissingNode() || bodyNode.isNull()) {
            return null;
        }

        if (bodyNode.has("json")) {
            return bodyNode.get("json");
        }

        if (bodyNode.has("string")) {
            try {
                return objectMapper.readTree(bodyNode.get("string").asText());
            } catch (IOException e) {
                return null;
            }
        }

        return bodyNode;
    }
}
