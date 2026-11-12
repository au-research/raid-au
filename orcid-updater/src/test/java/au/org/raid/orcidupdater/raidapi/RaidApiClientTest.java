package au.org.raid.orcidupdater.raidapi;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("RaidApiClient")
class RaidApiClientTest {
    private static final String BASE = "http://raid-api.test";
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String TOKEN = "minter-token";

    private static final String RAID = """
            {
              "identifier": {"owner": {"servicePoint": 20000001}},
              "title": [
                {"text": "Alternative title", "type": {"id": "https://vocabulary.raid.org/title.type.schema/4"}},
                {"text": "Coral reef resilience", "type": {"id": "https://vocabulary.raid.org/title.type.schema/5"}}
              ],
              "contributor": [
                {"id": "https://orcid.org/0000-0002-1825-0097"},
                {"id": "https://orcid.org/0000-0001-5109-3700"},
                {"position": []}
              ]
            }
            """;

    private static final String SERVICE_POINT = """
            {"id": 20000001, "name": "Reef Research Institute",
             "adminEmail": "admin@reef.example.org", "techEmail": "tech@reef.example.org"}
            """;

    private MockRestServiceServer server;
    private RaidApiClient client;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RaidApiClient(builder, TestProperties.complete());
    }

    private void expectPermissions(final String body) {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c/permissions"))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Reads the primary title, the service point name only, and every contributor iD")
    void readsSummary() {
        expectPermissions("{\"servicePointMatch\": true, \"read\": true, \"write\": true}");
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c"))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(RAID, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/service-point/20000001"))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(SERVICE_POINT, MediaType.APPLICATION_JSON));

        final var summary = client.findEditableRaid(HANDLE, TOKEN);

        assertThat(summary.handle()).isEqualTo(HANDLE);
        assertThat(summary.title()).isEqualTo("Coral reef resilience");
        assertThat(summary.servicePointName()).isEqualTo("Reef Research Institute");
        assertThat(summary.toString()).doesNotContain("admin@reef", "tech@reef");
        assertThat(summary.contributorIds()).containsExactly(
                "https://orcid.org/0000-0002-1825-0097", "https://orcid.org/0000-0001-5109-3700");
        server.verify();
    }

    @Test
    @DisplayName("Minter can read but not edit: not permitted, and the RAiD is not read")
    void readOnlyMinter() {
        expectPermissions("{\"servicePointMatch\": false, \"read\": true, \"write\": false}");

        assertThatThrownBy(() -> client.findEditableRaid(HANDLE, TOKEN))
                .isInstanceOf(MinterNotPermittedException.class);
        server.verify();
    }

    @Test
    @DisplayName("RAiD API answers 404: RAiD not found")
    void notFound() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c/permissions"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.findEditableRaid(HANDLE, TOKEN)).isInstanceOf(RaidNotFoundException.class);
    }

    @Test
    @DisplayName("RAiD API refuses the minter's token: not permitted")
    void refusedToken() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c/permissions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.findEditableRaid(HANDLE, TOKEN))
                .isInstanceOf(MinterNotPermittedException.class);
    }

    @Test
    @DisplayName("RAiD API fails: unavailable")
    void serverError() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c/permissions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.findEditableRaid(HANDLE, TOKEN))
                .isInstanceOf(RaidApiUnavailableException.class);
    }

    @Test
    @DisplayName("RAiD API unreachable: unavailable")
    void unreachable() {
        server.expect(requestTo(BASE + "/raid/10.82841/86bf1b6c/permissions"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.findEditableRaid(HANDLE, TOKEN))
                .isInstanceOf(RaidApiUnavailableException.class);
    }
}
