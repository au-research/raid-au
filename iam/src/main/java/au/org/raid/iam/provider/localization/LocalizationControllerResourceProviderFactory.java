// LocalizationControllerResourceProviderFactory.java
package au.org.raid.iam.provider.localization;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class LocalizationControllerResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "localization";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new LocalizationControllerResourceProvider(session);
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
