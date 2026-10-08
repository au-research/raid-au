package au.org.raid.api.config.bean;

import au.org.raid.api.client.contributor.orcid.OrcidClient;
import au.org.raid.api.client.contributor.orcid.OrcidRequestEntityFactory;
import au.org.raid.api.client.repository.DataciteRepositoryClient;
import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.client.ror.RorRequestEntityFactory;
import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.config.properties.RepositoryClientProperties;
import au.org.raid.api.config.properties.StubProperties;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.api.service.stub.DataciteRepositoryClientStub;
import au.org.raid.api.service.stub.DataciteServiceStub;
import au.org.raid.api.service.stub.OrcidClientStub;
import au.org.raid.api.service.stub.RorClientStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression guard for RAID-809: the in-memory stub beans for ORCID and ROR must only be
 * selected when the corresponding {@code raid.stub.orcid/ror.enabled} property is explicitly
 * true. When the property is false or absent, the REAL client must be wired - this is the
 * "binding trap" that previously left prod/stage/demo talking to an in-memory stub instead of
 * the real ORCID service because the property defaulted to true.
 */
@ExtendWith(MockitoExtension.class)
class ExternalPidServiceTest {
    @Mock
    private OrcidRequestEntityFactory orcidRequestEntityFactory;
    @Mock
    private RorRequestEntityFactory rorRequestEntityFactory;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private DataciteProperties dataciteProperties;
    @Mock
    private DataciteRequestFactory dataciteRequestFactory;
    @Mock
    private HttpEntityFactory httpEntityFactory;
    @Mock
    private RepositoryClientProperties repositoryClientProperties;
    @Mock
    private ObjectMapper objectMapper;

    private final ExternalPidService externalPidService = new ExternalPidService();

    @Test
    @DisplayName("orcidClient should return the stub when raid.stub.orcid.enabled=true")
    void orcidClientReturnsStubWhenEnabled() {
        final var stubProperties = new StubProperties();
        final var orcid = new StubProperties.Orcid();
        orcid.setEnabled(true);
        orcid.setDelay(0L);
        stubProperties.setOrcid(orcid);

        final var client = externalPidService.orcidClient(stubProperties, orcidRequestEntityFactory, restTemplate);

        assertThat(client, instanceOf(OrcidClientStub.class));
    }

    @Test
    @DisplayName("orcidClient should return the real client when raid.stub.orcid.enabled=false")
    void orcidClientReturnsRealClientWhenDisabled() {
        final var stubProperties = new StubProperties();
        final var orcid = new StubProperties.Orcid();
        orcid.setEnabled(false);
        stubProperties.setOrcid(orcid);

        final var client = externalPidService.orcidClient(stubProperties, orcidRequestEntityFactory, restTemplate);

        assertThat(client, instanceOf(OrcidClient.class));
        assertThat(client, is(not(instanceOf(OrcidClientStub.class))));
    }

    @Test
    @DisplayName("orcidClient should return the real client when the orcid stub config is absent")
    void orcidClientReturnsRealClientWhenConfigAbsent() {
        final var stubProperties = new StubProperties();

        final var client = externalPidService.orcidClient(stubProperties, orcidRequestEntityFactory, restTemplate);

        assertThat(client, instanceOf(OrcidClient.class));
        assertThat(client, is(not(instanceOf(OrcidClientStub.class))));
    }

    @Test
    @DisplayName("rorClient should return the stub when raid.stub.ror.enabled=true")
    void rorClientReturnsStubWhenEnabled() {
        final var stubProperties = new StubProperties();
        final var ror = new StubProperties.Ror();
        ror.setEnabled(true);
        ror.setDelay(0L);
        stubProperties.setRor(ror);

        final var client = externalPidService.rorClient(stubProperties, rorRequestEntityFactory, restTemplate);

        assertThat(client, instanceOf(RorClientStub.class));
    }

    @Test
    @DisplayName("rorClient should return the real client when raid.stub.ror.enabled=false")
    void rorClientReturnsRealClientWhenDisabled() {
        final var stubProperties = new StubProperties();
        final var ror = new StubProperties.Ror();
        ror.setEnabled(false);
        stubProperties.setRor(ror);

        final var client = externalPidService.rorClient(stubProperties, rorRequestEntityFactory, restTemplate);

        assertThat(client, instanceOf(RorClient.class));
        assertThat(client, is(not(instanceOf(RorClientStub.class))));
    }

    @Test
    @DisplayName("rorClient should return the real client when the ror stub config is absent")
    void rorClientReturnsRealClientWhenConfigAbsent() {
        final var stubProperties = new StubProperties();

        final var client = externalPidService.rorClient(stubProperties, rorRequestEntityFactory, restTemplate);

        assertThat(client, instanceOf(RorClient.class));
        assertThat(client, is(not(instanceOf(RorClientStub.class))));
    }

    private static StubProperties dataciteStub(final boolean enabled) {
        final var stubProperties = new StubProperties();
        final var datacite = new StubProperties.Datacite();
        datacite.setEnabled(enabled);
        datacite.setDelay(0L);
        datacite.setPrefix("10.5072");
        stubProperties.setDatacite(datacite);
        return stubProperties;
    }

    private DataciteService dataciteService(final StubProperties stubProperties, final String environment) {
        return externalPidService.dataciteService(stubProperties, environment, dataciteProperties, restTemplate,
                dataciteRequestFactory, httpEntityFactory, objectMapper);
    }

    private DataciteRepositoryClient dataciteRepositoryClient(final StubProperties stubProperties, final String environment) {
        return externalPidService.dataciteRepositoryClient(stubProperties, environment, restTemplate,
                repositoryClientProperties, httpEntityFactory);
    }

    @Test
    @DisplayName("dataciteService should return the stub when raid.stub.datacite.enabled=true")
    void dataciteServiceReturnsStubWhenEnabled() {
        assertThat(dataciteService(dataciteStub(true), "test"), instanceOf(DataciteServiceStub.class));
    }

    @Test
    @DisplayName("dataciteService should return the real service when raid.stub.datacite.enabled=false")
    void dataciteServiceReturnsRealServiceWhenDisabled() {
        assertThat(dataciteService(dataciteStub(false), "test"), is(not(instanceOf(DataciteServiceStub.class))));
    }

    @Test
    @DisplayName("dataciteService should return the real service when the datacite stub config is absent")
    void dataciteServiceReturnsRealServiceWhenConfigAbsent() {
        assertThat(dataciteService(new StubProperties(), "prod"), is(not(instanceOf(DataciteServiceStub.class))));
    }

    @Test
    @DisplayName("dataciteService should refuse to start with the stub enabled in prod")
    void dataciteServiceRefusesStubInProd() {
        assertThrows(IllegalStateException.class, () -> dataciteService(dataciteStub(true), "prod"));
    }

    @Test
    @DisplayName("dataciteRepositoryClient should return the stub when raid.stub.datacite.enabled=true")
    void dataciteRepositoryClientReturnsStubWhenEnabled() {
        assertThat(dataciteRepositoryClient(dataciteStub(true), "test"), instanceOf(DataciteRepositoryClientStub.class));
    }

    @Test
    @DisplayName("dataciteRepositoryClient should return the real client when raid.stub.datacite.enabled=false")
    void dataciteRepositoryClientReturnsRealClientWhenDisabled() {
        assertThat(dataciteRepositoryClient(dataciteStub(false), "test"),
                is(not(instanceOf(DataciteRepositoryClientStub.class))));
    }

    @Test
    @DisplayName("dataciteRepositoryClient should return the real client when the datacite stub config is absent")
    void dataciteRepositoryClientReturnsRealClientWhenConfigAbsent() {
        assertThat(dataciteRepositoryClient(new StubProperties(), "prod"),
                is(not(instanceOf(DataciteRepositoryClientStub.class))));
    }

    @Test
    @DisplayName("dataciteRepositoryClient should refuse to start with the stub enabled in prod")
    void dataciteRepositoryClientRefusesStubInProd() {
        assertThrows(IllegalStateException.class, () -> dataciteRepositoryClient(dataciteStub(true), "prod"));
    }
}
