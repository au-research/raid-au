package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.RaidListenerMessage;
import au.org.raid.api.factory.HttpEntityFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcidIntegrationClientTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private OrcidIntegrationProperties properties;
    @Mock
    private HttpEntityFactory httpEntityFactory;
    @InjectMocks
    private OrcidIntegrationClient orcidIntegrationClient;

    @Test
    @DisplayName("post method sends POST request to raid listener")
    void post() {
        final var message = new RaidListenerMessage();
        final var httpEntity = mock(HttpEntity.class);
        final var uri = "_uri";
        final var response = mock(ResponseEntity.class);
        final var apiKey = "api-key";

        when(properties.getApiKey()).thenReturn(apiKey);

        final var raidListenerProperties = new OrcidIntegrationProperties.RaidListener();
        raidListenerProperties.setUri(uri);

        when(properties.getRaidListener()).thenReturn(raidListenerProperties);
        when(httpEntityFactory.create(message, apiKey)).thenReturn(httpEntity);
        when(restTemplate.exchange(uri, HttpMethod.POST, httpEntity, Void.class)).thenReturn(response);

        orcidIntegrationClient.post(message);

        verify(restTemplate).exchange(uri, HttpMethod.POST, httpEntity, Void.class);
    }
}