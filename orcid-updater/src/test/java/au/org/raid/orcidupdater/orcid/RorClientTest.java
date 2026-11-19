package au.org.raid.orcidupdater.orcid;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("RorClient")
class RorClientTest {
    private static final String ARDC = "https://ror.org/038sjwq14";
    private static final String LOOKUP = "https://api.ror.org/v2/organizations/038sjwq14";
    // trimmed from ROR's v2 response for ARDC
    private static final String RESPONSE = """
            {"id": "https://ror.org/038sjwq14",
             "names": [{"value": "ARDC", "types": ["acronym"]},
                       {"value": "Australian Research Data Commons", "types": ["ror_display", "label"]}],
             "locations": [{"geonames_details": {"name": "Melbourne", "country_code": "AU",
                                                 "country_subdivision_name": "Victoria"}}]}
            """;

    private MockRestServiceServer server;
    private RorClient client;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RorClient(builder, TestProperties.complete());
    }

    @Test
    @DisplayName("Display name, city, region and country of the organisation")
    void found() {
        server.expect(requestTo(LOOKUP)).andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(client.find(ARDC)).contains(new OrcidOrganisation("Australian Research Data Commons",
                "Melbourne", "Victoria", "AU", ARDC));
        server.verify();
    }

    @Test
    @DisplayName("Not a ROR id: no lookup, no organisation")
    void notRor() {
        assertThat(client.find("https://example.org/038sjwq14")).isEmpty();
        assertThat(client.find(null)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("ROR fails, is unreachable or lacks a city: no organisation, no exception")
    void failures() {
        server.expect(requestTo(LOOKUP)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.find(ARDC)).isEmpty();

        server.reset();
        server.expect(requestTo(LOOKUP)).andRespond(withException(new IOException("timed out")));
        assertThat(client.find(ARDC)).isEmpty();

        server.reset();
        server.expect(requestTo(LOOKUP)).andRespond(withSuccess("""
                {"names": [{"value": "No city", "types": ["ror_display"]}], "locations": []}
                """, MediaType.APPLICATION_JSON));
        assertThat(client.find(ARDC)).isEmpty();
    }
}
