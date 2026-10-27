package au.org.raid.iam.provider.orcidupdater;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

/**
 * Creates the ORCID updater's Keycloak client unattended at every Keycloak boot (RAID-1011), so an
 * RA never creates it by hand or copies a secret.
 *
 * <p>Same hook as the RAID-884 service-point-admin backfill: Keycloak's {@link PostMigrationEvent},
 * which fires on first boot and every restart, after realm import and before traffic. See
 * {@code ServicePointAdminRoleBootstrapper} and the ADR
 * {@code doc/adr/2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md}.
 *
 * <p>The ORCID updater is optional for an RA. The RA turns this on by setting
 * {@value #JWKS_URL_ENV_VAR} to the updater's JWKS URL ({@code <updater URL>/.well-known/jwks.json}).
 * Unset, nothing happens, and an existing client is left as it is. An invalid URL is logged and
 * ignored. Configuration comes from the environment because {@code Config.Scope} does not reach
 * providers in this module.
 *
 * <p>Failures are logged and swallowed per realm: provisioning must never stop Keycloak starting.
 */
@Slf4j
public final class OrcidUpdaterClientBootstrapper {

    public static final String JWKS_URL_ENV_VAR = "RAID_ORCID_UPDATER_JWKS_URL";

    private OrcidUpdaterClientBootstrapper() {
    }

    /**
     * Registers the boot-time provisioning. Call from a
     * {@code RealmResourceProviderFactory#postInit(KeycloakSessionFactory)} hook.
     */
    public static void register(final KeycloakSessionFactory factory) {
        register(factory, System::getenv);
    }

    static void register(final KeycloakSessionFactory factory, final UnaryOperator<String> environment) {
        factory.register(event -> {
            if (event instanceof PostMigrationEvent) {
                run(factory, environment.apply(JWKS_URL_ENV_VAR));
            }
        });
    }

    private static void run(final KeycloakSessionFactory factory, final String configuredJwksUrl) {
        if (configuredJwksUrl == null || configuredJwksUrl.isBlank()) {
            log.debug("RAID-1011 {} is not set; the ORCID updater's client is not provisioned", JWKS_URL_ENV_VAR);
            return;
        }
        final var jwksUrl = configuredJwksUrl.trim();
        final var invalid = OrcidUpdaterClientProvisioner.invalidJwksUrlReason(jwksUrl);
        if (invalid != null) {
            log.error("RAID-1011 {} is ignored because {}; the ORCID updater's client is not provisioned. " +
                    "Keycloak startup continues.", JWKS_URL_ENV_VAR, invalid);
            return;
        }

        final var realmIds = new ArrayList<String>();
        try {
            KeycloakModelUtils.runJobInTransaction(factory, session ->
                    session.realms().getRealmsStream().forEach(realm -> realmIds.add(realm.getId())));
        } catch (Exception e) {
            log.error("RAID-1011 boot-time ORCID updater client provisioning could not list realms; " +
                    "skipping. Keycloak startup continues.", e);
            return;
        }

        for (final var realmId : realmIds) {
            try {
                KeycloakModelUtils.runJobInTransaction(factory, session -> provision(session, realmId, jwksUrl));
            } catch (Exception e) {
                log.error("RAID-1011 boot-time ORCID updater client provisioning failed for realm id {}; " +
                        "that realm is unchanged and Keycloak startup continues.", realmId, e);
            }
        }
    }

    private static void provision(final KeycloakSession session, final String realmId, final String jwksUrl) {
        final var realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return;
        }
        session.getContext().setRealm(realm);

        final var outcome = OrcidUpdaterClientProvisioner.provision(session, realm, jwksUrl);
        switch (outcome) {
            case NOT_A_RAID_REALM -> log.debug("RAID-1011 realm '{}' is not a RAiD realm; no ORCID updater client",
                    realm.getName());
            case UNCHANGED -> log.debug("RAID-1011 realm '{}': client '{}' already up to date",
                    realm.getName(), OrcidUpdaterClientProvisioner.CLIENT_ID);
            default -> log.info("RAID-1011 realm '{}': client '{}' {}, signed-JWT authentication from {}",
                    realm.getName(), OrcidUpdaterClientProvisioner.CLIENT_ID,
                    outcome == OrcidUpdaterClientProvisioner.Outcome.CREATED ? "created" : "updated", jwksUrl);
        }
    }
}
