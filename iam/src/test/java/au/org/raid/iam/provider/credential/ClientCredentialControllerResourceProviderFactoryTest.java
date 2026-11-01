package au.org.raid.iam.provider.credential;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClientCredentialControllerResourceProviderFactoryTest {

    private final ClientCredentialControllerResourceProviderFactory factory =
            new ClientCredentialControllerResourceProviderFactory();

    @Test
    void getId_returnsClientCredential() {
        // The factory id becomes the URL segment: /realms/raid/client-credential
        assertThat(factory.getId(), is("client-credential"));
    }

    @Test
    void create_returnsResourceProvider() {
        var session = mock(KeycloakSession.class);
        var provider = factory.create(session);
        assertThat(provider, is(notNullValue()));
    }

    /**
     * RAID-1011: postInit must register the boot-time ORCID updater client provisioning, or an RA
     * would have to create the client by hand.
     */
    @Test
    void postInit_registersOrcidUpdaterClientBootstrapper() {
        var sessionFactory = mock(KeycloakSessionFactory.class);

        factory.postInit(sessionFactory);

        verify(sessionFactory).register(notNull());
    }
}
