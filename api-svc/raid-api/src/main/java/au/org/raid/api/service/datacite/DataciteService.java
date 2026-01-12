package au.org.raid.api.service.datacite;

import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.model.datacite.doi.DataciteRequest;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataciteService {
    private final DataciteProperties properties;
    private final RestTemplate restTemplate;
    private final DataciteRequestFactory dataciteRequestFactory;
    private final HttpEntityFactory httpEntityFactory;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void mint(final RaidCreateRequest request, final String handle,
                     String repositoryId, String password ){

        final DataciteRequest dataciteRequest = dataciteRequestFactory.create(request, handle);

        log.debug("POSTing Datacite request: {}", objectMapper.writeValueAsString(dataciteRequest));

        final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(dataciteRequest, repositoryId, password);
        log.debug("Making POST request to Datacite: {}", properties.getEndpoint());

        try {
            restTemplate.exchange(properties.getEndpoint(), HttpMethod.POST, entity, JsonNode.class);
        } catch (HttpClientErrorException e) {
            log.error("Unable to create Datacite record", e);
        }
    }

    @SneakyThrows
    public void update(RaidUpdateRequest request, String handle,
                       final String repositoryId, final String password) {

        final var endpoint = "%s/%s".formatted(properties.getEndpoint(), handle);

        final DataciteRequest dataciteRequest = dataciteRequestFactory.create(request, handle);

        log.debug("PUTting Datacite request: {}", objectMapper.writeValueAsString(dataciteRequest));

        final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(dataciteRequest, repositoryId, password);

        try {
            restTemplate.exchange(endpoint, HttpMethod.PUT, entity, JsonNode.class);
        } catch (HttpClientErrorException e) {
            log.error("Unable to update Datacite record", e);
            throw e;
        }
    }

    public void update(RaidDto request, String handle,
                       final String repositoryId, final String password) {

        final var endpoint = "%s/%s".formatted(properties.getEndpoint(), handle);

        final DataciteRequest dataciteRequest = dataciteRequestFactory.create(request, handle);
        final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(dataciteRequest, repositoryId, password);

        try {
            restTemplate.exchange(endpoint, HttpMethod.PUT, entity, JsonNode.class);
        } catch (HttpClientErrorException e) {
            log.error("Unable to update Datacite record", e);
        }
    }
}