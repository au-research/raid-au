package au.org.raid.inttest;

import au.org.raid.fixtures.APIFixtures;
import au.org.raid.idl.raidv2.model.RelatedRaid;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeSchemaUriEnum;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box, non-gated companion to {@code DataciteLiveRelatedRaidIntegrationTest}
 * (api-svc/raid-api/src/test/java/au/org/raid/api/datacite/DataciteLiveRelatedRaidIntegrationTest.java),
 * which requires real DataCite credentials and is disabled by default. This test runs in the normal
 * intTest suite with no external credentials, driving the full app (controller -&gt; service -&gt;
 * DataciteAttributesDtoFactory -&gt; DataciteService -&gt; DataciteRelatedIdentifierFactory) against
 * the local DataCite MockServer instance, and proves the outbound DataCite payload for a RAiD minted
 * with a {@code relatedRaid} contains a {@code relatedIdentifier} of type {@code "RAiD"} (RAID-797).
 *
 * <p>It does not clear or override MockServer's static {@code /dois} expectations (see
 * docker-compose/mockserver/expectations.json) - other tests, including the sentinel-title 429
 * expectation used by {@link DataciteErrorIntegrationTest}, rely on that shared state remaining
 * intact. Instead, it identifies its own request among any recorded traffic by embedding a unique
 * random handle value (via the freshly minted target RAiD's identifier) and matching MockServer's
 * recorded request bodies against it.
 */
public class DataciteRelatedRaidMockIntegrationTest extends AbstractIntegrationTest {

    private static final String MOCKSERVER_HOST = "localhost";
    private static final int MOCKSERVER_PORT = 1080;
    private static final int MOCKSERVER_CONNECT_TIMEOUT_MILLIS = 2000;
    // MockServer 5.x unifies request/log/expectation retrieval behind /mockserver/retrieve with a
    // `type` query param (the older /mockserver/retrieveRecordedRequests path 404s on this version).
    private static final String MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL =
            "http://" + MOCKSERVER_HOST + ":" + MOCKSERVER_PORT + "/mockserver/retrieve?type=REQUESTS";

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    void checkMockServerIsReachable() {
        Assumptions.assumeTrue(isMockServerReachable(),
                "mockserver not reachable - skipping DataCite related RAiD mock test");
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
    @DisplayName("Minting a RAiD with a relatedRaid emits a DataCite relatedIdentifier of type RAiD (RAID-797)")
    void mintWithRelatedRaidEmitsDataciteRaidType() {
        final var uniqueMarker = UUID.randomUUID().toString();

        // 1. Mint the target RAiD that the source RAiD below will point at.
        createRequest.getTitle().get(0).setText("RAID-797-target-" + uniqueMarker);

        final var targetRaid = raidApi.mintRaid(createRequest).getBody();
        assertThat(targetRaid).isNotNull();

        final var targetHandle = targetRaid.getIdentifier().getId();
        assertThat(targetHandle).isNotBlank();

        // 2. Mint a second (source) RAiD with a relatedRaid pointing at the target's handle, using
        // a valid related-raid type from DataciteRelatedIdentifierFactory.RAID_RELATION_TYPE_MAP.
        final var sourceCreateRequest = APIFixtures.newCreateRequest();
        sourceCreateRequest.getTitle().get(0).setText("RAID-797-source-" + uniqueMarker);
        sourceCreateRequest.setRelatedRaid(List.of(new RelatedRaid()
                .id(targetHandle)
                .type(new RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_204)
                        .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367))));

        final var sourceRaid = raidApi.mintRaid(sourceCreateRequest).getBody();
        assertThat(sourceRaid).isNotNull();

        // 3. Retrieve the DataCite requests MockServer recorded for the /dois route (without
        // clearing any state) and find this test's own mint by the unique target handle embedded
        // in its body.
        final var recordedRequestBodies = retrieveRecordedDoisRequestBodies();

        final var matchingRequestBody = recordedRequestBodies.stream()
                .filter(body -> body != null && body.toString().contains(targetHandle))
                .reduce((first, second) -> second) // prefer the most recent matching recorded request
                .orElse(null);

        assertThat(matchingRequestBody)
                .as("Expected a recorded DataCite /dois request body referencing target handle %s", targetHandle)
                .isNotNull();

        final var relatedIdentifiers = matchingRequestBody.path("data").path("attributes").path("relatedIdentifiers");
        assertThat(relatedIdentifiers.isArray()).isTrue();

        final var candidates = new ArrayList<JsonNode>();
        relatedIdentifiers.forEach(candidates::add);

        final var raidRelatedIdentifier = candidates.stream()
                .filter(node -> targetHandle.equals(node.path("relatedIdentifier").asText()))
                .findFirst()
                .orElse(null);

        assertThat(raidRelatedIdentifier)
                .as("Expected a relatedIdentifier entry for the target handle in the recorded DataCite request")
                .isNotNull();
        assertThat(raidRelatedIdentifier.path("relatedIdentifierType").asText()).isEqualTo("RAiD");
        assertThat(raidRelatedIdentifier.path("resourceTypeGeneral").asText()).isEqualTo("Project");
        assertThat(raidRelatedIdentifier.path("relatedIdentifier").asText()).isEqualTo(targetHandle);
    }

    private List<JsonNode> retrieveRecordedDoisRequestBodies() {
        final var matcher = objectMapper.createObjectNode();
        matcher.put("path", "/dois");
        matcher.put("method", "POST");

        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var entity = new HttpEntity<>(matcher.toString(), headers);
        final var response = restTemplate.exchange(
                MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL,
                HttpMethod.PUT,
                entity,
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

        // MockServer represents a recorded JSON body either as the raw JSON value itself, or
        // wrapped as {"type":"JSON", "json": {...}} / {"type":"STRING", "string": "..."} depending
        // on version/content negotiation - handle both shapes defensively.
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
