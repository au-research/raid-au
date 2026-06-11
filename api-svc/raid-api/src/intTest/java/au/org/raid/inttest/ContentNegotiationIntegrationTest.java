package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.RaidDto;
import feign.Client;
import feign.Feign;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Integration tests for the content negotiation feature.
 */
public class ContentNegotiationIntegrationTest extends AbstractIntegrationTest {

    @Value("${raid.test.api.url}")
    private String apiUrl;

    /**
     * Test that the API returns Turtle format when requested with Accept: text/turtle header.
     */
    @Test
    public void testTurtleFormatResponse() {
        // First mint a RAID
        RaidDto raid = mintRaid();

        // Then fetch it with Accept: text/turtle
        String raidId = raid.getIdentifier().getId();
        String handle = extractHandleFromRaidId(raidId);
        
        Response response = makeRequestWithAcceptHeader(handle, "text/turtle");
        
        // Verify response
        assertThat(response.status()).isEqualTo(200);
        assertThat(getHeaderValue(response, "Content-Type")).contains("text/turtle");
        
        String responseBody = getResponseBody(response);
        assertThat(responseBody).isNotEmpty();
        assertThat(responseBody).contains(raidId);
    }

    /**
     * Test that the API returns N-Triples format when requested with Accept: application/n-triples header.
     */
    @Test
    public void testNTriplesFormatResponse() {
        // First mint a RAID
        RaidDto raid = mintRaid();

        // Then fetch it with Accept: application/n-triples
        String raidId = raid.getIdentifier().getId();
        String handle = extractHandleFromRaidId(raidId);
        
        Response response = makeRequestWithAcceptHeader(handle, "application/n-triples");
        
        // Verify response
        assertThat(response.status()).isEqualTo(200);
        assertThat(getHeaderValue(response, "Content-Type")).contains("application/n-triples");
        
        String responseBody = getResponseBody(response);
        assertThat(responseBody).isNotEmpty();
        assertThat(responseBody).contains(raidId);
    }

    /**
     * Test that the API returns RDF/XML format when requested with Accept: application/rdf+xml header.
     */
    @Test
    public void testRdfXmlFormatResponse() {
        // First mint a RAID
        RaidDto raid = mintRaid();

        // Then fetch it with Accept: application/rdf+xml
        String raidId = raid.getIdentifier().getId();
        String handle = extractHandleFromRaidId(raidId);
        
        Response response = makeRequestWithAcceptHeader(handle, "application/rdf+xml");
        
        // Verify response
        assertThat(response.status()).isEqualTo(200);
        assertThat(getHeaderValue(response, "Content-Type")).contains("application/rdf+xml");
        
        String responseBody = getResponseBody(response);
        assertThat(responseBody).isNotEmpty();
        assertThat(responseBody).contains(raidId);
        assertThat(responseBody).contains("<rdf:RDF");
    }

    /**
     * Test that the API returns JSON by default when no specific Accept header is provided.
     */
    @Test
    public void testDefaultJsonFormatResponse() {
        // First mint a RAID
        RaidDto raid = mintRaid();

        // Then fetch it with no specific Accept header
        String raidId = raid.getIdentifier().getId();
        String handle = extractHandleFromRaidId(raidId);
        
        Response response = makeRequestWithAcceptHeader(handle, "*/*");
        
        // Verify response
        assertThat(response.status()).isEqualTo(200);
        assertThat(getHeaderValue(response, "Content-Type")).contains("application/json");
        
        String responseBody = getResponseBody(response);
        assertThat(responseBody).isNotEmpty();
        assertThat(responseBody).contains(raidId);
    }

    /**
     * Helper method to mint a new RAID for testing.
     */
    private RaidDto mintRaid() {
        try {
            return raidApi.mintRaid(createRequest).getBody();
        } catch (Exception e) {
            failOnError(e);
            return null; // This won't be reached due to failOnError throwing an exception
        }
    }

    /**
     * Extract the handle part from a RAID ID.
     */
    private String extractHandleFromRaidId(String raidId) {
        // Example: https://raid.org.au/10378.1/1696639 -> 10378.1/1696639
        String[] parts = raidId.split("/");
        return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }

    /**
     * Make a request to the RAID API with a specified Accept header.
     */
    private Response makeRequestWithAcceptHeader(String handle, String acceptHeader) {
        String[] handleParts = handle.split("/");
        String prefix = handleParts[0];
        String suffix = handleParts[1];
        
        try {
            Map<String, Collection<String>> headers = new HashMap<>();
            headers.put("Accept", Arrays.asList(acceptHeader));
            headers.put(AUTHORIZATION, Arrays.asList("Bearer " + userContext.getToken()));
            
            Request request = Request.create(
                    Request.HttpMethod.GET,
                    apiUrl + "/raid/" + prefix + "/" + suffix,
                    headers,
                    null,
                    StandardCharsets.UTF_8,
                    null
            );
            
            Client client = new OkHttpClient();
            return client.execute(request, new Request.Options(10, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, false));
        } catch (IOException e) {
            failOnError(e);
            return null; // This won't be reached due to failOnError throwing an exception
        }
    }

    /**
     * Get a header value from a response.
     */
    private String getHeaderValue(Response response, String headerName) {
        Map<String, Collection<String>> headers = response.headers();
        Collection<String> values = headers.get(headerName);
        if (values != null && !values.isEmpty()) {
            return values.iterator().next();
        }
        return "";
    }

    /**
     * Get the response body as a string.
     */
    private String getResponseBody(Response response) {
        try {
            return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            failOnError(e);
            return null; // This won't be reached due to failOnError throwing an exception
        }
    }
}