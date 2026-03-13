package au.org.raid.iam.provider.raid;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

class RaidPermissionsControllerResourceProviderFactoryTest {

    private final RaidPermissionsControllerResourceProviderFactory factory =
            new RaidPermissionsControllerResourceProviderFactory();

    @Test
    void getId_returnsRaid() {
        assertThat(factory.getId(), is("raid"));
    }

    @Test
    void create_returnsResourceProvider() {
        var session = mock(KeycloakSession.class);
        var provider = factory.create(session);
        assertThat(provider, is(notNullValue()));
    }
}
