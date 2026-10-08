package au.org.raid.api.service.stub;

import au.org.raid.api.client.repository.DataciteRepositoryClient;
import au.org.raid.api.model.datacite.repository.DataciteRepository;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryData;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryPrefixes;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryPrefixesData;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryRelationships;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Echoes the requested repository back with a fixed prefix, without creating anything in
 * DataCite. Temporary measure for RAID-892: every real repository creation permanently
 * reserves a prefix from DataCite's shared pool. Replaced by the RAID-812 mock server.
 */
@Slf4j
public class DataciteRepositoryClientStub extends DataciteRepositoryClient {
    private final String prefix;

    public DataciteRepositoryClientStub(final String prefix) {
        super(null, null, null);
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalStateException("raid.stub.datacite.prefix must be set when the DataCite stub is enabled");
        }
        this.prefix = prefix;
    }

    @Override
    public DataciteRepository createRepository(final DataciteRepository repository) {
        final var data = repository.getData();
        final var relationships = data.getRelationships();

        log.info("in-memory DataCite stub: skipped repository creation for {}", data.getAttributes().getSymbol());

        return DataciteRepository.builder()
                .data(DataciteRepositoryData.builder()
                        .type(data.getType())
                        .attributes(data.getAttributes())
                        .relationships(DataciteRepositoryRelationships.builder()
                                .provider(relationships != null ? relationships.getProvider() : null)
                                .prefixes(DataciteRepositoryPrefixes.builder()
                                        .data(List.of(DataciteRepositoryPrefixesData.builder()
                                                .type("prefixes")
                                                .id(prefix)
                                                .build()))
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
