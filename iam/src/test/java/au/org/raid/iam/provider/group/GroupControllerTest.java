package au.org.raid.iam.provider.group;

import au.org.raid.iam.provider.cors.Cors;
import au.org.raid.iam.provider.group.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupControllerTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext keycloakContext;
    @Mock private RealmModel realm;
    @Mock private GroupProvider groupProvider;
    @Mock private UserProvider userProvider;
    @Mock private RoleProvider roleProvider;
    @Mock private ClientProvider clientProvider;
    @Mock private HttpHeaders httpHeaders;
    @Mock private AuthenticationManager.AuthResult authResult;
    @Mock private UserSessionModel userSession;
    @Mock private UserModel user;

    private void setupSession() {
        when(session.getContext()).thenReturn(keycloakContext);
        when(keycloakContext.getRealm()).thenReturn(realm);
        when(keycloakContext.getRequestHeaders()).thenReturn(httpHeaders);
        when(session.groups()).thenReturn(groupProvider);
        when(session.users()).thenReturn(userProvider);
        when(session.roles()).thenReturn(roleProvider);
        when(session.clients()).thenReturn(clientProvider);
        when(clientProvider.getClientsStream(any())).thenAnswer(inv -> Stream.empty());
    }

    private GroupController createController(AuthenticationManager.AuthResult auth) {
        setupSession();
        try (var ignoredAuth = mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                (mock, ctx) -> when(mock.authenticate()).thenReturn(auth));
             var ignoredCors = mockConstruction(Cors.class,
                (mock, ctx) -> {
                    when(mock.buildCorsResponse(anyString(), any(Response.ResponseBuilder.class)))
                            .thenAnswer(inv -> ((Response.ResponseBuilder) inv.getArgument(1)).build());
                    when(mock.buildOptionsResponse(any(String[].class)))
                            .thenReturn(Response.ok().build());
                })) {
            return new GroupController(session);
        }
    }

    private GroupController createAuthenticatedController() {
        when(authResult.session()).thenReturn(userSession);
        when(userSession.getUser()).thenReturn(user);
        return createController(authResult);
    }

    private void setupOperator() {
        RoleModel operatorRole = mock(RoleModel.class);
        when(operatorRole.getName()).thenReturn("operator");
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(operatorRole));
    }

    private void setupGroupAdmin(String groupId) {
        RoleModel groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(groupAdminRole));

        GroupModel userGroup = mock(GroupModel.class);
        when(userGroup.getId()).thenReturn(groupId);
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.of(userGroup));
    }

    private void setupRegularUser() {
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.empty());
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.empty());
    }

    // --- getGroups ---

    @Test
    void getGroups_returnsUnauthorized_whenAuthIsNull() throws JsonProcessingException {
        var controller = createController(null);
        Response response = controller.getGroups();
        assertEquals(401, response.getStatus());
    }

    @Test
    void getGroups_returnsGroupsList() throws JsonProcessingException {
        var controller = createAuthenticatedController();

        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn("group-1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.of(group));

        Response response = controller.getGroups();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    // --- get (group members) ---

    @Test
    void get_returnsUnauthorized_whenAuthIsNull() throws JsonProcessingException {
        var controller = createController(null);
        Response response = controller.get("group-1");
        assertEquals(401, response.getStatus());
    }

    @Test
    void get_throwsNotAuthorized_whenNotAdminOrOperator() {
        var controller = createAuthenticatedController();
        setupRegularUser();

        assertThrows(NotAuthorizedException.class, () -> controller.get("group-1"));
    }

    @Test
    void get_returnsBadRequest_whenNoGroupId() throws JsonProcessingException {
        var controller = createAuthenticatedController();
        setupOperator();

        Response response = controller.get(null);
        assertEquals(400, response.getStatus());
    }

    @Test
    void get_returnsBadRequest_whenEmptyGroupId() throws JsonProcessingException {
        var controller = createAuthenticatedController();
        setupOperator();

        Response response = controller.get("");
        assertEquals(400, response.getStatus());
    }

    @Test
    void get_returnsNotFound_whenGroupDoesNotExist() throws JsonProcessingException {
        var controller = createAuthenticatedController();
        setupOperator();
        when(groupProvider.getGroupById(realm, "nonexistent")).thenReturn(null);

        Response response = controller.get("nonexistent");
        assertEquals(404, response.getStatus());
    }

    @Test
    void get_returnsGroupMembers_whenOperator() throws JsonProcessingException {
        var controller = createAuthenticatedController();
        setupOperator();
        when(user.getId()).thenReturn("current-user");

        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn("group-1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupById(realm, "group-1")).thenReturn(group);

        UserModel member = mock(UserModel.class);
        when(member.getId()).thenReturn("member-1");
        when(member.getAttributes()).thenReturn(Map.of());
        when(member.getRoleMappingsStream()).thenAnswer(inv -> Stream.empty());
        when(userProvider.getGroupMembersStream(realm, group)).thenReturn(Stream.of(member));

        Response response = controller.get("group-1");
        assertEquals(200, response.getStatus());
    }

    @Test
    void get_excludesCurrentUserFromMembers() throws JsonProcessingException {
        var controller = createAuthenticatedController();
        setupOperator();
        when(user.getId()).thenReturn("current-user");

        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn("group-1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupById(realm, "group-1")).thenReturn(group);

        // Return current user as a group member — should be filtered out
        when(userProvider.getGroupMembersStream(realm, group)).thenReturn(Stream.of(user));
        when(user.getAttributes()).thenReturn(Map.of());

        Response response = controller.get("group-1");
        assertEquals(200, response.getStatus());
        String entity = (String) response.getEntity();
        assertFalse(entity.contains("current-user"));
    }

    @Test
    void get_throwsNotAuthorized_whenGroupAdminNotInGroup() {
        var controller = createAuthenticatedController();

        RoleModel groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(groupAdminRole));
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.empty());

        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn("group-1");
        when(groupProvider.getGroupById(realm, "group-1")).thenReturn(group);

        assertThrows(NotAuthorizedException.class, () -> controller.get("group-1"));
    }

    @Test
    void get_returnsMembers_whenGroupAdminInGroup() throws JsonProcessingException {
        var controller = createAuthenticatedController();
        setupGroupAdmin("group-1");
        when(user.getId()).thenReturn("current-user");

        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn("group-1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupById(realm, "group-1")).thenReturn(group);
        when(userProvider.getGroupMembersStream(realm, group)).thenReturn(Stream.empty());

        Response response = controller.get("group-1");
        assertEquals(200, response.getStatus());
    }

    // --- grant (service-point-user role) ---

    @Test
    void grant_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);
        Grant grant = new Grant();
        grant.setUserId("user-1");
        grant.setGroupId("group-1");

        Response response = controller.grant(grant);
        assertEquals(401, response.getStatus());
    }

    @Test
    void grant_throwsNotAuthorized_whenNotAdminOrOperator() {
        var controller = createAuthenticatedController();
        setupRegularUser();

        Grant grant = new Grant();
        grant.setUserId("user-1");
        grant.setGroupId("group-1");

        assertThrows(NotAuthorizedException.class, () -> controller.grant(grant));
    }

    @Test
    void grant_grantsServicePointUserRole() {
        var controller = createAuthenticatedController();
        setupOperator();

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);

        RoleModel servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(roleProvider.getRealmRolesStream(eq(realm), isNull(), isNull()))
                .thenAnswer(inv -> Stream.of(servicePointUserRole));

        Grant grant = new Grant();
        grant.setUserId("user-1");
        grant.setGroupId("group-1");

        Response response = controller.grant(grant);
        assertEquals(200, response.getStatus());
        verify(targetUser).grantRole(servicePointUserRole);
    }

    // --- revoke (service-point-user role) ---

    @Test
    void revoke_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);
        Grant grant = new Grant();
        grant.setUserId("user-1");
        grant.setGroupId("group-1");

        Response response = controller.revoke(grant);
        assertEquals(401, response.getStatus());
    }

    @Test
    void revoke_revokesServicePointUserRole() {
        var controller = createAuthenticatedController();
        setupOperator();

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);

        RoleModel servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(roleProvider.getRealmRolesStream(eq(realm), isNull(), isNull()))
                .thenAnswer(inv -> Stream.of(servicePointUserRole));

        Grant grant = new Grant();
        grant.setUserId("user-1");
        grant.setGroupId("group-1");

        Response response = controller.revoke(grant);
        assertEquals(200, response.getStatus());
        verify(targetUser).deleteRoleMapping(servicePointUserRole);
    }

    // --- group-admin (add) ---

    @Test
    void addGroupAdmin_grantsGroupAdminRole() {
        var controller = createAuthenticatedController();
        setupOperator();

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);

        RoleModel groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), isNull(), isNull()))
                .thenAnswer(inv -> Stream.of(groupAdminRole));

        AddGroupAdminRequest request = new AddGroupAdminRequest();
        request.setUserId("user-1");
        request.setGroupId("group-1");

        Response response = controller.grant(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).grantRole(groupAdminRole);
    }

    // --- group-admin (remove) ---

    @Test
    void removeGroupAdmin_revokesGroupAdminRole() {
        var controller = createAuthenticatedController();
        setupOperator();

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);

        RoleModel groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), isNull(), isNull()))
                .thenAnswer(inv -> Stream.of(groupAdminRole));

        RemoveGroupAdminRequest request = new RemoveGroupAdminRequest();
        request.setUserId("user-1");
        request.setGroupId("group-1");

        Response response = controller.grant(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).deleteRoleMapping(groupAdminRole);
    }

    // --- join ---

    @Test
    void join_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);
        GroupJoinRequest request = new GroupJoinRequest();
        request.setGroupId("group-1");

        Response response = controller.join(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void join_addsUserToGroup() {
        var controller = createAuthenticatedController();

        GroupModel group = mock(GroupModel.class);
        when(groupProvider.getGroupById(realm, "group-1")).thenReturn(group);

        GroupJoinRequest request = new GroupJoinRequest();
        request.setGroupId("group-1");

        Response response = controller.join(request);
        assertEquals(200, response.getStatus());
        verify(user).joinGroup(group);
    }

    // --- leave ---

    @Test
    void leave_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);
        GroupLeaveRequest request = new GroupLeaveRequest();
        request.setGroupId("group-1");
        request.setUserId("user-1");

        Response response = controller.leave(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void leave_removesUserFromGroup() {
        var controller = createAuthenticatedController();

        UserModel leavingUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(leavingUser);

        GroupModel group = mock(GroupModel.class);
        when(groupProvider.getGroupById(realm, "group-1")).thenReturn(group);

        GroupLeaveRequest request = new GroupLeaveRequest();
        request.setGroupId("group-1");
        request.setUserId("user-1");

        Response response = controller.leave(request);
        assertEquals(200, response.getStatus());
        verify(leavingUser).leaveGroup(group);
    }

    // --- setActiveGroup ---

    @Test
    void setActiveGroup_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);
        SetActiveGroupRequest request = new SetActiveGroupRequest();
        request.setActiveGroupId("group-1");

        Response response = controller.setActiveGroup(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void setActiveGroup_setsAttribute() {
        var controller = createAuthenticatedController();

        SetActiveGroupRequest request = new SetActiveGroupRequest();
        request.setActiveGroupId("group-1");

        Response response = controller.setActiveGroup(request);
        assertEquals(200, response.getStatus());
        verify(user).setAttribute("activeGroupId", List.of("group-1"));
    }

    // --- removeActiveGroup ---

    @Test
    void removeActiveGroup_removesAttribute() {
        var controller = createAuthenticatedController();

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);

        RemoveActiveGroupRequest request = new RemoveActiveGroupRequest();
        request.setUserId("user-1");

        Response response = controller.removeActiveGroup(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).removeAttribute("activeGroupId");
    }

    // --- userGroups ---

    @Test
    void userGroups_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);

        Response response = controller.userGroups();
        assertEquals(401, response.getStatus());
    }

    @Test
    void userGroups_returnsUserGroups() {
        var controller = createAuthenticatedController();

        GroupModel group = mock(GroupModel.class);
        when(group.getId()).thenReturn("group-1");
        when(group.getName()).thenReturn("Test Group");
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.of(group));

        Response response = controller.userGroups();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    // --- createGroup ---

    @Test
    void createGroup_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);
        CreateGroupRequest request = new CreateGroupRequest("Test", "/test");

        Response response = controller.createGroup(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void createGroup_throwsNotAuthorized_whenNotOperator() {
        var controller = createAuthenticatedController();
        setupRegularUser();

        CreateGroupRequest request = new CreateGroupRequest("Test", "/test");
        assertThrows(NotAuthorizedException.class, () -> controller.createGroup(request));
    }

    @Test
    void createGroup_returnsBadRequest_whenNameIsNull() {
        var controller = createAuthenticatedController();
        setupOperator();

        CreateGroupRequest request = new CreateGroupRequest();
        Response response = controller.createGroup(request);
        assertEquals(400, response.getStatus());
    }

    @Test
    void createGroup_returnsBadRequest_whenNameIsEmpty() {
        var controller = createAuthenticatedController();
        setupOperator();

        CreateGroupRequest request = new CreateGroupRequest("", "/test");
        Response response = controller.createGroup(request);
        assertEquals(400, response.getStatus());
    }

    @Test
    void createGroup_returnsConflict_whenGroupNameExists() {
        var controller = createAuthenticatedController();
        setupOperator();

        GroupModel existing = mock(GroupModel.class);
        when(existing.getName()).thenReturn("Existing Group");
        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.of(existing));

        CreateGroupRequest request = new CreateGroupRequest("Existing Group", null);
        Response response = controller.createGroup(request);
        assertEquals(409, response.getStatus());
    }

    @Test
    void createGroup_createsGroupAndJoinsCreator() {
        var controller = createAuthenticatedController();
        setupOperator();

        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.empty());

        GroupModel newGroup = mock(GroupModel.class);
        when(newGroup.getId()).thenReturn("new-group-id");
        when(newGroup.getName()).thenReturn("New Group");
        when(newGroup.getAttributes()).thenReturn(Map.of());
        when(groupProvider.createGroup(realm, "New Group")).thenReturn(newGroup);

        RoleModel groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), isNull(), isNull()))
                .thenAnswer(inv -> Stream.of(groupAdminRole));

        CreateGroupRequest request = new CreateGroupRequest("New Group", null);
        Response response = controller.createGroup(request);

        assertEquals(201, response.getStatus());
        verify(user).joinGroup(newGroup);
        verify(user).grantRole(groupAdminRole);
        verify(newGroup).setAttribute("path", List.of("/New Group"));
    }

    @Test
    void createGroup_usesProvidedPath() {
        var controller = createAuthenticatedController();
        setupOperator();

        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.empty());

        GroupModel newGroup = mock(GroupModel.class);
        when(newGroup.getId()).thenReturn("new-group-id");
        when(newGroup.getName()).thenReturn("New Group");
        when(newGroup.getAttributes()).thenReturn(Map.of());
        when(groupProvider.createGroup(realm, "New Group")).thenReturn(newGroup);

        RoleModel groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), isNull(), isNull()))
                .thenAnswer(inv -> Stream.of(groupAdminRole));

        CreateGroupRequest request = new CreateGroupRequest("New Group", "/custom/path");
        Response response = controller.createGroup(request);

        assertEquals(201, response.getStatus());
        verify(newGroup).setAttribute("path", List.of("/custom/path"));
    }
}
