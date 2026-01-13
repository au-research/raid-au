package au.org.raid.api.model.datacite.repository;

import au.org.raid.api.config.properties.RepositoryClientProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataciteRepositoryFactory {
    private final RepositoryClientProperties properties;

    public DataciteRepository create(final String name, final String email, final String password) {

        final var suffix = RandomStringUtils.insecure().nextAlphanumeric(7).toUpperCase();

        return DataciteRepository.builder()
                .data(DataciteRepositoryData.builder()
                        .type("repositories")
                        .attributes(DataciteRepositoryAttributes.builder()
                                .symbol("%s.%s".formatted(properties.getOrganisationId(), suffix))
                                .clientType("repository")
                                .name(name)
                                .systemEmail(email)
                                .passwordInput(password)
                                .build())
                        .relationships(DataciteRepositoryRelationships.builder()
                                .provider(DataciteRepositoryProvider.builder()
                                        .data(DataciteRepositoryProviderData.builder()
                                                .type("providers")
                                                .id(properties.getOrganisationId())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
