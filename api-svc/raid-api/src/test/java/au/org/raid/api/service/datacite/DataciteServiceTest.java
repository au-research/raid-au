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

import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;

import static org.mockito.Mockito.*;

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
    @DisplayName("Sends Datacite request on mint for DOI handle")
    void mint() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
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

    @Test
    @DisplayName("Skips Datacite mint for non-DOI handle")
    void mintSkipsNonDoiHandle() {
        dataciteService.mint(new RaidCreateRequest(), "102.100.100/447187", "repo", "pass");

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Sends Datacite request on update (RaidUpdateRequest) for DOI handle")
    void updateRaidUpdateRequest() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var endpoint = "_endpoint";
        final var request = new RaidUpdateRequest();

        final var headers = new HttpHeaders();
        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, headers);

        when(dataciteRequestFactory.create(request, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);

        dataciteService.update(request, handle, repositoryId, password);

        verify(restTemplate).exchange("%s/%s".formatted(endpoint, handle), HttpMethod.PUT, entity, JsonNode.class);
    }

    @Test
    @DisplayName("Skips Datacite update (RaidUpdateRequest) for non-DOI handle")
    void updateRaidUpdateRequestSkipsNonDoiHandle() {
        dataciteService.update(new RaidUpdateRequest(), "102.100.100/447187", "repo", "pass");

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Sends Datacite request on update (RaidDto) for DOI handle")
    void updateRaidDto() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var endpoint = "_endpoint";
        final var request = new RaidDto();

        final var headers = new HttpHeaders();
        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, headers);

        when(dataciteRequestFactory.create(request, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);

        dataciteService.update(request, handle, repositoryId, password);

        verify(restTemplate).exchange("%s/%s".formatted(endpoint, handle), HttpMethod.PUT, entity, JsonNode.class);
    }

    @Test
    @DisplayName("Skips Datacite update (RaidDto) for non-DOI handle")
    void updateRaidDtoSkipsNonDoiHandle() {
        dataciteService.update(new RaidDto(), "10378.1/1700205", "repo", "pass");

        verifyNoInteractions(restTemplate);
    }
}
