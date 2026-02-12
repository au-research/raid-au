package au.org.raid.iam.provider.raid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RaidPermissionsControllerResourceProviderFactoryTest {

    @Mock private KeycloakSession session;

    @Test
    void getId_returnsRaid() {
        var factory = new RaidPermissionsControllerResourceProviderFactory();
        assertEquals("raid", factory.getId());
    }

    @Test
    void create_returnsRaidPermissionsControllerResourceProvider() {
        var factory = new RaidPermissionsControllerResourceProviderFactory();
        var provider = factory.create(session);
        assertInstanceOf(RaidPermissionsControllerResourceProvider.class, provider);
    }
}
