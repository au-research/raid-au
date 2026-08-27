package au.org.raid.iam.provider.credential;

import jakarta.ws.rs.ext.Provider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

@Provider
public class ClientCredentialControllerResourceProvider implements RealmResourceProvider {
    private final KeycloakSession session;

    public ClientCredentialControllerResourceProvider(final KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new ClientCredentialController(session);
    }

    @Override
    public void close() {

    }
}
