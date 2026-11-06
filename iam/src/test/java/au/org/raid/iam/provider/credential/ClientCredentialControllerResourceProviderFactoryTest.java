package au.org.raid.iam.provider.credential;

import au.org.raid.iam.provider.federation.FederationHarvestClientBootstrapper;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class ClientCredentialControllerResourceProviderFactoryTest {

    private final ClientCredentialControllerResourceProviderFactory factory =
            new ClientCredentialControllerResourceProviderFactory();

    @Test
    void getId_returnsClientCredential() {
        // The factory id becomes the URL segment: /realms/raid/client-credential
        assertThat(factory.getId(), is("client-credential"));
    }

    @Test
    void postInit_registersTheFederationHarvestBootHook() {
        // RAID-982: without this registration the harvest client is never created at boot.
        // Asserts on this hook specifically, so other boot hooks registered from the same postInit
        // (RAID-1011's ORCID updater client) neither break nor satisfy this test.
        var sessionFactory = mock(KeycloakSessionFactory.class);

        try (var bootstrapper = mockStatic(FederationHarvestClientBootstrapper.class)) {
            factory.postInit(sessionFactory);

            bootstrapper.verify(() -> FederationHarvestClientBootstrapper.register(sessionFactory));
        }
    }

    @Test
    void create_returnsResourceProvider() {
        var session = mock(KeycloakSession.class);
        var provider = factory.create(session);
        assertThat(provider, is(notNullValue()));
    }
}
