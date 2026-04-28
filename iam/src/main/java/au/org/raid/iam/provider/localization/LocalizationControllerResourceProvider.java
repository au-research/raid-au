// LocalizationControllerResourceProvider.java
package au.org.raid.iam.provider.localization;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class LocalizationControllerResourceProvider implements RealmResourceProvider {
    private final KeycloakSession session;

    public LocalizationControllerResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new LocalizationController(session);
    }

    @Override
    public void close() {
    }
}
