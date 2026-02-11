package au.org.raid.iam.provider.group;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerResourceProviderFactoryTest {

    @Mock private KeycloakSession session;

    @Test
    void getId_returnsGroup() {
        var factory = new GroupControllerResourceProviderFactory();
        assertEquals("group", factory.getId());
    }

    @Test
    void create_returnsGroupControllerResourceProvider() {
        var factory = new GroupControllerResourceProviderFactory();
        var provider = factory.create(session);
        assertInstanceOf(GroupControllerResourceProvider.class, provider);
    }
}
