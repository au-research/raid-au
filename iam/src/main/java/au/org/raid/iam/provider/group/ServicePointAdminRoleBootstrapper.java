package au.org.raid.iam.provider.group;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;

import java.util.ArrayList;

/**
 * Runs the scoped service-point-admin backfill unattended at every Keycloak boot (RAID-884).
 *
 * <p>Before this existed, the only way to provision the scoped
 * "service-point-admin:&lt;groupId&gt;" roles in a deployed environment was for an operator to call
 * POST /realms/raid/group/migrate-service-point-admins by hand. That violated the hard
 * no-manual-deployment-steps NFR, and in ARDC's own production the step was never performed, so
 * the RAID-827 self-serve credential feature was inert there. Registration Agencies deploy the
 * RAiD Service onto platforms ARDC does not control, so any human step in the sequence will be
 * skipped somewhere.
 *
 * <p>This listens for Keycloak's {@link PostMigrationEvent}, which is the platform's standard
 * "run once, unattended, after the realm and database are ready" hook. Verified empirically on
 * Keycloak 26.6.2 (Quarkus): it fires on first boot <em>and</em> on every subsequent restart when
 * no schema migration is pending, after realm import completes and before the server accepts
 * traffic.
 *
 * <p>Every realm is visited, not just the RaiD one. Realms without the legacy flat group-admin
 * role - master, and any realm an agency runs alongside RAiD - are a no-op inside
 * {@link ServicePointAdminRoleProvisioner#backfill}, so no realm name needs configuring. This
 * keeps the mechanism deployment-agnostic, which is the whole point of putting it in the SPI jar.
 *
 * <p>Failures are logged and swallowed per realm. Boot-time provisioning must never stop Keycloak
 * from starting: a realm that fails to backfill leaves that realm exactly as it was before, and the
 * operator endpoint remains available as a recovery lever.
 */
@Slf4j
public final class ServicePointAdminRoleBootstrapper {

    private ServicePointAdminRoleBootstrapper() {
    }

    /**
     * Registers the boot-time backfill. Call from a
     * {@code RealmResourceProviderFactory#postInit(KeycloakSessionFactory)} hook.
     */
    public static void register(final KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof PostMigrationEvent) {
                runForAllRealms(factory);
            }
        });
    }

    private static void runForAllRealms(final KeycloakSessionFactory factory) {
        // Collect realm ids in their own transaction, then back each realm's backfill with a
        // separate transaction, so one failing realm cannot roll back another's grants.
        final var realmIds = new ArrayList<String>();
        try {
            KeycloakModelUtils.runJobInTransaction(factory, session ->
                    session.realms().getRealmsStream().forEach(realm -> realmIds.add(realm.getId())));
        } catch (Exception e) {
            log.error("RAID-884 boot-time service-point-admin backfill could not list realms; " +
                    "skipping. Keycloak startup continues.", e);
            return;
        }

        for (final var realmId : realmIds) {
            runForRealm(factory, realmId);
        }
    }

    private static void runForRealm(final KeycloakSessionFactory factory, final String realmId) {
        try {
            KeycloakModelUtils.runJobInTransaction(factory, session -> backfill(session, realmId));
        } catch (Exception e) {
            log.error("RAID-884 boot-time service-point-admin backfill failed for realm id {}; " +
                    "that realm is unchanged and Keycloak startup continues. The operator endpoint " +
                    "POST /realms/<realm>/group/migrate-service-point-admins can be used to recover.",
                    realmId, e);
        }
    }

    private static void backfill(final KeycloakSession session, final String realmId) {
        final var realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return;
        }

        // The provisioner reads the realm it is handed rather than the session context, but
        // UserProvider lookups still expect a realm on the context.
        session.getContext().setRealm(realm);

        final var result = ServicePointAdminRoleProvisioner.backfill(session, realm);

        if (result.rolesCreated() == 0 && result.grantsAdded() == 0) {
            log.debug("RAID-884 boot-time service-point-admin backfill: realm '{}' already up to date ({})",
                    realm.getName(), result.message());
            return;
        }

        log.info("RAID-884 boot-time service-point-admin backfill: realm '{}' - {} flat group-admin " +
                        "user(s), {} scoped role(s) created, {} grant(s) added, {} already present",
                realm.getName(), result.flatGroupAdminUsers(), result.rolesCreated(),
                result.grantsAdded(), result.grantsSkipped());
    }
}
