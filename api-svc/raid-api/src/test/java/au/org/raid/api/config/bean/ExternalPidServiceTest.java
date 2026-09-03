package au.org.raid.api.config.bean;

import au.org.raid.api.client.contributor.orcid.OrcidClient;
import au.org.raid.api.client.contributor.orcid.OrcidRequestEntityFactory;
import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.client.ror.RorRequestEntityFactory;
import au.org.raid.api.config.properties.StubProperties;
import au.org.raid.api.service.stub.OrcidClientStub;
import au.org.raid.api.service.stub.RorClientStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;

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
}
