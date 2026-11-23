// ThemeControllerResourceProvider.java
package au.org.raid.iam.provider.theme;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class ThemeControllerResourceProvider implements RealmResourceProvider {
    private final KeycloakSession session;

    public ThemeControllerResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new ThemeController(session);
    }

    @Override
    public void close() {
    }
}
