package au.org.raid.iam.provider.group;

import au.org.raid.iam.provider.cors.Cors;
import au.org.raid.iam.provider.group.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.management.relation.RoleNotFoundException;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Provider
public class GroupController {
    private static final String OPERATOR_ROLE_NAME = "operator";
    private static final String GROUP_ADMIN_ROLE_NAME = "group-admin";
    private static final String SERVICE_POINT_USER_ROLE = "service-point-user";
    // Scoped role name format: "service-point-admin:<groupId>". Granted alongside the flat
    // GROUP_ADMIN_ROLE_NAME on group creation and group-admin grant (dual-write), and enforced as
    // the primary authorization check in isGroupAdminOf below.
    private static final String SERVICE_POINT_ADMIN_ROLE_PREFIX = "service-point-admin";
    // Config flag controlling whether the legacy flat GROUP_ADMIN_ROLE_NAME (plus group
    // membership) still authorises group-admin operations. Keycloak's Config.Scope isn't wired
    // through to this per-request resource today (GroupControllerResourceProviderFactory#init is
    // unused and GroupControllerResourceProvider only forwards the KeycloakSession), so this is
    // read directly from the environment, following Keycloak's own SPI env var naming convention
    // in spirit. Defaults to true so existing flat group-admin role holders aren't locked out
    // until service points finish migrating onto scoped roles.
    private static final String FLAT_GROUP_ADMIN_FALLBACK_ENV_VAR = "RAID_FLAT_GROUP_ADMIN_FALLBACK";
    private final AuthenticationManager.AuthResult auth;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KeycloakSession session;
    private final Cors cors;
    private final boolean flatGroupAdminFallbackEnabled;

    public GroupController(final KeycloakSession session) {
        this(session, resolveFlatGroupAdminFallbackEnabled());
    }

    // Package-private seam for tests: lets us set the fallback flag directly rather than having to
    // mutate process environment variables, which the JVM does not support safely at runtime.
    GroupController(final KeycloakSession session, final boolean flatGroupAdminFallbackEnabled) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        this.cors = new Cors(session, objectMapper);
        this.flatGroupAdminFallbackEnabled = flatGroupAdminFallbackEnabled;
    }

    private static boolean resolveFlatGroupAdminFallbackEnabled() {
        final var value = System.getenv(FLAT_GROUP_ADMIN_FALLBACK_ENV_VAR);
        if (value == null || value.isBlank()) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    @OPTIONS
    @Path("/all")
    public Response getGroupsPreflight() {
        return cors.buildOptionsResponse("GET", "PUT", "OPTIONS");
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() throws JsonProcessingException {
        log.debug("Getting all groups");

        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        final var realm = session.getContext().getRealm();
        final var groups = session.groups().getGroupsStream(realm)
                .map(g -> {
                    final var map = new HashMap<String, Object>();
                    map.put("id", g.getId());
                    map.put("name", g.getName());
                    map.put("attributes", g.getAttributes());
                    return map;
                })
                .toList();

        final var responseBody = new HashMap<String, Object>();
        responseBody.put("groups", groups);

        return cors.buildCorsResponse("GET",
                Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
    }

    @OPTIONS
    @Path("")
    public Response preflight() {
        return cors.buildOptionsResponse("GET", "PUT", "OPTIONS");
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@QueryParam("groupId") String groupId) throws JsonProcessingException {
        log.debug("Getting members of group");
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }
        // Return error if groupId not provided
        if (groupId == null || groupId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("\"groupId parameter is required\"")
                .build();
        }

        if (!isOperator(user) && !isGroupAdminOf(user, groupId)) {
            throw new NotAuthorizedException("Permission denied - not an admin of this group");
        }

        final var realm = session.getContext().getRealm();
        var group = session.groups().getGroupById(realm, groupId);

        // Check if group exists
        if (group == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("\"Group not found\"")
                .build();
        }

        final var responseBody = new HashMap<String, Object>();
        responseBody.put("id", group.getId());
        responseBody.put("name", group.getName());
        responseBody.put("attributes", group.getAttributes());
        
        final var members = session.users().getGroupMembersStream(realm, group)
            .filter(u -> !u.getId().equals(user.getId()))
            .map(u -> {
                final var map = new HashMap<String, Object>();
                map.put("id", u.getId());
                map.put("attributes", u.getAttributes());
                map.put("roles", u.getRoleMappingsStream().map(RoleModel::getName).toList());
                return map;
            })
            .toList();
            
        responseBody.put("members", members);
        return cors.buildCorsResponse("GET",
            Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
    }

    @OPTIONS
    @Path("/grant")
    public Response grantPreflight() {
        return cors.buildOptionsResponse("PUT");
    }

    @PUT
    @Path("/grant")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response grant(final Grant grant) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isOperator(user) && !isGroupAdminOf(user, grant.getGroupId())) {
            throw new NotAuthorizedException("Permission denied - not an admin of this group");
        }

        final var realm = session.getContext().getRealm();
        final var groupUser = session.users().getUserById(realm, grant.getUserId());
        final var servicePointUserRole = session.roles()
                .getRealmRolesStream(realm, null, null)
                .filter(r -> r.getName().equals(SERVICE_POINT_USER_ROLE))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException(SERVICE_POINT_USER_ROLE));

        groupUser.grantRole(servicePointUserRole);

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/revoke")
    public Response revokePreflight() {
        return cors.buildOptionsResponse("PUT");
    }

    @PUT
    @Path("/revoke")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response revoke(final Grant grant) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isOperator(user) && !isGroupAdminOf(user, grant.getGroupId())) {
            throw new NotAuthorizedException("Permission denied - not an admin of this group");
        }

        final var realm = session.getContext().getRealm();
        final var groupUser = session.users().getUserById(realm, grant.getUserId());
        final var servicePointUserRole = session.roles()
                .getRealmRolesStream(realm, null, null)
                .filter(r -> r.getName().equals(SERVICE_POINT_USER_ROLE))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException(SERVICE_POINT_USER_ROLE));

        groupUser.deleteRoleMapping(servicePointUserRole);

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/group-admin")
    public Response grantGroupAdminPreflight() {
        return cors.buildOptionsResponse("PUT", "DELETE");
    }

    @DELETE
    @Path("/group-admin")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response grant(final RemoveGroupAdminRequest grant) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isOperator(user) && !isGroupAdminOf(user, grant.getGroupId())) {
            throw new NotAuthorizedException("Permission denied - not an admin of this group");
        }

        final var realm = session.getContext().getRealm();
        final var groupUser = session.users().getUserById(realm, grant.getUserId());
        final var groupAdminUserRole = session.roles()
                .getRealmRolesStream(realm, null, null)
                .filter(r -> r.getName().equals(GROUP_ADMIN_ROLE_NAME))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException(GROUP_ADMIN_ROLE_NAME));

        groupUser.deleteRoleMapping(groupAdminUserRole);

        // Dual-revoke: also remove the scoped service-point-admin role for this group, if it
        // exists, so admin access is fully withdrawn regardless of the fallback setting.
        final var servicePointAdminRole = realm.getRole(servicePointAdminRoleName(grant.getGroupId()));
        if (servicePointAdminRole != null) {
            groupUser.deleteRoleMapping(servicePointAdminRole);
        }

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @PUT
    @Path("/group-admin")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response grant(final AddGroupAdminRequest grant) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isOperator(user) && !isGroupAdminOf(user, grant.getGroupId())) {
            throw new NotAuthorizedException("Permission denied - not an admin of this group");
        }

        final var realm = session.getContext().getRealm();
        final var groupUser = session.users().getUserById(realm, grant.getUserId());
        final var groupAdminUserRole = session.roles()
                .getRealmRolesStream(realm, null, null)
                .filter(r -> r.getName().equals(GROUP_ADMIN_ROLE_NAME))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException(GROUP_ADMIN_ROLE_NAME));

        groupUser.grantRole(groupAdminUserRole);

        // Dual-write: also grant the scoped service-point-admin role for this group so the
        // grantee keeps admin access once the flat group-admin fallback is disabled.
        groupUser.grantRole(getOrCreateServicePointAdminRole(realm, grant.getGroupId()));

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/join")
    public Response joinPreflight() {
        return cors.buildOptionsResponse("PUT");
    }

    @PUT
    @Path("/join")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response join(GroupJoinRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        user.joinGroup(session.groups().getGroupById(session.getContext().getRealm(), request.getGroupId()));

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/leave")
    public Response leavePreflight() {
        return cors.buildOptionsResponse("PUT");
    }

    @PUT
    @Path("/leave")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response leave(GroupLeaveRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        final var realm = session.getContext().getRealm();
        final var user = session.users().getUserById(realm, request.getUserId());
        user.leaveGroup(session.groups().getGroupById(session.getContext().getRealm(), request.getGroupId()));

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/active-group")
    public Response setActiveGroupPreflight() {
        return cors.buildOptionsResponse("PUT", "DELETE");
    }

    @PUT
    @Path("/active-group")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setActiveGroup(SetActiveGroupRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        user.setAttribute("activeGroupId", List.of(request.getActiveGroupId()));

        return cors.buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @DELETE
    @Path("/active-group")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeActiveGroup(RemoveActiveGroupRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var realm = session.getContext().getRealm();
        final var user = session.users().getUserById(realm, request.getUserId());
        user.removeAttribute("activeGroupId");

        return cors.buildCorsResponse("DELETE",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/user-groups")
    public Response userGroupsPreflight() {
        return cors.buildOptionsResponse("GET");
    }

    @GET
    @Path("/user-groups")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response userGroups() {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        final var userGroups = user.getGroupsStream()
                .map(g -> new GroupDetails(g.getId(), g.getName()))
                .toList();

        return cors.buildCorsResponse("GET",
                Response.ok().entity(objectMapper.writeValueAsString(userGroups)));
    }

    private record GroupDetails(String id, String name) {}

    private boolean isGroupAdmin(final UserModel user) {
        return !user.getRoleMappingsStream()
                .filter(r -> r.getName().equals(GROUP_ADMIN_ROLE_NAME))
                .toList().isEmpty();
    }

    /**
     * A user is an admin of the given group if they hold the scoped
     * "service-point-admin:&lt;groupId&gt;" realm role, or - while the flat group-admin fallback is
     * enabled - if they hold the legacy flat group-admin role and are a member of the group.
     */
    private boolean isGroupAdminOf(final UserModel user, final String groupId) {
        final var scopedRoleName = servicePointAdminRoleName(groupId);
        final var hasScopedRole = user.getRoleMappingsStream()
                .anyMatch(r -> r.getName().equals(scopedRoleName));

        if (hasScopedRole) {
            return true;
        }

        return flatGroupAdminFallbackEnabled && isGroupAdmin(user) && isGroupMember(user, groupId);
    }

    private boolean isGroupMember(final UserModel user, final String groupId) {
        return !user.getGroupsStream()
                .filter(g -> g.getId().equals(groupId))
                .toList().isEmpty();
    }

    private boolean isOperator(final UserModel user) {
        return !user.getRoleMappingsStream()
                .filter(r -> r.getName().equals(OPERATOR_ROLE_NAME))
                .toList().isEmpty();
    }

    /**
     * Looks up the realm role scoped to a single service point group
     * ("service-point-admin:<groupId>"), creating it if it does not already exist.
     */
    private RoleModel getOrCreateServicePointAdminRole(final RealmModel realm, final String groupId) {
        final var roleName = servicePointAdminRoleName(groupId);
        final var existingRole = realm.getRole(roleName);
        if (existingRole != null) {
            return existingRole;
        }

        // Note: RoleContainerModel#addRole(String, String) is (id, name) - not (name, description).
        // Use the single-arg overload (generated id, given name) and set the description separately.
        final var role = realm.addRole(roleName);
        role.setDescription("Service point admin for group " + groupId);
        return role;
    }

    private static String servicePointAdminRoleName(final String groupId) {
        return SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + groupId;
    }

    @OPTIONS
    @Path("/create")
    public Response createGroupPreflight() {
        return cors.buildOptionsResponse("POST", "OPTIONS");
    }
    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response createGroup(CreateGroupRequest request) {
        log.debug("Creating new group with name: {}", request.getName());

        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        // Only operators can create groups
        if (!isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not authorized to create groups");
        }

        // Validate request
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Group name is required\"}")
                    .build();
        }

        final var realm = session.getContext().getRealm();

        // Check if group with same name already exists
        var existingGroups = session.groups().getGroupsStream(realm)
                .filter(g -> g.getName().equals(request.getName().trim()))
                .toList();

        if (!existingGroups.isEmpty()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"Group with this name already exists\"}")
                    .build();
        }

        try {
            // Create the group
            var newGroup = session.groups().createGroup(realm, request.getName().trim());

            // Set path if provided, otherwise use default
            String path = request.getPath() != null ? request.getPath() : "/" + request.getName().trim();
            // Note: Keycloak groups don't have a direct "path" field, but you can store it as an attribute
            newGroup.setAttribute("path", List.of(path));

            // Add any additional attributes if provided
            if (request.getAttributes() != null) {
                request.getAttributes().forEach((key, values) -> {
                    newGroup.setAttribute(key, values);
                });
            }

            // Add the creating user to the group as a member
            user.joinGroup(newGroup);

            // make the creator a group admin
            final var groupAdminRole = session.roles()
                    .getRealmRolesStream(realm, null, null)
                    .filter(r -> r.getName().equals(GROUP_ADMIN_ROLE_NAME))
                    .findFirst();

            if (groupAdminRole.isPresent()) {
                user.grantRole(groupAdminRole.get());
            }

            // Also grant the scoped service-point-admin role for the new group. This is a
            // dual-write alongside the flat group-admin role above for backward compatibility
            // while the transition to scoped roles is in progress.
            final var servicePointAdminRole = getOrCreateServicePointAdminRole(realm, newGroup.getId());
            user.grantRole(servicePointAdminRole);

            // Prepare response
            CreateGroupResponse response = new CreateGroupResponse(
                    newGroup.getId(),
                    newGroup.getName(),
                    newGroup.getAttributes(),
                    "Group created successfully"
            );

            return cors.buildCorsResponse("POST",
                    Response.status(Response.Status.CREATED)
                            .entity(objectMapper.writeValueAsString(response)));

        } catch (Exception e) {
            log.error("Error creating group: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create group\"}")
                    .build();
        }
    }
    @OPTIONS
    @Path("/delete")
    public Response deleteGroupPreflight() {
        return cors.buildOptionsResponse("DELETE", "OPTIONS");
    }

    @DELETE
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response deleteGroup(@QueryParam("groupId") String groupId) {
        log.debug("Deleting group with id: {}", groupId);

        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        // Only operators can delete groups
        if (!isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not authorized to delete groups");
        }

        // Validate groupId
        if (groupId == null || groupId.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"groupId parameter is required\"}")
                    .build();
        }

        final var realm = session.getContext().getRealm();
        var group = session.groups().getGroupById(realm, groupId);

        if (group == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Group not found\"}")
                    .build();
        }

        try {
            String groupName = group.getName();
            session.groups().removeGroup(realm, group);

            var responseBody = new HashMap<String, String>();
            responseBody.put("message", "Group deleted successfully");
            responseBody.put("groupId", groupId);
            responseBody.put("groupName", groupName);

            return cors.buildCorsResponse("DELETE",
                    Response.ok().entity(objectMapper.writeValueAsString(responseBody)));

        } catch (Exception e) {
            log.error("Error deleting group: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to delete group\"}")
                    .build();
        }
    }

    @OPTIONS
    @Path("/migrate-service-point-admins")
    public Response migrateServicePointAdminsPreflight() {
        return cors.buildOptionsResponse("POST", "OPTIONS");
    }

    /**
     * One-off, idempotent backfill for RAID-712: grants the scoped
     * "service-point-admin:&lt;groupId&gt;" realm role to every current holder of the legacy flat
     * GROUP_ADMIN_ROLE_NAME, for each group they belong to. This preserves each flat
     * group-admin's existing effective access while service points transition onto scoped roles
     * (see role-permissions.md section 9).
     *
     * <p>Operator-only. Safe to re-run: users who already hold the scoped role for a group are
     * counted as skipped rather than re-granted, and existing scoped roles are reused rather than
     * recreated.
     *
     * <p>Note: this intentionally over-grants - a flat group-admin is granted the scoped admin
     * role for every group they are a member of, not just the group(s) they were originally
     * intended to administer. Pruning any resulting excess scoped grants is deferred to a future
     * RAID-712 follow-up once service points have reviewed their membership.
     */
    @POST
    @Path("/migrate-service-point-admins")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response migrateServicePointAdmins() {
        log.debug("Migrating flat group-admin holders to scoped service-point-admin roles");

        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var user = auth.session().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not authorized to migrate service point admins");
        }

        final var realm = session.getContext().getRealm();
        final var flatGroupAdminRole = realm.getRole(GROUP_ADMIN_ROLE_NAME);

        if (flatGroupAdminRole == null) {
            final var responseBody = new MigrationResult(0, 0, 0, 0,
                    "Flat group-admin role not present; nothing to migrate");
            return cors.buildCorsResponse("POST",
                    Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
        }

        try {
            var flatGroupAdminUsers = 0;
            var rolesCreated = 0;
            var grantsAdded = 0;
            var grantsSkipped = 0;

            var firstResult = 0;
            final var pageSize = 100;
            while (true) {
                final var page = session.users()
                        .getRoleMembersStream(realm, flatGroupAdminRole, firstResult, pageSize)
                        .toList();

                if (page.isEmpty()) {
                    break;
                }

                for (final var member : page) {
                    flatGroupAdminUsers++;

                    // Materialise before granting roles below - member.grantRole mutates this
                    // user's role mappings, and we must not mutate them while consuming a live
                    // stream over the same underlying data.
                    final var groups = member.getGroupsStream().toList();

                    for (final var group : groups) {
                        final var roleName = servicePointAdminRoleName(group.getId());
                        final var existed = realm.getRole(roleName) != null;
                        final var scopedRole = getOrCreateServicePointAdminRole(realm, group.getId());

                        if (!existed) {
                            rolesCreated++;
                        }

                        if (member.hasDirectRole(scopedRole)) {
                            grantsSkipped++;
                        } else {
                            member.grantRole(scopedRole);
                            grantsAdded++;
                        }
                    }
                }

                firstResult += pageSize;
            }

            final var responseBody = new MigrationResult(
                    flatGroupAdminUsers, rolesCreated, grantsAdded, grantsSkipped, "Migration complete");

            return cors.buildCorsResponse("POST",
                    Response.ok().entity(objectMapper.writeValueAsString(responseBody)));

        } catch (Exception e) {
            log.error("Error migrating service point admins: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to migrate service point admins\"}")
                    .build();
        }
    }

    private record MigrationResult(
            int flatGroupAdminUsers, int rolesCreated, int grantsAdded, int grantsSkipped, String message) {}
}
