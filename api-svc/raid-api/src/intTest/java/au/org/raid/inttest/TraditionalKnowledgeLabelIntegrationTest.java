package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.ValidationFailureResponse;
import au.org.raid.inttest.service.Handle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Client;
import feign.Request;
import feign.Response;
import feign.okhttp.OkHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * RAID-738: traditionalKnowledgeLabel has been withdrawn from the schema pending Indigenous
 * co-design. These are the primary acceptance tests for AC1 - any mint/update request whose raw
 * JSON body contains a top-level traditionalKnowledgeLabel property must be rejected with a 400
 * and a ValidationFailureResponse identifying the offending field.
 *
 * The field no longer exists on the generated request models, so the requests here are built by
 * serialising a valid request to a JsonNode and injecting the property directly, then POSTed/PUT
 * as raw JSON (bypassing the typed Feign client used elsewhere in this suite).
 */
public class TraditionalKnowledgeLabelIntegrationTest extends AbstractIntegrationTest {

    @Value("${raid.test.api.url}")
    private String apiUrl;

    @Test
    @DisplayName("Minting with a traditionalKnowledgeLabel property is rejected with a 400")
    void mintRejectsTraditionalKnowledgeLabel() throws IOException {
        final var body = withTraditionalKnowledgeLabel(objectMapper.valueToTree(createRequest));

        final var response = sendJson(Request.HttpMethod.POST, "/raid/", body);

        assertThat(response.status()).isEqualTo(400);

        final var failureResponse = objectMapper.readValue(readBody(response), ValidationFailureResponse.class);

        assertThat(failureResponse.getFailures())
                .anySatisfy(failure -> assertThat(failure.getFieldId()).isEqualTo("traditionalKnowledgeLabel"));
    }

    @Test
    @DisplayName("Updating with a traditionalKnowledgeLabel property is rejected with a 400")
    void updateRejectsTraditionalKnowledgeLabel() throws IOException {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert readResult != null;
        final var updateRequest = raidUpdateRequestFactory.create(readResult);

        final var body = withTraditionalKnowledgeLabel(objectMapper.valueToTree(updateRequest));

        final var response = sendJson(
                Request.HttpMethod.PUT, "/raid/%s/%s".formatted(handle.getPrefix(), handle.getSuffix()), body);

        assertThat(response.status()).isEqualTo(400);

        final var failureResponse = objectMapper.readValue(readBody(response), ValidationFailureResponse.class);

        assertThat(failureResponse.getFailures())
                .anySatisfy(failure -> assertThat(failure.getFieldId()).isEqualTo("traditionalKnowledgeLabel"));
    }

    @Test
    @DisplayName("Minting without a traditionalKnowledgeLabel property still succeeds")
    void mintSucceedsWithoutTraditionalKnowledgeLabel() {
        try {
            final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
            assertThat(mintedRaid).isNotNull();
            assertThat(mintedRaid.getIdentifier()).isNotNull();
        } catch (final Exception e) {
            failOnError(e);
        }
    }

    private JsonNode withTraditionalKnowledgeLabel(final JsonNode node) {
        final var objectNode = (ObjectNode) node;
        final var traditionalKnowledgeLabel = objectMapper.createArrayNode();
        traditionalKnowledgeLabel.add(objectMapper.createObjectNode()
                .put("id", "https://localcontexts.org/label/tk-attribution/"));
        objectNode.set("traditionalKnowledgeLabel", traditionalKnowledgeLabel);
        return objectNode;
    }

    private Response sendJson(final Request.HttpMethod method, final String path, final JsonNode body) throws IOException {
        final Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put(AUTHORIZATION, Arrays.asList("Bearer " + userContext.getToken()));

        final var request = Request.create(
                method,
                apiUrl + path,
                headers,
                objectMapper.writeValueAsBytes(body),
                StandardCharsets.UTF_8,
                null
        );

        final Client client = new OkHttpClient();
        return client.execute(request, new Request.Options(10, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, false));
    }

    private String readBody(final Response response) throws IOException {
        return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
