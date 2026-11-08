package au.org.raid.iam.provider.federation;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates or reconciles the federation harvest client in one realm.
 *
 * <p>The Registration Authority's Federation API harvests each agency's public RAiDs as the client
 * {@value #CLIENT_ID}. It authenticates with {@code private_key_jwt}: it signs an assertion with a
 * key held in AWS KMS, and this realm verifies it against the Registration Authority's public keys.
 * No client secret exists, so nothing secret passes between the agency and ARDC.
 *
 * <p>The client's service account holds only {@value #RAID_DUMPER_ROLE}, plus the realm's default
 * role composite that every service account carries. On every boot any other direct role is removed
 * and the account leaves every group, so the credential stays public-only even if someone grants it
 * more. What the default composite itself contains is the agency's to keep free of API roles.
 *
 * <p>Only realms holding the {@value #RAID_DUMPER_ROLE} role are RAiD realms for this purpose; any
 * other realm, including master, is left alone. The role is never created here.
 *
 * <p>Safe to run on every boot. Everything the harvester depends on is put back if it has drifted,
 * but an existing client keeps its enabled flag: disabling the client is how an agency cuts off
 * harvesting, so a restart must not undo it.
 */
public final class FederationHarvestClientProvisioner {

    public static final String CLIENT_ID = "raid-federation-harvester";
    public static final String RAID_DUMPER_ROLE = "raid-dumper";
    public static final String ACCESS_TOKEN_LIFESPAN_SECONDS = "300";

    static final String CLIENT_JWT_AUTHENTICATOR = "client-jwt";
    static final String USE_JWKS_URL_ATTRIBUTE = "use.jwks.url";
    static final String JWKS_URL_ATTRIBUTE = "jwks.url";
    static final String USE_JWKS_STRING_ATTRIBUTE = "use.jwks.string";
    static final String JWKS_STRING_ATTRIBUTE = "jwks.string";
    static final String SIGNING_ALG_ATTRIBUTE = "token.endpoint.auth.signing.alg";
    static final String SIGNING_ALG = "RS256";
    static final String ACCESS_TOKEN_LIFESPAN_ATTRIBUTE = "access.token.lifespan";
    /** jwt.credential.certificate, jwt.credential.public.key and jwt.credential.kid. */
    static final String JWT_CREDENTIAL_ATTRIBUTE_PREFIX = "jwt.credential.";
    /** ClientSecretConstants.CLIENT_ROTATED_SECRET and its creation and expiry times. */
    static final String ROTATED_SECRET_ATTRIBUTE_PREFIX = "client.secret.rotated";

    public enum Outcome {
        NOT_A_RAID_REALM,
        CREATED,
        UPDATED,
        UNCHANGED
    }

    private FederationHarvestClientProvisioner() {
    }

    public static Outcome provision(final KeycloakSession session, final RealmModel realm,
                                    final FederationHarvestKeySource keySource) {
        final var raidDumper = realm.getRole(RAID_DUMPER_ROLE);
        if (raidDumper == null) {
            return Outcome.NOT_A_RAID_REALM;
        }

        var client = realm.getClientByClientId(CLIENT_ID);
        final var created = client == null;
        if (created) {
            client = session.clients().addClient(realm, CLIENT_ID);
            client.setName("RAiD federation harvester");
            client.setDescription("Lets the RAiD Registration Authority harvest this agency's public RAiDs " +
                    "(created at boot by raid-iam.jar). Disable it to stop harvesting.");
            client.setEnabled(true);
            // addClient() is a raw model-layer create and skips the realm's default client scopes,
            // so without these the token has no roles scope and realm_access.roles is empty.
            // Materialise before mutating.
            final var target = client;
            realm.getDefaultClientScopesStream(true).toList()
                    .forEach(scope -> target.addClientScope(scope, true));
        }

        var changed = configure(client, keySource);
        if (changed) {
            // Model-layer setters publish no ClientUpdatedEvent. updateClient() does, which makes
            // Keycloak drop any public keys it cached for this client under the old key source.
            client.updateClient();
        }
        changed |= ensureServiceAccountHoldsOnlyRaidDumper(session, realm, client, raidDumper);

        if (created) {
            return Outcome.CREATED;
        }
        return changed ? Outcome.UPDATED : Outcome.UNCHANGED;
    }

    private static boolean configure(final ClientModel client, final FederationHarvestKeySource keySource) {
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
        // client's explicit scope mappings, which strips raid-dumper.
        if (!client.isFullScopeAllowed()) {
            client.setFullScopeAllowed(true);
            changed = true;
        }
        if (!CLIENT_JWT_AUTHENTICATOR.equals(client.getClientAuthenticatorType())) {
            client.setClientAuthenticatorType(CLIENT_JWT_AUTHENTICATOR);
            changed = true;
        }
        // The client-jwt authenticator never accepts a secret, but a stored one would become live if
        // anyone switched the authenticator back. The model-layer create makes none; this clears any
        // that was added later.
        if (client.getSecret() != null) {
            client.setSecret(null);
            changed = true;
        }

        final var wanted = new LinkedHashMap<String, String>();
        wanted.put(SIGNING_ALG_ATTRIBUTE, SIGNING_ALG);
        wanted.put(ACCESS_TOKEN_LIFESPAN_ATTRIBUTE, ACCESS_TOKEN_LIFESPAN_SECONDS);
        final String unusedKeyAttribute;
        if (keySource.usesUrl()) {
            wanted.put(USE_JWKS_URL_ATTRIBUTE, "true");
            wanted.put(JWKS_URL_ATTRIBUTE, keySource.jwksUrl());
            wanted.put(USE_JWKS_STRING_ATTRIBUTE, "false");
            unusedKeyAttribute = JWKS_STRING_ATTRIBUTE;
        } else {
            wanted.put(USE_JWKS_URL_ATTRIBUTE, "false");
            wanted.put(USE_JWKS_STRING_ATTRIBUTE, "true");
            wanted.put(JWKS_STRING_ATTRIBUTE, keySource.jwksString());
            unusedKeyAttribute = JWKS_URL_ATTRIBUTE;
        }
        changed |= setAttributes(client, wanted);
        if (client.getAttribute(unusedKeyAttribute) != null) {
            client.removeAttribute(unusedKeyAttribute);
            changed = true;
        }
        changed |= removeDormantCredentials(client);
        return changed;
    }

    /**
     * Removes credentials the client-jwt authenticator ignores today but that would become live if
     * someone changed the key source or authenticator: an uploaded certificate or public key, and
     * rotated client secrets.
     */
    private static boolean removeDormantCredentials(final ClientModel client) {
        final var attributes = client.getAttributes();
        if (attributes == null) {
            return false;
        }
        var changed = false;
        for (final var name : List.copyOf(attributes.keySet())) {
            if (name.startsWith(JWT_CREDENTIAL_ATTRIBUTE_PREFIX) || name.startsWith(ROTATED_SECRET_ATTRIBUTE_PREFIX)) {
                client.removeAttribute(name);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean setAttributes(final ClientModel client, final Map<String, String> wanted) {
        var changed = false;
        for (final var attribute : wanted.entrySet()) {
            if (!attribute.getValue().equals(client.getAttribute(attribute.getKey()))) {
                client.setAttribute(attribute.getKey(), attribute.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean ensureServiceAccountHoldsOnlyRaidDumper(final KeycloakSession session,
                                                                   final RealmModel realm,
                                                                   final ClientModel client,
                                                                   final RoleModel raidDumper) {
        var changed = false;
        // A freshly added client has service accounts disabled, so only look one up once enabled.
        UserModel serviceAccount = client.isServiceAccountsEnabled() ? session.users().getServiceAccount(client) : null;
        if (serviceAccount == null) {
            // Must use the RealmManager constructor: the no-arg ClientManager NPEs here.
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            serviceAccount = session.users().getServiceAccount(client);
            if (serviceAccount == null) {
                throw new IllegalStateException("Service account was not created for client " + CLIENT_ID);
            }
            changed = true;
        }

        // Group memberships carry roles too, and a new user joins the realm's default groups.
        // The harvester belongs to no group. Materialise before mutating.
        for (final var group : serviceAccount.getGroupsStream().toList()) {
            serviceAccount.leaveGroup(group);
            changed = true;
        }

        // Public-only: strip any direct role beyond raid-dumper and the realm's default composite,
        // whether realm or client role. Materialise before mutating.
        final var defaultRole = realm.getDefaultRole();
        for (final var role : serviceAccount.getRoleMappingsStream().toList()) {
            final var isRaidDumper = role.getId().equals(raidDumper.getId());
            final var isDefaultRole = defaultRole != null && role.getId().equals(defaultRole.getId());
            if (!isRaidDumper && !isDefaultRole) {
                serviceAccount.deleteRoleMapping(role);
                changed = true;
            }
        }

        if (!serviceAccount.hasDirectRole(raidDumper)) {
            serviceAccount.grantRole(raidDumper);
            changed = true;
        }
        return changed;
    }
}
