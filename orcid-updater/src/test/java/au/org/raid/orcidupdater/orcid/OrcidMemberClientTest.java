package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import au.org.raid.orcidupdater.support.TestProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("OrcidMemberClient")
class OrcidMemberClientTest {
    private static final String ID = "https://sandbox.orcid.org/0000-0002-1825-0097";
    private static final String MEMBERSHIPS = "https://api.sandbox.orcid.org/v3.0/0000-0002-1825-0097/membership";
    private static final Map<String, Object> MEMBERSHIP = Map.of("department-name", "Coral reef resilience");

    private MockRestServiceServer server;
    private OrcidMemberClient client;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrcidMemberClient(builder, TestProperties.complete(), new ObjectMapper());
    }

    @Test
    @DisplayName("Create: POSTs ORCID JSON with the researcher's token and returns the put code from Location")
    void create() {
        server.expect(requestTo(MEMBERSHIPS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, "application/vnd.orcid+json"))
                .andExpect(content().json("{\"department-name\": \"Coral reef resilience\"}"))
                .andRespond(withCreatedEntity(URI.create(MEMBERSHIPS + "/1234567")));

        assertThat(client.create(ID, "access-token", MEMBERSHIP)).isEqualTo(1234567L);
        server.verify();
    }

    @Test
    @DisplayName("Update and delete address the entry by put code")
    void updateAndDelete() {
        server.expect(requestTo(MEMBERSHIPS + "/1234567")).andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess());
        server.expect(requestTo(MEMBERSHIPS + "/1234567")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        client.update(ID, "access-token", 1234567L, MEMBERSHIP);
        client.delete(ID, "access-token", 1234567L);
        server.verify();
    }

    @Test
    @DisplayName("Created without a Location: rejected, since the entry couldn't be updated later")
    void noLocation() {
        server.expect(requestTo(MEMBERSHIPS)).andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> client.create(ID, "t", MEMBERSHIP))
                .isInstanceOfSatisfying(OrcidWriteException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.REJECTED));
    }

    @Test
    @DisplayName("Errors are classified: 401 token rejected, 404 not found, 5xx and 429 retryable, others rejected")
    void errors() {
        assertKind(HttpStatus.UNAUTHORIZED, Kind.TOKEN_REJECTED);
        assertKind(HttpStatus.NOT_FOUND, Kind.NOT_FOUND);
        assertKind(HttpStatus.SERVICE_UNAVAILABLE, Kind.RETRYABLE);
        assertKind(HttpStatus.TOO_MANY_REQUESTS, Kind.RETRYABLE);
        assertKind(HttpStatus.BAD_REQUEST, Kind.REJECTED);
        assertKind(HttpStatus.CONFLICT, Kind.REJECTED);
    }

    @Test
    @DisplayName("No response: retryable")
    void unreachable() {
        server.expect(requestTo(MEMBERSHIPS)).andRespond(withException(new IOException("connect timed out")));

        assertThatThrownBy(() -> client.create(ID, "t", MEMBERSHIP))
                .isInstanceOfSatisfying(OrcidWriteException.class, e -> assertThat(e.getKind()).isEqualTo(Kind.RETRYABLE));
    }

    @Test
    @DisplayName("The failure keeps ORCID's developer message, never the token")
    void message() {
        server.expect(requestTo(MEMBERSHIPS)).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"response-code": 409, "developer-message": "409 Conflict: You have already added this activity",
                         "user-message": "There was an error", "error-code": 9021}
                        """));

        assertThatThrownBy(() -> client.create(ID, "secret-token", MEMBERSHIP))
                .hasMessage("ORCID returned 409: 409 Conflict: You have already added this activity")
                .message().doesNotContain("secret-token");
    }

    @Test
    @DisplayName("The member API takes the bare iD")
    void bareId() {
        assertThat(OrcidMemberClient.bareId(ID)).isEqualTo("0000-0002-1825-0097");
    }

    private void assertKind(final HttpStatus status, final Kind kind) {
        server.reset();
        server.expect(requestTo(MEMBERSHIPS)).andRespond(withStatus(status));
        assertThatThrownBy(() -> client.create(ID, "t", MEMBERSHIP))
                .isInstanceOfSatisfying(OrcidWriteException.class, e -> assertThat(e.getKind()).isEqualTo(kind));
    }
}
