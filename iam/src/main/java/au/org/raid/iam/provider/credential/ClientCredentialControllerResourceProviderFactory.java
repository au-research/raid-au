package au.org.raid.iam.provider.credential;

import au.org.raid.iam.provider.federation.FederationHarvestClientBootstrapper;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ClientCredentialControllerResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "client-credential";

    @Override
    public RealmResourceProvider create(final KeycloakSession session) {
        return new ClientCredentialControllerResourceProvider(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(final Config.Scope config) {

    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        // RAID-982: create the federation harvest client at every boot.
        FederationHarvestClientBootstrapper.register(factory);
    }

    @Override
    public void close() {

    }
}
