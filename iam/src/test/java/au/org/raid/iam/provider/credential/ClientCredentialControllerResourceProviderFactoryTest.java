package au.org.raid.iam.provider.credential;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

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
}
