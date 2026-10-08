package au.org.raid.api.service.stub;

import au.org.raid.api.model.datacite.repository.DataciteRepository;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryAttributes;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryData;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryProvider;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryProviderData;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryRelationships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataciteRepositoryClientStubTest {
    private static final String PREFIX = "10.5072";

    private static DataciteRepository request() {
        return DataciteRepository.builder()
                .data(DataciteRepositoryData.builder()
                        .type("repositories")
                        .attributes(DataciteRepositoryAttributes.builder()
                                .symbol("ATHH.ABC1234")
                                .name("RAiD AU (branch-raid-943)")
                                .build())
                        .relationships(DataciteRepositoryRelationships.builder()
                                .provider(DataciteRepositoryProvider.builder()
                                        .data(DataciteRepositoryProviderData.builder().type("providers").id("ATHH").build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("createRepository should echo the requested symbol, as DataCite does")
    void createRepositoryEchoesSymbol() {
        final var response = new DataciteRepositoryClientStub(PREFIX).createRepository(request());

        assertThat(response.getData().getAttributes().getSymbol(), is("ATHH.ABC1234"));
        assertThat(response.getData().getAttributes().getName(), is("RAiD AU (branch-raid-943)"));
    }

    @Test
    @DisplayName("createRepository should return the configured prefix where ServicePointService reads it")
    void createRepositoryReturnsConfiguredPrefix() {
        final var response = new DataciteRepositoryClientStub(PREFIX).createRepository(request());

        assertThat(response.getData().getRelationships().getPrefixes().getData().get(0).getId(), is(PREFIX));
    }

    @Test
    @DisplayName("constructor should fail fast when no prefix is configured")
    void constructorRejectsBlankPrefix() {
        assertThrows(IllegalStateException.class, () -> new DataciteRepositoryClientStub(" "));
        assertThrows(IllegalStateException.class, () -> new DataciteRepositoryClientStub(null));
    }
}
