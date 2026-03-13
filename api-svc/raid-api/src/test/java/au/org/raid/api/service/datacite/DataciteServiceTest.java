package au.org.raid.api.service.datacite;

import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.model.datacite.doi.DataciteRequest;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataciteServiceTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private DataciteProperties properties;
    @Mock
    private DataciteRequestFactory dataciteRequestFactory;
    @Mock
    private HttpEntityFactory httpEntityFactory;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private DataciteService dataciteService;

    @Test
    @DisplayName("Sends Datacite request on mint")
    void mint() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "_handle";
        final var endpoint = "_endpoint";
        final var raidRequest = new RaidCreateRequest();

        final var headers = new HttpHeaders();
        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, headers);

        when(dataciteRequestFactory.create(raidRequest, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);

        dataciteService.mint(raidRequest, handle, repositoryId, password);

        verify(restTemplate).exchange(endpoint, HttpMethod.POST, entity, JsonNode.class);
    }
}
