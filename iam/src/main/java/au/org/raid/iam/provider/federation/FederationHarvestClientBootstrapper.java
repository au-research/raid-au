package au.org.raid.iam.provider.federation;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

/**
 * Creates the federation harvest client unattended at every Keycloak boot (RAID-982), so an agency
 * never creates it by hand or sends ARDC a secret. The agency's only step is telling ARDC its base URL.
 *
 * <p>Same hook as the RAID-884 service-point-admin backfill: Keycloak's {@link PostMigrationEvent},
 * which fires on first boot and every restart, after realm import and before traffic. See
 * {@code doc/adr/2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md} and, for this
 * client, {@code doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md}.
 *
 * <p>Unlike the optional ORCID updater client, this runs on every instance: the key source defaults
 * to the production Registration Authority JWKS (see {@link FederationHarvestKeySource}). An invalid
 * override is logged and nothing is provisioned, rather than falling back to the production keys.
 *
 * <p>Failures are logged and swallowed per realm: provisioning must never stop Keycloak starting.
 */
@Slf4j
public final class FederationHarvestClientBootstrapper {

    private FederationHarvestClientBootstrapper() {
    }

    /**
     * Registers the boot-time provisioning. Call from a
     * {@code RealmResourceProviderFactory#postInit(KeycloakSessionFactory)} hook.
     */
    public static void register(final KeycloakSessionFactory factory) {
        register(factory, System::getenv,
                (url, embeddedKeysAvailable) -> JwksReachabilityCheck.start(factory, url, embeddedKeysAvailable));
    }

    /** Starts a non-blocking check that the JWKS URL can be fetched. */
    @FunctionalInterface
    interface ReachabilityCheck {
        void start(String jwksUrl, boolean embeddedKeysAvailable);
    }

    static void register(final KeycloakSessionFactory factory, final UnaryOperator<String> environment,
                         final ReachabilityCheck reachabilityCheck) {
        factory.register(event -> {
            if (event instanceof PostMigrationEvent) {
                run(factory, environment, reachabilityCheck);
            }
        });
    }

    private static void run(final KeycloakSessionFactory factory, final UnaryOperator<String> environment,
                            final ReachabilityCheck reachabilityCheck) {
        final var resolution = FederationHarvestKeySource.resolve(environment);
        if (resolution.error() != null) {
            log.error("RAID-982 {}; the federation harvest client is not provisioned and any existing one is " +
                    "left unchanged. Keycloak startup continues.", resolution.error());
            return;
        }
        final var keySource = resolution.keySource();
        if (resolution.insecureHttp()) {
            log.warn("RAID-982 the federation harvest client trusts keys fetched over plain http from {} because " +
                    "{}=true. Anyone who can tamper with that fetch can obtain harvest tokens. Use this only on " +
                    "local and test rigs.", keySource.jwksUrl(), FederationHarvestKeySource.ALLOW_HTTP_JWKS_URL_ENV_VAR);
        }
        if (!keySource.usesUrl() && resolution.bundledKeyCount() == 0) {
            log.warn("RAID-982 {}=false but the key set bundled in raid-iam.jar holds no keys, so the " +
                    "Registration Authority cannot harvest this instance. Use a release that ships the keys, " +
                    "or allow outbound HTTPS and use the JWKS URL.", FederationHarvestKeySource.USE_JWKS_URL_ENV_VAR);
        }

        final var realmIds = new ArrayList<String>();
        try {
            KeycloakModelUtils.runJobInTransaction(factory, session ->
                    session.realms().getRealmsStream().forEach(realm -> realmIds.add(realm.getId())));
        } catch (Exception e) {
            log.error("RAID-982 boot-time federation harvest client provisioning could not list realms; " +
                    "skipping. Keycloak startup continues.", e);
            return;
        }

        var provisionedAny = false;
        for (final var realmId : realmIds) {
            try {
                final var holder = new boolean[1];
                KeycloakModelUtils.runJobInTransaction(factory, session ->
                        holder[0] = provision(session, realmId, keySource));
                provisionedAny |= holder[0];
            } catch (Exception e) {
                log.error("RAID-982 boot-time federation harvest client provisioning failed for realm id {}; " +
                        "that realm is unchanged and Keycloak startup continues.", realmId, e);
            }
        }

        if (provisionedAny && keySource.usesUrl()) {
            try {
                reachabilityCheck.start(keySource.jwksUrl(), resolution.bundledKeyCount() > 0);
            } catch (Exception e) {
                log.warn("RAID-982 the federation harvest JWKS reachability check could not start", e);
            }
        }
    }

    /** Returns true when the realm holds a harvest client after this run. */
    private static boolean provision(final KeycloakSession session, final String realmId,
                                     final FederationHarvestKeySource keySource) {
        final var realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return false;
        }
        session.getContext().setRealm(realm);

        final var outcome = FederationHarvestClientProvisioner.provision(session, realm, keySource);
        final var keys = keySource.usesUrl() ? "JWKS URL " + keySource.jwksUrl() : "the embedded key set";
        switch (outcome) {
            case NOT_A_RAID_REALM -> log.debug("RAID-982 realm '{}' has no {} role; no federation harvest client",
                    realm.getName(), FederationHarvestClientProvisioner.RAID_DUMPER_ROLE);
            case UNCHANGED -> log.debug("RAID-982 realm '{}': client '{}' already up to date ({})",
                    realm.getName(), FederationHarvestClientProvisioner.CLIENT_ID, keys);
            default -> log.info("RAID-982 realm '{}': client '{}' {}, signed-JWT authentication from {}",
                    realm.getName(), FederationHarvestClientProvisioner.CLIENT_ID,
                    outcome == FederationHarvestClientProvisioner.Outcome.CREATED ? "created" : "updated", keys);
        }
        return outcome != FederationHarvestClientProvisioner.Outcome.NOT_A_RAID_REALM;
    }
}
