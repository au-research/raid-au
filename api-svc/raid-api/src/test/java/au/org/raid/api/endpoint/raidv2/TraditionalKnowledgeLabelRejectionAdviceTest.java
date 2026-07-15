package au.org.raid.api.endpoint.raidv2;

import au.org.raid.api.exception.ValidationException;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidPatchRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraditionalKnowledgeLabelRejectionAdviceTest {

    private final TraditionalKnowledgeLabelRejectionAdvice advice =
            new TraditionalKnowledgeLabelRejectionAdvice(new ObjectMapper());

    @Test
    @DisplayName("supports() returns true for RaidCreateRequest and RaidUpdateRequest, false otherwise")
    void supports_targetsCreateAndUpdateRequestsOnly() {
        assertThat(advice.supports(null, RaidCreateRequest.class, null)).isTrue();
        assertThat(advice.supports(null, RaidUpdateRequest.class, null)).isTrue();
        assertThat(advice.supports(null, RaidPatchRequest.class, null)).isFalse();
    }

    @Test
    @DisplayName("beforeBodyRead throws ValidationException when body contains traditionalKnowledgeLabel")
    void beforeBodyRead_rejectsBodyContainingTraditionalKnowledgeLabel() {
        final var inputMessage = messageFor("""
                {
                  "title": [],
                  "traditionalKnowledgeLabel": [
                    { "id": "https://localcontexts.org/label/tk-attribution/" }
                  ]
                }
                """);

        assertThatThrownBy(() -> advice.beforeBodyRead(inputMessage, null, RaidCreateRequest.class, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> {
                    final var failures = ((ValidationException) e).getFailures();
                    assertThat(failures).hasSize(1);
                    assertThat(failures.get(0).getFieldId()).isEqualTo("traditionalKnowledgeLabel");
                    assertThat(failures.get(0).getErrorType()).isEqualTo("invalidValue");
                    assertThat(failures.get(0).getMessage())
                            .isEqualTo("The traditionalKnowledgeLabel field is not currently supported.");
                });
    }

    @Test
    @DisplayName("beforeBodyRead passes body through unchanged when traditionalKnowledgeLabel is absent")
    void beforeBodyRead_passesThroughBodyWithoutTraditionalKnowledgeLabel() throws IOException {
        final var json = """
                {
                  "title": []
                }
                """;
        final var inputMessage = messageFor(json);

        final var result = advice.beforeBodyRead(inputMessage, null, RaidCreateRequest.class, null);

        assertThat(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
        assertThat(result.getHeaders()).isSameAs(inputMessage.getHeaders());
    }

    @Test
    @DisplayName("beforeBodyRead passes malformed JSON through unchanged rather than failing")
    void beforeBodyRead_passesThroughMalformedJson() throws IOException {
        final var json = "{ not valid json";
        final var inputMessage = messageFor(json);

        final var result = advice.beforeBodyRead(inputMessage, null, RaidCreateRequest.class, null);

        assertThat(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @Test
    @DisplayName("beforeBodyRead only rejects a top-level property, not a nested traditionalKnowledgeLabel key")
    void beforeBodyRead_ignoresNestedTraditionalKnowledgeLabel() throws IOException {
        final var json = """
                {
                  "title": [
                    { "traditionalKnowledgeLabel": "not the top-level field" }
                  ]
                }
                """;
        final var inputMessage = messageFor(json);

        final var result = advice.beforeBodyRead(inputMessage, null, RaidCreateRequest.class, null);

        assertThat(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @Test
    @DisplayName("beforeBodyRead passes an empty body through unchanged rather than failing")
    void beforeBodyRead_passesThroughEmptyBody() throws IOException {
        final var json = "";
        final var inputMessage = messageFor(json);

        final var result = advice.beforeBodyRead(inputMessage, null, RaidCreateRequest.class, null);

        assertThat(new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    private static HttpInputMessage messageFor(final String json) {
        final var headers = new HttpHeaders();
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }
}
