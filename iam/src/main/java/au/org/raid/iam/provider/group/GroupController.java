package au.org.raid.iam.provider.group;

import au.org.raid.iam.provider.cors.Cors;
import au.org.raid.iam.provider.group.dto.Grant;
import au.org.raid.iam.provider.group.dto.AddGroupAdminRequest;
import au.org.raid.iam.provider.group.dto.RemoveGroupAdminRequest;
import au.org.raid.iam.provider.group.dto.GroupJoinRequest;
import au.org.raid.iam.provider.group.dto.GroupLeaveRequest;
import au.org.raid.iam.provider.group.dto.SetActiveGroupRequest;
import au.org.raid.iam.provider.group.dto.RemoveActiveGroupRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
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
    private final AuthenticationManager.AuthResult auth;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KeycloakSession session;
    private final Cors cors;

    public GroupController(final KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        this.cors = new Cors(session, objectMapper);
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

        final var user = auth.getSession().getUser();
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
        final var user = auth.getSession().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }
        if (!isGroupAdmin(user) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied");
        }
        
        // Return error if groupId not provided
        if (groupId == null || groupId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("\"groupId parameter is required\"")
                .build();
        }
        
        final var realm = session.getContext().getRealm();
        var group = session.groups().getGroupById(realm, groupId);
        
        // Check if group exists
        if (group == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("\"Group not found\"")
                .build();
        }
        
        // Check if user has access to this group
        if (!isOperator(user) && !user.getGroupsStream().anyMatch(g -> g.getId().equals(groupId))) {
            throw new NotAuthorizedException("Permission denied for accessing this group");
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

        final var user = auth.getSession().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isGroupAdmin(user) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group admin");
        }

        if (!isGroupMember(user, grant.getGroupId()) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group member");
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

        final var user = auth.getSession().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isGroupAdmin(user) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group admin");
        }

        if (!isGroupMember(user, grant.getGroupId()) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group member");
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

        final var user = auth.getSession().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isGroupAdmin(user) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group admin");
        }

        if (!isGroupMember(user, grant.getGroupId()) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group member");
        }

        final var realm = session.getContext().getRealm();
        final var groupUser = session.users().getUserById(realm, grant.getUserId());
        final var groupAdminUserRole = session.roles()
                .getRealmRolesStream(realm, null, null)
                .filter(r -> r.getName().equals(GROUP_ADMIN_ROLE_NAME))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException(GROUP_ADMIN_ROLE_NAME));

        groupUser.deleteRoleMapping(groupAdminUserRole);

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

        final var user = auth.getSession().getUser();
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }

        if (!isGroupAdmin(user) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group admin");
        }

        if (!isGroupMember(user, grant.getGroupId()) && !isOperator(user)) {
            throw new NotAuthorizedException("Permission denied - not a group member");
        }

        final var realm = session.getContext().getRealm();
        final var groupUser = session.users().getUserById(realm, grant.getUserId());
        final var groupAdminUserRole = session.roles()
                .getRealmRolesStream(realm, null, null)
                .filter(r -> r.getName().equals(GROUP_ADMIN_ROLE_NAME))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException(GROUP_ADMIN_ROLE_NAME));

        groupUser.grantRole(groupAdminUserRole);

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

        final var user = auth.getSession().getUser();
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

        final var user = auth.getSession().getUser();
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

        final var user = auth.getSession().getUser();
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
}