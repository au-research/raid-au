package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.dto.RaidListenerMessage;
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

    public void post(final RaidListenerMessage message) {
        final var httpEntity = httpEntityFactory.create(message, properties.getApiKey());

        final var response = restTemplate.exchange(properties.getRaidListener().getUri(), HttpMethod.POST, httpEntity, Void.class);
        log.debug("Response from Raid Listener: {} {}", response.getStatusCode(), response.getBody());
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
