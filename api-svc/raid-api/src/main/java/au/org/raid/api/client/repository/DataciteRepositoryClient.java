package au.org.raid.api.client.repository;

import au.org.raid.api.config.properties.RepositoryClientProperties;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.model.datacite.repository.DataciteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * Wired by {@link au.org.raid.api.config.bean.ExternalPidService}, which substitutes
 * {@link au.org.raid.api.service.stub.DataciteRepositoryClientStub} when raid.stub.datacite.enabled=true.
 */
@RequiredArgsConstructor
public class DataciteRepositoryClient {
    private final RestTemplate restTemplate;
    private final RepositoryClientProperties properties;
    private final HttpEntityFactory httpEntityFactory;

    public DataciteRepository createRepository(final DataciteRepository repository) {
        final var entity = httpEntityFactory.create(repository, properties.getUsername(), properties.getPassword());
        final var response = restTemplate.exchange(properties.getUrl(), HttpMethod.POST, entity, DataciteRepository.class);
        return response.getBody();
    }
}
