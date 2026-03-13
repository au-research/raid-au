package au.org.raid.api.client.orcid;

import au.org.raid.api.client.contributor.orcid.OrcidRequestEntityFactory;
import au.org.raid.api.config.properties.OrcidClientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcidRequestEntityFactoryTest {
    @Mock
    private OrcidClientProperties properties;
    @InjectMocks
    private OrcidRequestEntityFactory requestEntityFactory;

    @Test
    @DisplayName("Should return RequestEntity with valid input")
    void returnsRequestEntity() {
        final var baseUrl = "https://localhost/";
        final var accessToken = "_access-token";
        final var orcid = "https://orcid.org/0009-0002-5128-5184";

        when(properties.getBaseUrl()).thenReturn(baseUrl);
        when(properties.getAccessToken()).thenReturn(accessToken);

        final var request = requestEntityFactory.createGetPersonalDetailsRequest(orcid);

        assertThat(request.getMethod(), is(HttpMethod.GET));
        assertThat(request.getHeaders().get("Authorization").get(0), is("Bearer %s".formatted(accessToken)));
        assertThat(request.getUrl(), is(URI.create("https://localhost/0009-0002-5128-5184/personal-details")));
    }
}