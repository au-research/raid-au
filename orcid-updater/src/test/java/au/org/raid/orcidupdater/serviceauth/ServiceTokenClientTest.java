package au.org.raid.orcidupdater.serviceauth;

import au.org.raid.orcidupdater.support.TestProperties;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ServiceTokenClient")
class ServiceTokenClientTest {
    private static final Instant NOW = Instant.parse("2026-10-27T01:00:00Z");
    private static final String ISSUER = "http://iam.test/realms/raid";
    private static final String TOKEN_URL = ISSUER + "/protocol/openid-connect/token";
    private static final String TOKEN_RESPONSE = """
            {"access_token": "service-token", "expires_in": 300, "token_type": "Bearer"}
            """;

    private final ServiceKeyStore keyStore = mock(ServiceKeyStore.class);
    private final MutableClock clock = new MutableClock(NOW);
    private RSAKey key;
    private MockRestServiceServer server;
    private ServiceTokenClient client;

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(final Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static Map<String, String> form(final MockClientHttpRequest request) {
        return Arrays.stream(request.getBodyAsString().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

    @BeforeEach
    void setUp() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        when(keyStore.signingKey()).thenReturn(key);
        final var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ServiceTokenClient(builder, keyStore, TestProperties.complete(), clock);
    }

    @Test
    @DisplayName("Client credentials with a signed client assertion, and no client secret")
    void request() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    final var form = form((MockClientHttpRequest) request);
                    assertThat(form).containsEntry("grant_type", "client_credentials")
                            .containsEntry("client_id", "orcid-updater")
                            .containsEntry("client_assertion_type", ServiceTokenClient.ASSERTION_TYPE)
                            .doesNotContainKey("client_secret");
                    assertThat(form.get("client_assertion")).isNotBlank();
                })
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(client.accessToken()).isEqualTo("service-token");
    }

    @Test
    @DisplayName("The assertion is RS256 with the key id, from and about the client, for the issuer, for 60 seconds")
    void assertion() throws Exception {
        final var jwt = SignedJWT.parse(client.assertion(NOW));

        assertThat(jwt.verify(new RSASSAVerifier(key.toPublicJWK()))).isTrue();
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("kid-1");
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        final var claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("orcid-updater");
        assertThat(claims.getSubject()).isEqualTo("orcid-updater");
        assertThat(claims.getAudience()).containsExactly(ISSUER);
        assertThat(claims.getJWTID()).isNotBlank();
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(NOW.plusSeconds(60));
        // a new id each time, so Keycloak's replay check never refuses a fresh assertion
        assertThat(SignedJWT.parse(client.assertion(NOW)).getJWTClaimsSet().getJWTID()).isNotEqualTo(claims.getJWTID());
    }

    @Test
    @DisplayName("The token is reused until 30 seconds before it expires")
    void cached() {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess("""
                {"access_token": "second-token", "expires_in": 300}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.accessToken()).isEqualTo("service-token");
        clock.now = NOW.plusSeconds(269);
        assertThat(client.accessToken()).isEqualTo("service-token");
        clock.now = NOW.plusSeconds(270);
        assertThat(client.accessToken()).isEqualTo("second-token");
        server.verify();
    }

    @Test
    @DisplayName("After reset the next call fetches a new token")
    void reset() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("""
                {"access_token": "second-token", "expires_in": 300}
                """, MediaType.APPLICATION_JSON));

        client.accessToken();
        client.reset();

        assertThat(client.accessToken()).isEqualTo("second-token");
    }

    @Test
    @DisplayName("Keycloak refuses: only its error code is kept, never its description")
    void refused() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"invalid_client\", \"error_description\": \"Invalid client or Invalid client credentials\"}"));

        assertThatThrownBy(() -> client.accessToken()).isInstanceOf(ServiceTokenException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("invalid_client")
                .hasMessageNotContaining("Invalid client credentials");
    }

    @Test
    @DisplayName("Keycloak unreachable or answering without a token: ServiceTokenException")
    void unreachable() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withException(new IOException("refused")));
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.accessToken()).isInstanceOf(ServiceTokenException.class);
        assertThatThrownBy(() -> client.accessToken()).isInstanceOf(ServiceTokenException.class)
                .hasMessageContaining("no access token");
    }
}
