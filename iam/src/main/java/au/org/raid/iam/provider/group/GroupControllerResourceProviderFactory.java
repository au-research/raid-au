package au.org.raid.iam.provider.group;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class GroupControllerResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "group";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new GroupControllerResourceProvider(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(final Config.Scope config) {

    }

    /**
     * Provisions the scoped service-point-admin roles at boot (RAID-884), replacing the manual
     * operator call to /group/migrate-service-point-admins that the deployment procedure used to
     * depend on. See {@link ServicePointAdminRoleBootstrapper}.
     */
    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        ServicePointAdminRoleBootstrapper.register(factory);
    }

    @Override
    public void close() {

    }
}
