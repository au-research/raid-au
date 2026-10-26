package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("OrcidOAuthClient")
class OrcidOAuthClientTest {
    private static final String TOKEN_URL = "https://sandbox.orcid.org/oauth/token";
    private static final String TOKEN_RESPONSE = """
            {"access_token": "access-token-value", "token_type": "bearer", "refresh_token": "refresh-token-value",
             "expires_in": 631138518, "scope": "/read-limited /activities/update", "name": "Josiah Carberry",
             "orcid": "0000-0002-1825-0097"}
            """;

    private MockRestServiceServer server;
    private OrcidOAuthClient client;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrcidOAuthClient(builder, TestProperties.complete());
    }

    @Test
    @DisplayName("Authorisation URL: ORCID's authorise endpoint with the client, scopes, callback and state")
    void authorisationUri() {
        final var uri = client.authorisationUri("state-value");
        final var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        assertThat(uri.getScheme() + "://" + uri.getHost() + uri.getPath()).isEqualTo("https://sandbox.orcid.org/oauth/authorize");
        assertThat(query.getFirst("client_id")).isEqualTo("APP-TEST");
        assertThat(query.getFirst("response_type")).isEqualTo("code");
        assertThat(query.getFirst("state")).isEqualTo("state-value");
        // encoded, and the trailing slash on the public base URL is not doubled
        assertThat(uri.getRawQuery()).contains("scope=/read-limited%20/activities/update")
                .contains("redirect_uri=https://orcid-updater.example.org/orcid/callback");
    }

    @Test
    @DisplayName("Exchange: posts the code with the client credentials and returns the iD as a URL")
    void exchange() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().formDataContains(java.util.Map.of(
                        "client_id", "APP-TEST", "client_secret", "test", "grant_type", "authorization_code",
                        "code", "Q70Y3A", "redirect_uri", "https://orcid-updater.example.org/orcid/callback")))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        final var signIn = client.exchange("Q70Y3A");

        assertThat(signIn.orcidId()).isEqualTo("https://sandbox.orcid.org/0000-0002-1825-0097");
        assertThat(signIn.accessToken()).isEqualTo("access-token-value");
        assertThat(signIn.refreshToken()).isEqualTo("refresh-token-value");
        assertThat(signIn.scope()).isEqualTo("/read-limited /activities/update");
        server.verify();
    }

    @Test
    @DisplayName("The sign-in result never prints its tokens")
    void toStringRedacted() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(client.exchange("code").toString()).doesNotContain("access-token-value", "refresh-token-value");
    }

    @Test
    @DisplayName("ORCID refuses the code (used or expired): sign-in failed, without the response body")
    void refused() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"invalid_grant\",\"error_description\":\"Reused authorization code: Q70Y3A\"}"));

        assertThatThrownBy(() -> client.exchange("Q70Y3A"))
                .isInstanceOf(OrcidSignInFailedException.class)
                .hasMessageContaining("400")
                .hasMessageNotContaining("Q70Y3A");
    }

    @Test
    @DisplayName("ORCID fails or can't be reached: sign-in failed")
    void unavailable() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.exchange("code")).isInstanceOf(OrcidSignInFailedException.class);

        server.reset();
        server.expect(requestTo(TOKEN_URL)).andRespond(withException(new IOException("connection reset")));
        assertThatThrownBy(() -> client.exchange("code")).isInstanceOf(OrcidSignInFailedException.class);
    }

    @Test
    @DisplayName("A response without a well-formed iD is not trusted")
    void malformedOrcidId() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(
                TOKEN_RESPONSE.replace("0000-0002-1825-0097", "https://evil.example/x"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchange("code")).isInstanceOf(OrcidSignInFailedException.class);
    }
}
