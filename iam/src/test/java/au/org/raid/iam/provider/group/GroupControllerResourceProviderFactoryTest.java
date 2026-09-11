package au.org.raid.iam.provider.group;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    /**
     * RAID-884: postInit must register the boot-time backfill listener. Without this the scoped
     * service-point-admin roles are only ever created by a manual operator call, which is the bug.
     */
    @Test
    void postInit_registersBootstrapListener() {
        var sessionFactory = mock(KeycloakSessionFactory.class);

        factory.postInit(sessionFactory);

        verify(sessionFactory).register(notNull());
    }
}
