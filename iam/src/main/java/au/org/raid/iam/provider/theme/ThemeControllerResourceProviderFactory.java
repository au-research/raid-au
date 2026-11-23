// ThemeControllerResourceProviderFactory.java
package au.org.raid.iam.provider.theme;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ThemeControllerResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "theme";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new ThemeControllerResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}
