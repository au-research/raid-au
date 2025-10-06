package au.org.raid.iam.provider.group;

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
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:7080",
            "https://app.test.raid.org.au",
            "https://app3.test.raid.org.au",
            "https://app.demo.raid.org.au",
            "https://app3.demo.raid.org.au",
            "https://app.stage.raid.org.au",
            "https://app3.stage.raid.org.au",
            "https://app.prod.raid.org.au"
    );

    private static final String OPERATOR_ROLE_NAME = "operator";
    private static final String GROUP_ADMIN_ROLE_NAME = "group-admin";
    private static final String SERVICE_POINT_USER_ROLE = "service-point-user";
    private final AuthenticationManager.AuthResult auth;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KeycloakSession session;

    public GroupController(final KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }


    @SneakyThrows
    private Response buildCorsResponse(String method, Response.ResponseBuilder responseBuilder) {
        String origin = session.getContext().getRequestHeaders().getHeaderString("Origin");

        // Only set the Origin header if it's in our allowed list
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            responseBuilder.header("Access-Control-Allow-Origin", origin);
            responseBuilder.header("Access-Control-Allow-Credentials", "true");
        }

        responseBuilder.header("Access-Control-Allow-Methods", String.join(", ", method));
        responseBuilder.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
        responseBuilder.header("Access-Control-Max-Age", "3600");

        final var response = responseBuilder.build();
        log.debug("Returning response {}", objectMapper.writeValueAsString(response));
        return response;
    }

    @SneakyThrows
    private Response buildOptionsResponse(String... methods) {
        String origin = session.getContext().getRequestHeaders().getHeaderString("Origin");
        Response.ResponseBuilder builder = Response.ok();

        // Only set the Origin header if it's in our allowed list
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            builder.header("Access-Control-Allow-Origin", origin);
            builder.header("Access-Control-Allow-Credentials", "true");
        }

        builder.header("Access-Control-Allow-Methods", String.join(", ", methods));
        builder.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
        builder.header("Access-Control-Max-Age", "3600");

        final var response = builder.build();
        log.debug("Returning response {}", objectMapper.writeValueAsString(response));
        return response;
    }

    @OPTIONS
    @Path("/all")
    public Response getGroupsPreflight() {
        return buildOptionsResponse("GET", "PUT", "OPTIONS");
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

        return buildCorsResponse("GET",
                Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
    }

    @OPTIONS
    @Path("")
    public Response preflight() {
        return buildOptionsResponse("GET", "PUT", "OPTIONS");
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
        return buildCorsResponse("GET",
            Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
    }

    @OPTIONS
    @Path("/grant")
    public Response grantPreflight() {
        return buildOptionsResponse("PUT");
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

        return buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/revoke")
    public Response revokePreflight() {
        return buildOptionsResponse("PUT");
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

        return buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/group-admin")
    public Response grantGroupAdminPreflight() {
        return buildOptionsResponse("PUT", "DELETE");
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

        return buildCorsResponse("PUT",
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

        return buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/join")
    public Response joinPreflight() {
        return buildOptionsResponse("PUT");
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

        return buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/leave")
    public Response leavePreflight() {
        return buildOptionsResponse("PUT");
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

        return buildCorsResponse("PUT",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/active-group")
    public Response setActiveGroupPreflight() {
        return buildOptionsResponse("PUT", "DELETE");
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

        return buildCorsResponse("PUT",
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

        return buildCorsResponse("DELETE",
                Response.ok().entity("{}"));
    }

    @OPTIONS
    @Path("/user-groups")
    public Response userGroupsPreflight() {
        return buildOptionsResponse("GET");
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

        return buildCorsResponse("GET",
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

    @OPTIONS
    @Path("/create")
    public Response createGroupPreflight() {
        return buildOptionsResponse("POST", "OPTIONS");
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

        final var user = auth.getSession().getUser();
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

            // Prepare response
            CreateGroupResponse response = new CreateGroupResponse(
                    newGroup.getId(),
                    newGroup.getName(),
                    newGroup.getAttributes(),
                    "Group created successfully"
            );

            return buildCorsResponse("POST",
                    Response.status(Response.Status.CREATED)
                            .entity(objectMapper.writeValueAsString(response)));

        } catch (Exception e) {
            log.error("Error creating group: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create group\"}")
                    .build();
        }
    }
}
