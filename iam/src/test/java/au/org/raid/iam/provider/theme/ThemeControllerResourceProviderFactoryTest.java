package au.org.raid.iam.provider.theme;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

class ThemeControllerResourceProviderFactoryTest {

    private final ThemeControllerResourceProviderFactory factory =
            new ThemeControllerResourceProviderFactory();

    @Test
    void getId_returnsTheme() {
        // The factory id becomes the URL segment: /realms/raid/theme
        assertThat(factory.getId(), is("theme"));
    }

    @Test
    void create_returnsResourceProvider() {
        final var provider = factory.create(mock(KeycloakSession.class));

        assertThat(provider, is(notNullValue()));
        assertThat(provider.getResource(), is(instanceOf(ThemeController.class)));
    }

    @Test
    void isRegisteredAsARealmResourceProviderFactory() throws Exception {
        // Without this line in META-INF/services Keycloak never loads the endpoint.
        final var registration = Files.readAllLines(Path.of(
                "src/main/resources/META-INF/services/"
                        + "org.keycloak.services.resource.RealmResourceProviderFactory"));

        assertThat(registration, hasItem(ThemeControllerResourceProviderFactory.class.getName()));
    }
}
