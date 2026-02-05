package au.org.raid.api.service;

import au.org.raid.api.client.repository.DataciteRepositoryClient;
import au.org.raid.api.model.datacite.repository.DataciteRepository;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryService {
    private static final int MAX_ATTEMPTS = 5;

    private final DataciteRepositoryFactory repositoryFactory;
    private final DataciteRepositoryClient repositoryClient;

    public DataciteRepository create(final String name, final String password) {

        for (int attempts = 0; attempts < MAX_ATTEMPTS; attempts++) {
            final var repository = repositoryFactory.create(name, password);
            try {
                return repositoryClient.createRepository(repository);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().equals(HttpStatusCode.valueOf(422))) {
                    log.info("Repository create request returned 422, retrying", e);
                }
                else {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Too many attempts. Unable to create Datacite repository");
    }
}
