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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Throws HttpClientErrorException when Datacite mint fails")
    void mintThrowsOnError() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var endpoint = "_endpoint";
        final var raidRequest = new RaidCreateRequest();

        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, new HttpHeaders());

        when(dataciteRequestFactory.create(raidRequest, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);
        when(restTemplate.exchange(endpoint, HttpMethod.POST, entity, JsonNode.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> dataciteService.mint(raidRequest, handle, repositoryId, password))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("Throws RestClientException (not swallowed) when a ROR lookup during mint times out")
    void mintThrowsWhenRorLookupTimesOutDuringRequestCreation() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var raidRequest = new RaidCreateRequest();

        // Simulates DataciteRequestFactory.create() failing because one of the DataCite factories
        // (DatacitePublisherFactory / DataciteContributorFactory / DataciteFundingReferenceFactory)
        // calls rorClient.getOrganisationName(), which now uses the bounded uriValidatorRestTemplate
        // and can throw a ResourceAccessException on a slow/unreachable ROR resolver.
        when(dataciteRequestFactory.create(raidRequest, handle))
                .thenThrow(new ResourceAccessException("ROR lookup timed out"));

        assertThatThrownBy(() -> dataciteService.mint(raidRequest, handle, repositoryId, password))
                .isInstanceOf(ResourceAccessException.class);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Throws RestClientException (not swallowed) when the Datacite POST fails with a 5xx")
    void mintThrowsWhenDataciteReturnsServerError() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var endpoint = "_endpoint";
        final var raidRequest = new RaidCreateRequest();

        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, new HttpHeaders());

        when(dataciteRequestFactory.create(raidRequest, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);
        when(restTemplate.exchange(endpoint, HttpMethod.POST, entity, JsonNode.class))
                .thenThrow(HttpServerErrorException.create(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null, null, null));

        assertThatThrownBy(() -> dataciteService.mint(raidRequest, handle, repositoryId, password))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("Throws HttpClientErrorException when Datacite update (RaidUpdateRequest) fails")
    void updateRaidUpdateRequestThrowsOnError() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var endpoint = "_endpoint";
        final var request = new RaidUpdateRequest();

        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, new HttpHeaders());

        when(dataciteRequestFactory.create(request, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);
        when(restTemplate.exchange("%s/%s".formatted(endpoint, handle), HttpMethod.PUT, entity, JsonNode.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> dataciteService.update(request, handle, repositoryId, password))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("Throws HttpClientErrorException when Datacite update (RaidDto) fails")
    void updateRaidDtoThrowsOnError() {
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var handle = "10.12345/abcde";
        final var endpoint = "_endpoint";
        final var request = new RaidDto();

        final var dataciteRequest = new DataciteRequest();
        final var entity = new HttpEntity<>(dataciteRequest, new HttpHeaders());

        when(dataciteRequestFactory.create(request, handle)).thenReturn(dataciteRequest);
        when(httpEntityFactory.create(dataciteRequest, repositoryId, password)).thenReturn(entity);
        when(properties.getEndpoint()).thenReturn(endpoint);
        when(restTemplate.exchange("%s/%s".formatted(endpoint, handle), HttpMethod.PUT, entity, JsonNode.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> dataciteService.update(request, handle, repositoryId, password))
                .isInstanceOf(HttpClientErrorException.class);
    }
}
