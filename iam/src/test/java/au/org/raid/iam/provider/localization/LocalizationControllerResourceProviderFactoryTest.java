package au.org.raid.iam.provider.localization;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

class LocalizationControllerResourceProviderFactoryTest {
    private final LocalizationControllerResourceProviderFactory factory =
            new LocalizationControllerResourceProviderFactory();

    @Test
    void getId_returnsLocalization() {
        assertThat(factory.getId(), is("localization"));
    }

    @Test
    void create_returnsResourceProvider() {
        var session = mock(KeycloakSession.class);
        var provider = factory.create(session);
        assertThat(provider, is(notNullValue()));
    }
}
