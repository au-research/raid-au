package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.factory.HttpEntityFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrcidIntegrationClient {
    private final RestTemplate restTemplate;
    private final OrcidIntegrationProperties properties;
    private final HttpEntityFactory httpEntityFactory;

    /**
     * Tells the registration agency's ORCID updater that the RAiD was minted or saved. The updater
     * queues the change and reads the RAiD back from this API.
     */
    public void notifyRaidChanged(final String handle) {
        final var httpEntity = httpEntityFactory.create(Map.of("handle", handle), properties.getApiKey());

        final var response = restTemplate.exchange(properties.getRaidChanges().getUri(), HttpMethod.POST, httpEntity, Void.class);
        log.debug("Response from ORCID updater for RAiD {}: {}", handle, response.getStatusCode());
    }

    public Optional<ContributorLookupResponse> findByOrcid(final String orcid) {
        final var httpEntity = httpEntityFactory.create(Map.of("orcid", orcid), properties.getApiKey());

        try {
            final var response = restTemplate.exchange(properties.getContributorIdLookup().getUri(), HttpMethod.POST, httpEntity, ContributorLookupResponse.class);
            log.debug("Response from Raid Listener: {} {}", response.getStatusCode(), response.getBody());
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatusCode.valueOf(404)) {
                throw e;
            }
        }

        return Optional.empty();
    }
}
