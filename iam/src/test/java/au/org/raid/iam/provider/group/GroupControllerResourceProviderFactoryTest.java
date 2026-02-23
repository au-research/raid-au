package au.org.raid.iam.provider.group;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

class GroupControllerResourceProviderFactoryTest {

    private final GroupControllerResourceProviderFactory factory = new GroupControllerResourceProviderFactory();

    @Test
    void getId_returnsGroup() {
        assertThat(factory.getId(), is("group"));
    }

    @Test
    void create_returnsResourceProvider() {
        var session = mock(KeycloakSession.class);
        var provider = factory.create(session);
        assertThat(provider, is(notNullValue()));
    }
}
