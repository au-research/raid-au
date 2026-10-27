package au.org.raid.iam.provider.orcidupdater;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;

import java.net.URI;
import java.util.Map;

/**
 * Creates or updates the ORCID updater's Keycloak client in one realm (RAID-1011).
 *
 * <p>The updater (RAID-975) calls the RAiD API as the client {@code orcid-updater}. It
 * authenticates with {@code private_key_jwt}: it signs an assertion with a key it creates itself and
 * publishes the public half at its own JWKS URL. Keycloak reads that URL, so no client secret is ever
 * generated, handed back or copied by a person. Its service account holds the RAiD API's
 * {@code contributor-writer} realm role, which lets it read RAiDs naming a contributor and set
 * contributor status.
 *
 * <p>Only realms holding the {@code service-point-user} role are RAiD realms; any other realm,
 * including master, is left alone. The {@code contributor-writer} role is created if a RAiD realm
 * lacks it, since the shipped realm does not define it.
 *
 * <p>Safe to run on every boot. An existing client keeps its enabled flag, so an operator can still
 * switch it off, but everything the updater depends on (confidential, service account only, signed
 * JWT from the JWKS URL, full scope, the role) is put back if it has drifted.
 */
public final class OrcidUpdaterClientProvisioner {

    public static final String CLIENT_ID = "orcid-updater";
    public static final String CONTRIBUTOR_WRITER_ROLE = "contributor-writer";
    static final String SERVICE_POINT_USER_ROLE = "service-point-user";

    static final String CLIENT_JWT_AUTHENTICATOR = "client-jwt";
    static final String USE_JWKS_URL_ATTRIBUTE = "use.jwks.url";
    static final String JWKS_URL_ATTRIBUTE = "jwks.url";
    static final String SIGNING_ALG_ATTRIBUTE = "token.endpoint.auth.signing.alg";
    static final String SIGNING_ALG = "RS256";

    public enum Outcome {
        NOT_A_RAID_REALM,
        CREATED,
        UPDATED,
        UNCHANGED
    }

    private OrcidUpdaterClientProvisioner() {
    }

    /**
     * Checks the JWKS URL is an absolute http or https URL. Returns null when it is, otherwise the
     * reason it was refused.
     */
    public static String invalidJwksUrlReason(final String jwksUrl) {
        try {
            final var uri = new URI(jwksUrl);
            final var scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
                return "it must be an http or https URL";
            }
            if (uri.getHost() == null) {
                return "it has no host";
            }
            return null;
        } catch (Exception e) {
            return "it is not a valid URL";
        }
    }

    public static Outcome provision(final KeycloakSession session, final RealmModel realm, final String jwksUrl) {
        if (realm.getRole(SERVICE_POINT_USER_ROLE) == null) {
            return Outcome.NOT_A_RAID_REALM;
        }

        var changed = false;

        var role = realm.getRole(CONTRIBUTOR_WRITER_ROLE);
        if (role == null) {
            // Single-argument addRole: the two-argument form is (id, name), not (name, description).
            role = realm.addRole(CONTRIBUTOR_WRITER_ROLE);
            role.setDescription("Reads RAiDs naming a contributor and sets contributor status (ORCID updater)");
            changed = true;
        }

        var client = realm.getClientByClientId(CLIENT_ID);
        final var created = client == null;
        if (created) {
            client = session.clients().addClient(realm, CLIENT_ID);
            client.setName("ORCID updater");
            client.setEnabled(true);
            // addClient() is a raw model-layer create and skips the realm's default client scopes,
            // so without these the token has no roles scope and realm_access.roles is empty.
            // Materialise before mutating.
            final var target = client;
            realm.getDefaultClientScopesStream(true).toList()
                    .forEach(scope -> target.addClientScope(scope, true));
        }

        changed |= configure(client, jwksUrl);
        changed |= ensureServiceAccountRole(session, client, role);

        if (created) {
            return Outcome.CREATED;
        }
        return changed ? Outcome.UPDATED : Outcome.UNCHANGED;
    }

    private static boolean configure(final ClientModel client, final String jwksUrl) {
        var changed = false;
        if (client.isPublicClient()) {
            client.setPublicClient(false);
            changed = true;
        }
        if (client.isBearerOnly()) {
            client.setBearerOnly(false);
            changed = true;
        }
        if (client.isStandardFlowEnabled()) {
            client.setStandardFlowEnabled(false);
            changed = true;
        }
        if (client.isImplicitFlowEnabled()) {
            client.setImplicitFlowEnabled(false);
            changed = true;
        }
        if (client.isDirectAccessGrantsEnabled()) {
            client.setDirectAccessGrantsEnabled(false);
            changed = true;
        }
        // addClient() leaves this false. While it is false Keycloak filters the token down to the
        // client's explicit scope mappings, which strips contributor-writer.
        if (!client.isFullScopeAllowed()) {
            client.setFullScopeAllowed(true);
            changed = true;
        }
        if (!CLIENT_JWT_AUTHENTICATOR.equals(client.getClientAuthenticatorType())) {
            client.setClientAuthenticatorType(CLIENT_JWT_AUTHENTICATOR);
            changed = true;
        }
        for (final var attribute : Map.of(
                USE_JWKS_URL_ATTRIBUTE, "true",
                JWKS_URL_ATTRIBUTE, jwksUrl,
                SIGNING_ALG_ATTRIBUTE, SIGNING_ALG).entrySet()) {
            if (!attribute.getValue().equals(client.getAttribute(attribute.getKey()))) {
                client.setAttribute(attribute.getKey(), attribute.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean ensureServiceAccountRole(final KeycloakSession session, final ClientModel client,
                                                    final RoleModel role) {
        var changed = false;
        var serviceAccount = client.isServiceAccountsEnabled() ? session.users().getServiceAccount(client) : null;
        if (serviceAccount == null) {
            // Must use the RealmManager constructor: the no-arg ClientManager NPEs here.
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            serviceAccount = session.users().getServiceAccount(client);
            if (serviceAccount == null) {
                throw new IllegalStateException("Service account was not created for client " + CLIENT_ID);
            }
            changed = true;
        }
        if (!serviceAccount.hasDirectRole(role)) {
            serviceAccount.grantRole(role);
            changed = true;
        }
        return changed;
    }
}
