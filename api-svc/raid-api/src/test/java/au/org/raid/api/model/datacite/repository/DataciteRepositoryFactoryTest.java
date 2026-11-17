package au.org.raid.api.model.datacite.repository;

import au.org.raid.api.config.properties.RepositoryClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteRepositoryFactoryTest {
    @Mock
    private RepositoryClientProperties properties;
    @InjectMocks
    private DataciteRepositoryFactory factory;

    @Test
    @DisplayName("create() sets data type to 'repositories'")
    void create_SetsDataType() {
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create("name", "password");

        assertThat(result.getData().getType(), is("repositories"));
    }

    @Test
    @DisplayName("create() sets name from input parameter")
    void create_SetsName() {
        final var name = "Test Repository Name";
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create(name, "password");

        assertThat(result.getData().getAttributes().getName(), is(name));
    }

    @Test
    @DisplayName("create() sets systemEmail from input parameter")
    void create_SetsSystemEmail() {
        final var email = "admin@example.com";
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);
        when(properties.getEmail()).thenReturn(email);

        final var result = factory.create("name", "password");

        assertThat(result.getData().getAttributes().getSystemEmail(), is(email));
    }

    @Test
    @DisplayName("create() sets passwordInput from input parameter")
    void create_SetsPasswordInput() {
        final var password = "securePassword123";
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create("name", password);

        assertThat(result.getData().getAttributes().getPasswordInput(), is(password));
    }

    @Test
    @DisplayName("create() sets clientType to 'raidRegistry'")
    void create_SetsClientType() {
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create("name", "password");

        assertThat(result.getData().getAttributes().getClientType(), is("raidRegistry"));
    }

    @Test
    @DisplayName("create() request body sent to DataCite has clientType 'raidRegistry' (RAID-1013)")
    void create_SerialisedRequestHasRaidRegistryClientType() throws Exception {
        when(properties.getUsername()).thenReturn("TEST.ORG");

        final var result = factory.create("name", "password");

        final var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(result));
        assertThat(json.at("/data/attributes/clientType").asText(), is("raidRegistry"));
    }

    @Test
    @DisplayName("create() sets symbol with organisation ID prefix and 7-character suffix")
    void create_SetsSymbolWithCorrectFormat() {
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create("name", "password");

        final var symbol = result.getData().getAttributes().getSymbol();
        assertThat(symbol, startsWith(username + "."));
        // Symbol should be username + "." + 7 alphanumeric uppercase characters
        assertThat(symbol, matchesPattern("TEST\\.ORG\\.[A-Z0-9]{7}"));
    }

    @Test
    @DisplayName("create() generates unique symbols on each invocation")
    void create_GeneratesUniqueSymbols() {
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result1 = factory.create("name", "password");
        final var result2 = factory.create("name", "password");

        final var symbol1 = result1.getData().getAttributes().getSymbol();
        final var symbol2 = result2.getData().getAttributes().getSymbol();

        // Symbols should be different (extremely unlikely to collide with 7 alphanumeric chars)
        assertThat(symbol1, is(not(equalTo(symbol2))));
    }

    @Test
    @DisplayName("create() sets provider data type to 'providers'")
    void create_SetsProviderDataType() {
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create("name", "password");

        assertThat(result.getData().getRelationships().getProvider().getData().getType(), is("providers"));
    }

    @Test
    @DisplayName("create() sets provider data id to organisation ID")
    void create_SetsProviderDataId() {
        final var username = "TEST.ORG";
        when(properties.getUsername()).thenReturn(username);

        final var result = factory.create("name", "password");

        assertThat(result.getData().getRelationships().getProvider().getData().getId(), is(username));
    }
}