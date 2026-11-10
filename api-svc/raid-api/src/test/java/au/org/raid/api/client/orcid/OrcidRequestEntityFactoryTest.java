package au.org.raid.api.client.orcid;

import au.org.raid.api.client.contributor.orcid.OrcidRequestEntityFactory;
import au.org.raid.api.config.properties.OrcidClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OrcidRequestEntityFactoryTest {
    private static final String BASE_URL = "https://localhost/";
    private static final String ORCID = "https://orcid.org/0009-0002-5128-5184";

    private OrcidClientProperties properties;
    private OrcidRequestEntityFactory requestEntityFactory;

    @BeforeEach
    void setUp() {
        properties = new OrcidClientProperties();
        properties.setBaseUrl(BASE_URL);
        requestEntityFactory = new OrcidRequestEntityFactory(properties);
    }

    @Test
    @DisplayName("GET personal-details carries the bearer token when one is configured")
    void personalDetailsRequestWithToken() {
        properties.setAccessToken("_access-token");

        final var request = requestEntityFactory.createGetPersonalDetailsRequest(ORCID);

        assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(request.getUrl()).isEqualTo(URI.create("https://localhost/0009-0002-5128-5184/personal-details"));
        assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer _access-token");
    }

    @Test
    @DisplayName("HEAD carries the bearer token when one is configured")
    void headRequestWithToken() {
        properties.setAccessToken("_access-token");

        final var request = requestEntityFactory.createHeadRequest(ORCID);

        assertThat(request.getMethod()).isEqualTo(HttpMethod.HEAD);
        assertThat(request.getUrl()).isEqualTo(URI.create("https://localhost/0009-0002-5128-5184"));
        assertThat(request.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer _access-token");
    }

    @ParameterizedTest(name = "token [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   ", "not-set", " not-set "})
    @DisplayName("GET personal-details is anonymous when no token is configured")
    void personalDetailsRequestWithoutToken(final String token) {
        properties.setAccessToken(token);

        final var request = requestEntityFactory.createGetPersonalDetailsRequest(ORCID);

        assertThat(request.getUrl()).isEqualTo(URI.create("https://localhost/0009-0002-5128-5184/personal-details"));
        assertThat(request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }

    @ParameterizedTest(name = "token [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   ", "not-set", " not-set "})
    @DisplayName("HEAD is anonymous when no token is configured")
    void headRequestWithoutToken(final String token) {
        properties.setAccessToken(token);

        final var request = requestEntityFactory.createHeadRequest(ORCID);

        assertThat(request.getUrl()).isEqualTo(URI.create("https://localhost/0009-0002-5128-5184"));
        assertThat(request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }
}
