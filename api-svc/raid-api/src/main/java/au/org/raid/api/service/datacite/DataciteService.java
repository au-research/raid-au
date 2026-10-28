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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Wired by {@link au.org.raid.api.config.bean.ExternalPidService}, which substitutes
 * {@link au.org.raid.api.service.stub.DataciteServiceStub} when raid.stub.datacite.enabled=true.
 */
@Slf4j
@RequiredArgsConstructor
public class DataciteService {
    private static final String DOI_PREFIX = "10.";
    private static final String REGISTER_EVENT = "register";
    private static final String HIDE_EVENT = "hide";

    private final DataciteProperties properties;
    private final RestTemplate restTemplate;
    private final DataciteRequestFactory dataciteRequestFactory;
    private final HttpEntityFactory httpEntityFactory;
    private final ObjectMapper objectMapper;

    protected boolean isDoi(final String handle) {
        return handle != null && handle.startsWith(DOI_PREFIX);
    }

    /**
     * An update to a non-open RAiD must hide its DOI. DataCite ignores "register" on a findable DOI,
     * so a RAiD changed from open to embargoed would otherwise stay findable. "hide" moves a findable
     * DOI to registered. See RAID-1012.
     */
    private void hideNonOpenDoi(final DataciteRequest dataciteRequest) {
        final var data = dataciteRequest.getData();

        if (data != null && data.getAttributes() != null
                && REGISTER_EVENT.equals(data.getAttributes().getEvent())) {
            data.getAttributes().setEvent(HIDE_EVENT);
        }
    }

    @SneakyThrows
    public void mint(final RaidCreateRequest request, final String handle,
                     String repositoryId, String password ){

        if (!isDoi(handle)) {
            log.debug("Skipping Datacite mint for non-DOI handle: {}", handle);
            return;
        }

        try {
            final DataciteRequest dataciteRequest = dataciteRequestFactory.create(request, handle);

            log.debug("POSTing Datacite request: {}", objectMapper.writeValueAsString(dataciteRequest));

            final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(dataciteRequest, repositoryId, password);
            log.debug("Making POST request to Datacite: {}", properties.getEndpoint());

            restTemplate.exchange(properties.getEndpoint(), HttpMethod.POST, entity, JsonNode.class);
        } catch (HttpClientErrorException e) {
            // Kept as its own catch, before the broader RestClientException below, so this
            // exception type still reaches RaidService.mintHandle's 422-retry logic unchanged.
            log.error("Unable to create Datacite record", e);
            throw e;
        } catch (RestClientException e) {
            // Covers ResourceAccessException (connect/read timeout, DNS failure, connection refused)
            // and HttpServerErrorException (5xx), raised either by the ROR lookups the DataCite request
            // factories perform (dataciteRequestFactory.create -> rorClient.getOrganisationName) or by
            // the POST to DataCite itself. Logged here for service-layer context and rethrown
            // unchanged; RaidExceptionHandler.handleRestClientException renders the client-facing
            // structured 502 response once it escapes the controller boundary. See RAID-803.
            log.error("Unable to create Datacite record", e);
            throw e;
        }
    }

    @SneakyThrows
    public void update(RaidUpdateRequest request, String handle,
                       final String repositoryId, final String password) {

        if (!isDoi(handle)) {
            log.debug("Skipping Datacite update for non-DOI handle: {}", handle);
            return;
        }

        final var endpoint = "%s/%s".formatted(properties.getEndpoint(), handle);

        final DataciteRequest dataciteRequest = dataciteRequestFactory.create(request, handle);
        hideNonOpenDoi(dataciteRequest);

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

        if (!isDoi(handle)) {
            log.debug("Skipping Datacite update for non-DOI handle: {}", handle);
            return;
        }

        final var endpoint = "%s/%s".formatted(properties.getEndpoint(), handle);

        final DataciteRequest dataciteRequest = dataciteRequestFactory.create(request, handle);
        hideNonOpenDoi(dataciteRequest);
        final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(dataciteRequest, repositoryId, password);

        try {
            restTemplate.exchange(endpoint, HttpMethod.PUT, entity, JsonNode.class);
        } catch (HttpClientErrorException e) {
            log.error("Unable to update Datacite record", e);
            throw e;
        }
    }
}