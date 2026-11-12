package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
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

import java.util.Map;

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
    @DisplayName("notifyRaidChanged sends the RAiD's handle to the updater's raid-changes endpoint with the API key")
    void notifyRaidChanged() {
        final var handle = "10.82841/86bf1b6c";
        final var httpEntity = mock(HttpEntity.class);
        final var uri = "_uri";
        final var response = mock(ResponseEntity.class);
        final var apiKey = "api-key";

        when(properties.getApiKey()).thenReturn(apiKey);

        final var raidChangesProperties = new OrcidIntegrationProperties.RaidChanges();
        raidChangesProperties.setUri(uri);

        when(properties.getRaidChanges()).thenReturn(raidChangesProperties);
        when(httpEntityFactory.create(Map.of("handle", handle), apiKey)).thenReturn(httpEntity);
        when(restTemplate.exchange(uri, HttpMethod.POST, httpEntity, Void.class)).thenReturn(response);

        orcidIntegrationClient.notifyRaidChanged(handle);

        verify(restTemplate).exchange(uri, HttpMethod.POST, httpEntity, Void.class);
    }
}
