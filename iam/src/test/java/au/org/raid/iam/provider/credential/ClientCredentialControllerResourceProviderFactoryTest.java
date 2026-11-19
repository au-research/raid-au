package au.org.raid.iam.provider.credential;

import au.org.raid.iam.provider.federation.FederationHarvestClientBootstrapper;
import au.org.raid.iam.provider.orcidupdater.OrcidUpdaterClientBootstrapper;
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
        // Without this registration the harvest client is never created at boot.
        // Asserts on this hook specifically, so other boot hooks registered from the same postInit
        // (such as the ORCID updater client's) neither break nor satisfy this test.
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

    @Test
    void postInit_registersTheOrcidUpdaterBootHook() {
        // Without this registration an RA would have to create the ORCID updater's client by hand.
        var sessionFactory = mock(KeycloakSessionFactory.class);

        try (var bootstrapper = mockStatic(OrcidUpdaterClientBootstrapper.class)) {
            factory.postInit(sessionFactory);

            bootstrapper.verify(() -> OrcidUpdaterClientBootstrapper.register(sessionFactory));
        }
    }
}
