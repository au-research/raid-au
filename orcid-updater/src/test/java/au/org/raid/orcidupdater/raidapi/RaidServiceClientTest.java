package au.org.raid.orcidupdater.raidapi;

import au.org.raid.orcidupdater.serviceauth.ServiceTokenClient;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("RaidServiceClient")
class RaidServiceClientTest {
    private static final String BASE = "http://raid-api.test";
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";

    private final ServiceTokenClient tokenClient = mock(ServiceTokenClient.class);
    private MockRestServiceServer server;
    private RaidServiceClient client;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RaidServiceClient(builder, TestProperties.complete(), tokenClient);
        when(tokenClient.accessToken()).thenReturn("service-token");
    }

    @Test
    @DisplayName("Reads a RAiD with the updater's own token")
    void findRaid() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c"))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("{\"identifier\": {\"id\": \"https://raid.org/10.82841/86bf1b6c\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.findRaid(HANDLE)).get()
                .satisfies(raid -> assertThat(raid.path("identifier").path("id").asText()).endsWith(HANDLE));
    }

    @Test
    @DisplayName("No such RAiD: empty")
    void findRaidNotFound() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findRaid(HANDLE)).isEmpty();
    }

    @Test
    @DisplayName("Lists RAiDs by the bare iD, and takes each handle from the identifier URL")
    void findHandles() {
        server.expect(requestTo(BASE + "/raid/?contributor.id=0000-0002-1825-0097&includeFields=identifier"))
                .andRespond(withSuccess("""
                        [{"identifier": {"id": "https://raid.org/10.82841/86bf1b6c"}},
                         {"identifier": {"id": "https://raid.org/10.82841/2"}},
                         {"identifier": {}}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.findHandlesByContributor(ID)).containsExactly(HANDLE, "10.82841/2");
    }

    @Test
    @DisplayName("Sets a status with PUT /contributor/status")
    void updateStatus() {
        server.expect(requestTo(BASE + "/contributor/status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andExpect(content().json("{\"id\": \"%s\", \"status\": \"AUTHENTICATED\"}".formatted(ID), true))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThat(client.updateContributorStatus(ID, ContributorStatus.AUTHENTICATED)).isTrue();
    }

    @Test
    @DisplayName("No contributor with this iD: false")
    void updateStatusNoContributor() {
        server.expect(requestTo(BASE + "/contributor/status")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.updateContributorStatus(ID, ContributorStatus.AUTHENTICATED)).isFalse();
    }

    @Test
    @DisplayName("401: the cached token is dropped and the call tried once more with a new one")
    void retriesOnceOn401() {
        when(tokenClient.accessToken()).thenReturn("stale-token", "fresh-token");
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c"))
                .andExpect(header("Authorization", "Bearer stale-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c"))
                .andExpect(header("Authorization", "Bearer fresh-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.findRaid(HANDLE)).isPresent();
        verify(tokenClient).reset();
    }

    @Test
    @DisplayName("401 twice: unavailable, so the worker tries later")
    void twice401() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.findRaid(HANDLE)).isInstanceOf(RaidApiUnavailableException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("403 (client lacks contributor-writer) and 5xx: unavailable, with no token in the message")
    void otherErrors() {
        server.expect(requestTo(BASE + "/contributor/status")).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.updateContributorStatus(ID, ContributorStatus.AUTHENTICATED))
                .isInstanceOf(RaidApiUnavailableException.class)
                .hasMessageContaining("403")
                .hasMessageNotContaining("service-token");
    }

    @Test
    @DisplayName("No response: unavailable")
    void unreachable() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c")).andRespond(withException(new IOException("refused")));

        assertThatThrownBy(() -> client.findRaid(HANDLE)).isInstanceOf(RaidApiUnavailableException.class);
    }
}
