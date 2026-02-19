package au.org.raid.iam.provider.group;

import au.org.raid.iam.provider.group.dto.*;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupControllerTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private UserModel user;
    @Mock private UserProvider userProvider;
    @Mock private GroupProvider groupProvider;
    @Mock private RoleProvider roleProvider;
    @Mock private ClientProvider clientProvider;
    @Mock private HttpHeaders headers;
    @Mock private AuthenticationManager.AuthResult authResult;
    @Mock private UserSessionModel userSession;

    @BeforeEach
    void setupSessionContext() {
        // The custom Cors class in GroupController needs session context for building responses
        when(session.getContext()).thenReturn(context);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        when(session.clients()).thenReturn(clientProvider);

        var client = mock(ClientModel.class);
        when(client.getWebOrigins()).thenReturn(Set.of("http://localhost:7080"));
        when(clientProvider.getClientsStream(realm)).thenAnswer(inv -> Stream.of(client));

        when(headers.getHeaderString("Origin")).thenReturn("http://localhost:7080");
    }

    private GroupController createAuthenticatedController() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(authResult))) {
            when(authResult.session()).thenReturn(userSession);
            when(userSession.getUser()).thenReturn(user);
            return new GroupController(session);
        }
    }

    private GroupController createUnauthenticatedController() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(null))) {
            return new GroupController(session);
        }
    }

    private void setupOperatorRole() {
        var operatorRole = mock(RoleModel.class);
        when(operatorRole.getName()).thenReturn("operator");
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(operatorRole));
    }

    private void setupGroupAdminRole() {
        var groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(groupAdminRole));
    }

    private void setupNoRoles() {
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.empty());
    }

    // --- getGroups tests ---

    @Test
    void getGroups_returnsUnauthorizedWhenNotAuthenticated() throws Exception {
        var controller = createUnauthenticatedController();
        var response = controller.getGroups();
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void getGroups_returnsGroupsForOperator() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);

        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn("g1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.of(group));

        var response = controller.getGroups();
        assertThat(response.getStatus(), is(200));
    }

    // --- get (group members) tests ---

    @Test
    void get_returnsUnauthorizedWhenNotAuthenticated() throws Exception {
        var controller = createUnauthenticatedController();
        var response = controller.get("g1");
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void get_returnsBadRequestWhenGroupIdNull() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        var response = controller.get(null);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    void get_returnsNotFoundWhenGroupDoesNotExist() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(groupProvider.getGroupById(realm, "missing")).thenReturn(null);

        var response = controller.get("missing");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    void get_throwsNotAuthorizedForNonAdminNonOperator() {
        var controller = createAuthenticatedController();
        setupNoRoles();

        assertThrows(NotAuthorizedException.class, () -> controller.get("g1"));
    }

    @Test
    void get_returnsGroupMembersForOperator() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(session.users()).thenReturn(userProvider);

        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn("g1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupById(realm, "g1")).thenReturn(group);

        var member = mock(UserModel.class);
        when(member.getId()).thenReturn("other-user");
        when(member.getAttributes()).thenReturn(Map.of());
        when(member.getRoleMappingsStream()).thenReturn(Stream.empty());
        when(userProvider.getGroupMembersStream(realm, group)).thenReturn(Stream.of(member));

        when(user.getId()).thenReturn("current-user");

        var response = controller.get("g1");
        assertThat(response.getStatus(), is(200));
    }

    // --- grant tests ---

    @Test
    void grant_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");
        var response = controller.grant(grant);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void grant_throwsNotAuthorizedForNonAdmin() {
        var controller = createAuthenticatedController();
        setupNoRoles();
        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        assertThrows(NotAuthorizedException.class, () -> controller.grant(grant));
    }

    @Test
    void grant_grantsServicePointUserRole() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.users()).thenReturn(userProvider);
        when(session.roles()).thenReturn(roleProvider);

        var targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.of(servicePointUserRole));

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        var response = controller.grant(grant);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(servicePointUserRole);
    }

    // --- revoke tests ---

    @Test
    void revoke_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");
        var response = controller.revoke(grant);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void revoke_deletesRoleMapping() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.users()).thenReturn(userProvider);
        when(session.roles()).thenReturn(roleProvider);

        var targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.of(servicePointUserRole));

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        var response = controller.revoke(grant);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).deleteRoleMapping(servicePointUserRole);
    }

    // --- join tests ---

    @Test
    void join_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new GroupJoinRequest();
        request.setGroupId("g1");
        var response = controller.join(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void join_addsUserToGroup() {
        var controller = createAuthenticatedController();
        when(session.groups()).thenReturn(groupProvider);

        var group = mock(GroupModel.class);
        when(groupProvider.getGroupById(realm, "g1")).thenReturn(group);

        var request = new GroupJoinRequest();
        request.setGroupId("g1");

        var response = controller.join(request);
        assertThat(response.getStatus(), is(200));
        verify(user).joinGroup(group);
    }

    // --- leave tests ---

    @Test
    void leave_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new GroupLeaveRequest();
        request.setGroupId("g1");
        request.setUserId("u1");
        var response = controller.leave(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void leave_removesUserFromGroup() {
        var controller = createAuthenticatedController();
        when(session.users()).thenReturn(userProvider);
        when(session.groups()).thenReturn(groupProvider);

        var targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var group = mock(GroupModel.class);
        when(groupProvider.getGroupById(realm, "g1")).thenReturn(group);

        var request = new GroupLeaveRequest();
        request.setGroupId("g1");
        request.setUserId("u1");

        var response = controller.leave(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).leaveGroup(group);
    }

    // --- setActiveGroup tests ---

    @Test
    void setActiveGroup_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new SetActiveGroupRequest();
        request.setActiveGroupId("g1");
        var response = controller.setActiveGroup(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void setActiveGroup_setsAttribute() {
        var controller = createAuthenticatedController();

        var request = new SetActiveGroupRequest();
        request.setActiveGroupId("g1");

        var response = controller.setActiveGroup(request);
        assertThat(response.getStatus(), is(200));
        verify(user).setAttribute("activeGroupId", List.of("g1"));
    }

    // --- removeActiveGroup tests ---

    @Test
    void removeActiveGroup_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new RemoveActiveGroupRequest();
        request.setUserId("u1");
        var response = controller.removeActiveGroup(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void removeActiveGroup_removesAttribute() {
        var controller = createAuthenticatedController();
        when(session.users()).thenReturn(userProvider);

        var targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var request = new RemoveActiveGroupRequest();
        request.setUserId("u1");

        var response = controller.removeActiveGroup(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).removeAttribute("activeGroupId");
    }

    // --- userGroups tests ---

    @Test
    void userGroups_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var response = controller.userGroups();
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void userGroups_returnsUserGroupList() {
        var controller = createAuthenticatedController();

        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn("g1");
        when(group.getName()).thenReturn("Test Group");
        when(user.getGroupsStream()).thenReturn(Stream.of(group));

        var response = controller.userGroups();
        assertThat(response.getStatus(), is(200));
    }

    // --- createGroup tests ---

    @Test
    void createGroup_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new CreateGroupRequest("New Group", "/new-group");
        var response = controller.createGroup(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void createGroup_throwsNotAuthorizedForNonOperator() {
        var controller = createAuthenticatedController();
        setupNoRoles();
        var request = new CreateGroupRequest("New Group", "/new-group");

        assertThrows(NotAuthorizedException.class, () -> controller.createGroup(request));
    }

    @Test
    void createGroup_returnsBadRequestForEmptyName() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        var request = new CreateGroupRequest("", "/empty");

        var response = controller.createGroup(request);
        assertThat(response.getStatus(), is(400));
    }

    @Test
    void createGroup_returnsConflictForDuplicateName() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);

        var existingGroup = mock(GroupModel.class);
        when(existingGroup.getName()).thenReturn("Existing");
        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.of(existingGroup));

        var request = new CreateGroupRequest("Existing", "/existing");
        var response = controller.createGroup(request);
        assertThat(response.getStatus(), is(409));
    }

    @Test
    void createGroup_createsGroupSuccessfully() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(session.roles()).thenReturn(roleProvider);

        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.empty());

        var newGroup = mock(GroupModel.class);
        when(newGroup.getId()).thenReturn("new-id");
        when(newGroup.getName()).thenReturn("New Group");
        when(newGroup.getAttributes()).thenReturn(Map.of());
        when(groupProvider.createGroup(realm, "New Group")).thenReturn(newGroup);

        var groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.of(groupAdminRole));

        var request = new CreateGroupRequest("New Group", "/new-group");
        var response = controller.createGroup(request);
        assertThat(response.getStatus(), is(201));
        verify(user).joinGroup(newGroup);
        verify(user).grantRole(groupAdminRole);
    }

    // --- preflight tests ---

    @Test
    void getGroupsPreflight_returns200() {
        var controller = createAuthenticatedController();
        var response = controller.getGroupsPreflight();
        assertThat(response.getStatus(), is(200));
    }

    // --- group-admin grant/revoke tests ---

    @Test
    void grantGroupAdmin_grantsRole() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.users()).thenReturn(userProvider);
        when(session.roles()).thenReturn(roleProvider);

        var targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.of(groupAdminRole));

        var request = new AddGroupAdminRequest();
        request.setUserId("u1");
        request.setGroupId("g1");

        var response = controller.grant(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(groupAdminRole);
    }

    @Test
    void removeGroupAdmin_removesRole() throws Exception {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.users()).thenReturn(userProvider);
        when(session.roles()).thenReturn(roleProvider);

        var targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.of(groupAdminRole));

        var request = new RemoveGroupAdminRequest();
        request.setUserId("u1");
        request.setGroupId("g1");

        var response = controller.grant(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).deleteRoleMapping(groupAdminRole);
    }
}
