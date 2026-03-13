package au.org.raid.api.client.ror;

import au.org.raid.api.config.properties.RorClientProperties;
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
class RorRequestEntityFactoryTest {
    @Mock
    private RorClientProperties properties;
    @InjectMocks
    private RorRequestEntityFactory requestEntityFactory;

    @Test
    @DisplayName("Should return RequestEntity with valid input")
    void returnsRequestEntity() {
        final var baseUrl = "https://localhost/";
        final var clientId = "client-id";
        final var ror = "https://ror.org/038sjwq14";

        when(properties.getBaseUrl()).thenReturn(baseUrl);
        when(properties.getClientId()).thenReturn(clientId);

        final var request = requestEntityFactory.create(ror);

        assertThat(request.getMethod(), is(HttpMethod.GET));
        assertThat(request.getHeaders().get("Client-Id").get(0), is(clientId));
        assertThat(request.getUrl(), is(URI.create("https://localhost/organizations/038sjwq14")));
    }
}