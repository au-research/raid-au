package au.org.raid.api;

import au.org.raid.api.service.raid.RaidService;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Disabled("This test has never passed")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://mock-keycloak:8080/realms/raid-dev/protocol/openid-connect/certs"
    }
)
@ActiveProfiles("test")
public class ContentNegotiationTest {

    @TestConfiguration
    @EnableAutoConfiguration(exclude = {
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    })
    public static class TestSecurityConfig {
        @Bean
        @Primary
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2ResourceServer(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    @MockBean
    private RaidService raidService;

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    /**
     * Set up a mock RAID record to be returned by the service
     */
    private void setupMockRaid() {
        // Create a minimal RaidDto for testing
        RaidDto mockRaid = new RaidDto()
            .identifier(new Id()
                .id("https://raid.org.au/10378.1/1696639")
                .schemaUri("https://raid.org/schemas/identifier")
                .registrationAgency(new RegistrationAgency()
                    .id("https://raid.org/")
                    .schemaUri("https://raid.org/schemas/registrationAgency"))
                .owner(new Owner()
                    .id("https://raid.org/")
                    .schemaUri("https://raid.org/schemas/owner")
                    .servicePoint(1L)))
            .title(List.of(new Title()
                .text("Test RAID")
                .type(new TitleType()
                    .id("https://raid.org/titles/primary")
                    .schemaUri("https://raid.org/schemas/titleType"))
                .language(new Language()
                    .id("en")
                    .schemaUri("https://raid.org/schemas/language"))))
            .date(new Date()
                .startDate("2022-01-01")
                .endDate("2023-01-01"))
            .access(new Access()
                .type(new AccessType()
                    .id("https://raid.org/access/open")
                    .schemaUri("https://raid.org/schemas/accessType")));

        when(raidService.findByHandle(anyString())).thenReturn(Optional.of(mockRaid));
    }

    /**
     * Test that the API can return Turtle format when requested.
     */
    @Test
    public void testTurtleFormat() {
        // Set up mock
        setupMockRaid();
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "text/turtle");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make request
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/raid/10378.1/1696639"),
                HttpMethod.GET, entity, String.class);

        // Assert response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/turtle");
        assertThat(response.getBody()).contains("https://raid.org.au/10378.1/1696639");
    }

    /**
     * Test that the API can return N-Triples format when requested.
     */
    @Test
    public void testNTriplesFormat() {
        // Set up mock
        setupMockRaid();
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/n-triples");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make request
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/raid/10378.1/1696639"),
                HttpMethod.GET, entity, String.class);

        // Assert response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/n-triples");
        assertThat(response.getBody()).contains("https://raid.org.au/10378.1/1696639");
    }

    /**
     * Test that the API can return RDF/XML format when requested.
     */
    @Test
    public void testRdfXmlFormat() {
        // Set up mock
        setupMockRaid();
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/rdf+xml");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make request
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/raid/10378.1/1696639"),
                HttpMethod.GET, entity, String.class);

        // Assert response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/rdf+xml");
        assertThat(response.getBody()).contains("https://raid.org.au/10378.1/1696639");
    }

    /**
     * Test that the API defaults to JSON when no specific Accept header is provided.
     */
    @Test
    public void testDefaultJsonFormat() {
        // Set up mock
        setupMockRaid();
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make request
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/raid/10378.1/1696639"),
                HttpMethod.GET, entity, String.class);

        // Assert response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/json");
        assertThat(response.getBody()).contains("https://raid.org.au/10378.1/1696639");
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }
}