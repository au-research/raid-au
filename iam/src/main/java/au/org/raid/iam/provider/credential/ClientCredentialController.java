package au.org.raid.iam.provider.credential;

import au.org.raid.iam.provider.cors.Cors;
import au.org.raid.iam.provider.credential.dto.CreateCredentialRequest;
import au.org.raid.iam.provider.credential.dto.CredentialResponse;
import au.org.raid.iam.provider.credential.dto.CredentialSecretResponse;
import au.org.raid.iam.provider.credential.dto.RotateCredentialRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Self-service lifecycle for a service point's scoped API client credentials (RAID-846).
 *
 * <p>This runs in-process inside Keycloak, so it mutates the realm through the model layer and
 * needs no service account and no {@code manage-clients} grant: Keycloak enforces
 * {@code realm-management} roles in its Admin REST endpoints, which an SPI resource never passes
 * through. See {@code doc/adr/2026-08-27_in-process-spi-credential-lifecycle.md}. Authorisation is
 * therefore entirely this class's own scoped role check, which makes it the tenant-isolation
 * security boundary for the feature.
 *
 * <p>Unlike {@code GroupController}, there is deliberately <em>no</em> flat {@code group-admin}
 * fallback here: RAID-827 requires the scoped {@code service-point-admin:<groupId>} role.
 */
@Slf4j
@Provider
public class ClientCredentialController {
    private static final String OPERATOR_ROLE_NAME = "operator";
    private static final String SERVICE_POINT_ADMIN_ROLE_PREFIX = "service-point-admin";
    private static final String SERVICE_POINT_USER_ROLE_PREFIX = "service-point-user";

    /**
     * Client scope carrying the {@code service_point_group_id} claim mapper, which reads the
     * {@code activeGroupId} user attribute. It is not a realm default scope (only the
     * {@code raid-api} client has it), so it must be attached explicitly to each credential we
     * create or its tokens will not identify a service point at all.
     */
    private static final String SERVICE_POINT_GROUP_ID_CLIENT_SCOPE = "service_point_group_id";
    private static final String ACTIVE_GROUP_ID_ATTRIBUTE = "activeGroupId";

    /** Marks a client as managed by this feature, so listing never returns the realm's own clients. */
    private static final String MANAGED_ATTRIBUTE = "raid.credential.managed";
    private static final String GROUP_ID_ATTRIBUTE = "raid.credential.group-id";
    private static final String LABEL_ATTRIBUTE = "raid.credential.label";
    private static final String CREATED_AT_ATTRIBUTE = "raid.credential.created-at";
    private static final String ROTATED_AT_ATTRIBUTE = "raid.credential.rotated-at";

    private static final String CLIENT_ID_PREFIX = "raid-cred-";

    /** Applied to every response carrying a secret value, so it is never cached anywhere. */
    private static final String NO_STORE = "no-store";

    /**
     * Maximum active (non-revoked) credentials per service point. Deliberately a constant rather
     * than configuration so it is cheap to revisit, per RAID-849: rate limiting was deferred until
     * demonstrated need and this cap ships in its place, addressing realm pollution from runaway
     * credential creation. See {@code doc/adr/2026-08-26_scoped-client-credential-quota.md}.
     */
    static final int MAX_CREDENTIALS_PER_SERVICE_POINT = 10;

    private final AuthenticationManager.AuthResult auth;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeycloakSession session;
    private final Cors cors;
    private final Clock clock;
    private final CredentialAuditLogger auditLogger;

    public ClientCredentialController(final KeycloakSession session) {
        this(session, Clock.systemUTC());
    }

    // Package-private seam for tests, so timestamps are deterministic without touching the system
    // clock.
    ClientCredentialController(final KeycloakSession session, final Clock clock) {
        this(session, clock, new CredentialAuditLogger(clock));
    }

    // Package-private seam for tests, so audit output can be captured directly.
    ClientCredentialController(final KeycloakSession session, final Clock clock,
                              final CredentialAuditLogger auditLogger) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        this.cors = new Cors(session, objectMapper);
        this.clock = clock;
        this.auditLogger = auditLogger;
    }

    @OPTIONS
    @Path("")
    public Response preflight() {
        return cors.buildOptionsResponse("GET", "POST", "DELETE", "OPTIONS");
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response create(final CreateCredentialRequest request) {
        log.debug("Creating client credential for group {}", request == null ? null : request.getGroupId());

        final var user = authenticatedUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (request == null || isBlank(request.getGroupId())) {
            return badRequest("groupId is required");
        }
        if (isBlank(request.getLabel())) {
            return badRequest("label is required");
        }

        final var groupId = request.getGroupId().trim();
        requireAuthorisedFor(user, groupId);

        final var realm = session.getContext().getRealm();
        if (session.groups().getGroupById(realm, groupId) == null) {
            return notFound("Service point not found");
        }

        if (countActiveCredentials(realm, groupId) >= MAX_CREDENTIALS_PER_SERVICE_POINT) {
            return conflict("Service point has reached the maximum of "
                    + MAX_CREDENTIALS_PER_SERVICE_POINT + " active client credentials. "
                    + "Revoke an existing credential before creating another.");
        }

        // Every mutation below shares this request's Keycloak transaction, so a failure part-way
        // through rolls the whole thing back rather than leaving a half-created client.
        final var now = nowIso();
        final var client = session.clients().addClient(realm, CLIENT_ID_PREFIX + KeycloakModelUtils.generateId());
        client.setName(request.getLabel().trim());
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setServiceAccountsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setDirectAccessGrantsEnabled(false);
        // addClient() leaves this false, unlike the Admin API path. While it is false Keycloak
        // filters the token down to roles present in the client's explicit scope mappings, so the
        // scoped service-point-user role granted below is stripped and the credential authenticates
        // with no roles at all. Safe here because this controller is the only thing that grants
        // roles to these service accounts, and it grants exactly one scoped usage role. Matches the
        // existing raid-dumper client.
        client.setFullScopeAllowed(true);
        client.setAttribute(MANAGED_ATTRIBUTE, "true");
        client.setAttribute(GROUP_ID_ATTRIBUTE, groupId);
        client.setAttribute(LABEL_ATTRIBUTE, request.getLabel().trim());
        client.setAttribute(CREATED_AT_ATTRIBUTE, now);

        final var secret = KeycloakModelUtils.generateSecret(client);

        attachClientScopes(realm, client);

        // Must use the RealmManager constructor. ClientManager's no-arg constructor leaves its
        // realmManager field null, and enableServiceAccount dereferences it, so the no-arg form
        // fails at runtime with an NPE even though it compiles.
        new ClientManager(new RealmManager(session)).enableServiceAccount(client);
        final var serviceAccount = session.users().getServiceAccount(client);
        if (serviceAccount == null) {
            // Fail closed rather than hand back a credential that cannot identify a service point.
            throw new InternalServerErrorException("Service account was not created for the new client");
        }
        // The service_point_group_id claim mapper reads this attribute, so without it the
        // credential's token carries no service point at all.
        serviceAccount.setAttribute(ACTIVE_GROUP_ID_ATTRIBUTE, List.of(groupId));
        serviceAccount.grantRole(getOrCreateScopedServicePointUserRole(realm, groupId));

        final var body = new CredentialSecretResponse(
                client.getClientId(), request.getLabel().trim(), secret, now, null);

        auditLogger.record(CredentialAuditLogger.ACTION_CREATE, user, client.getClientId(), groupId);

        return cors.buildCorsResponse("POST",
                Response.status(Response.Status.CREATED)
                        .header(HttpHeaders.CACHE_CONTROL, NO_STORE)
                        .entity(objectMapper.writeValueAsString(body)));
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response list(@QueryParam("groupId") final String groupId) {
        log.debug("Listing client credentials for group {}", groupId);

        final var user = authenticatedUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (isBlank(groupId)) {
            return badRequest("groupId parameter is required");
        }

        requireAuthorisedFor(user, groupId.trim());

        final var realm = session.getContext().getRealm();
        final var credentials = managedCredentials(realm, groupId.trim())
                .map(this::toResponse)
                .toList();

        auditLogger.record(CredentialAuditLogger.ACTION_LIST, user, null, groupId.trim());

        return cors.buildCorsResponse("GET",
                Response.ok().entity(objectMapper.writeValueAsString(credentials)));
    }

    @OPTIONS
    @Path("/rotate")
    public Response rotatePreflight() {
        return cors.buildOptionsResponse("POST", "OPTIONS");
    }

    @POST
    @Path("/rotate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response rotate(final RotateCredentialRequest request) {
        log.debug("Rotating secret for client credential {}", request == null ? null : request.getClientId());

        final var user = authenticatedUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (request == null || isBlank(request.getClientId())) {
            return badRequest("clientId is required");
        }

        final var realm = session.getContext().getRealm();
        final var client = findManagedCredential(realm, request.getClientId().trim());
        if (client == null) {
            return notFound("Client credential not found");
        }

        requireAuthorisedFor(user, ownerGroupId(client));

        if (!client.isEnabled()) {
            return conflict("Client credential has been revoked and cannot be rotated");
        }

        final var rotatedAt = nowIso();
        final var secret = KeycloakModelUtils.generateSecret(client);
        client.setAttribute(ROTATED_AT_ATTRIBUTE, rotatedAt);

        final var body = new CredentialSecretResponse(
                client.getClientId(),
                client.getAttribute(LABEL_ATTRIBUTE),
                secret,
                client.getAttribute(CREATED_AT_ATTRIBUTE),
                rotatedAt);

        auditLogger.record(CredentialAuditLogger.ACTION_ROTATE, user, client.getClientId(), ownerGroupId(client));

        return cors.buildCorsResponse("POST",
                Response.ok()
                        .header(HttpHeaders.CACHE_CONTROL, NO_STORE)
                        .entity(objectMapper.writeValueAsString(body)));
    }

    @OPTIONS
    @Path("/secret")
    public Response secretPreflight() {
        return cors.buildOptionsResponse("GET", "OPTIONS");
    }

    @GET
    @Path("/secret")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response getSecret(@QueryParam("clientId") final String clientId) {
        log.debug("Retrieving secret for client credential {}", clientId);

        final var user = authenticatedUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (isBlank(clientId)) {
            return badRequest("clientId parameter is required");
        }

        final var realm = session.getContext().getRealm();
        final var client = findManagedCredential(realm, clientId.trim());
        if (client == null) {
            return notFound("Client credential not found");
        }

        requireAuthorisedFor(user, ownerGroupId(client));

        final var body = new CredentialSecretResponse(
                client.getClientId(),
                client.getAttribute(LABEL_ATTRIBUTE),
                client.getSecret(),
                client.getAttribute(CREATED_AT_ATTRIBUTE),
                client.getAttribute(ROTATED_AT_ATTRIBUTE));

        auditLogger.record(
                CredentialAuditLogger.ACTION_REVEAL_SECRET, user, client.getClientId(), ownerGroupId(client));

        return cors.buildCorsResponse("GET",
                Response.ok()
                        .header(HttpHeaders.CACHE_CONTROL, NO_STORE)
                        .entity(objectMapper.writeValueAsString(body)));
    }

    /**
     * Revokes a credential by disabling it. Disabling rather than deleting keeps the audit trail,
     * makes repeat calls naturally idempotent, and lets rotation of a revoked credential be
     * rejected rather than silently resurrecting it.
     */
    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response revoke(@QueryParam("clientId") final String clientId) {
        log.debug("Revoking client credential {}", clientId);

        final var user = authenticatedUser();
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (isBlank(clientId)) {
            return badRequest("clientId parameter is required");
        }

        final var realm = session.getContext().getRealm();
        final var client = findManagedCredential(realm, clientId.trim());
        if (client == null) {
            return notFound("Client credential not found");
        }

        requireAuthorisedFor(user, ownerGroupId(client));

        // Idempotent: revoking an already-revoked credential is a no-op success, not an error.
        if (client.isEnabled()) {
            client.setEnabled(false);
        }

        auditLogger.record(CredentialAuditLogger.ACTION_REVOKE, user, client.getClientId(), ownerGroupId(client));

        return cors.buildCorsResponse("DELETE",
                Response.ok().entity(objectMapper.writeValueAsString(toResponse(client))));
    }

    private UserModel authenticatedUser() {
        if (this.auth == null) {
            return null;
        }
        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }
        return user;
    }

    /**
     * Uniform authorisation for every endpoint: an Operator short-circuit applied first and
     * independently, then the scoped per-service-point check. RAID-827 requires these be separate
     * rather than OR-ed into a single condition, so Operator access does not depend on any
     * service-point reasoning.
     */
    private void requireAuthorisedFor(final UserModel user, final String groupId) {
        if (isOperator(user)) {
            return;
        }
        if (!isServicePointAdminOf(user, groupId)) {
            throw new NotAuthorizedException("Permission denied - not an admin of this service point");
        }
    }

    private boolean isOperator(final UserModel user) {
        return user.getRoleMappingsStream()
                .anyMatch(r -> r.getName().equals(OPERATOR_ROLE_NAME));
    }

    /**
     * Requires the scoped {@code service-point-admin:<groupId>} realm role. The flat legacy
     * {@code group-admin} role is deliberately not honoured, and there is no fallback.
     */
    private boolean isServicePointAdminOf(final UserModel user, final String groupId) {
        final var scopedRoleName = SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + groupId;
        return user.getRoleMappingsStream()
                .anyMatch(r -> r.getName().equals(scopedRoleName));
    }

    private java.util.stream.Stream<ClientModel> managedCredentials(final RealmModel realm, final String groupId) {
        return session.clients().getClientsStream(realm)
                .filter(this::isManagedCredential)
                .filter(c -> groupId.equals(c.getAttribute(GROUP_ID_ATTRIBUTE)));
    }

    private long countActiveCredentials(final RealmModel realm, final String groupId) {
        return managedCredentials(realm, groupId)
                .filter(ClientModel::isEnabled)
                .count();
    }

    private ClientModel findManagedCredential(final RealmModel realm, final String clientId) {
        final var client = session.clients().getClientByClientId(realm, clientId);
        if (client == null || !isManagedCredential(client)) {
            // Treat a non-managed client as absent so this endpoint can never be used to probe or
            // manipulate the realm's own clients.
            return null;
        }
        return client;
    }

    private boolean isManagedCredential(final ClientModel client) {
        return "true".equals(client.getAttribute(MANAGED_ATTRIBUTE));
    }

    private String ownerGroupId(final ClientModel client) {
        return client.getAttribute(GROUP_ID_ATTRIBUTE);
    }

    private CredentialResponse toResponse(final ClientModel client) {
        return new CredentialResponse(
                client.getClientId(),
                client.getAttribute(LABEL_ATTRIBUTE),
                client.getAttribute(CREATED_AT_ATTRIBUTE),
                client.getAttribute(ROTATED_AT_ATTRIBUTE),
                client.isEnabled());
    }

    /**
     * Attaches the client scopes a credential needs for its tokens to be usable.
     *
     * <p>Two distinct problems, both of which produce a credential that authenticates successfully
     * while being silently useless:
     *
     * <ol>
     *   <li>{@code session.clients().addClient(...)} is a raw model-layer create and does
     *       <em>not</em> apply the realm's default client scopes, unlike the Admin API path. Without
     *       them the client has no {@code roles} scope, so realm roles granted to its service
     *       account never appear in {@code realm_access.roles} and the API sees an unauthorised
     *       caller.
     *   <li>{@code service_point_group_id} is not a realm default scope at all (only the
     *       {@code raid-api} client carries it), so it must be attached explicitly or the token
     *       identifies no service point.
     * </ol>
     */
    private void attachClientScopes(final RealmModel realm, final ClientModel client) {
        // Materialise before mutating: addClientScope writes to the same underlying data the stream
        // is reading.
        realm.getDefaultClientScopesStream(true).toList()
                .forEach(scope -> client.addClientScope(scope, true));

        final var servicePointScope =
                KeycloakModelUtils.getClientScopeByName(realm, SERVICE_POINT_GROUP_ID_CLIENT_SCOPE);
        if (servicePointScope == null) {
            // Fail closed rather than issue a credential with no service point.
            throw new InternalServerErrorException(
                    "Required client scope '" + SERVICE_POINT_GROUP_ID_CLIENT_SCOPE + "' is missing from the realm");
        }
        client.addClientScope(servicePointScope, true);
    }

    /**
     * Looks up the scoped {@code service-point-user:<groupId>} realm role, creating it if absent,
     * mirroring {@code GroupController#getOrCreateServicePointAdminRole}.
     *
     * <p>Note {@code RoleContainerModel#addRole(String, String)} is (id, name), not
     * (name, description), so the single-arg overload is used and the description set separately.
     */
    private RoleModel getOrCreateScopedServicePointUserRole(final RealmModel realm, final String groupId) {
        final var roleName = SERVICE_POINT_USER_ROLE_PREFIX + ":" + groupId;
        final var existingRole = realm.getRole(roleName);
        if (existingRole != null) {
            return existingRole;
        }
        final var role = realm.addRole(roleName);
        role.setDescription("Service point user for group " + groupId);
        return role;
    }

    private String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    @SneakyThrows
    private Response badRequest(final String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(objectMapper.writeValueAsString(new ErrorResponse(message)))
                .build();
    }

    @SneakyThrows
    private Response notFound(final String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(objectMapper.writeValueAsString(new ErrorResponse(message)))
                .build();
    }

    @SneakyThrows
    private Response conflict(final String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(objectMapper.writeValueAsString(new ErrorResponse(message)))
                .build();
    }

    private record ErrorResponse(String error) {}
}
