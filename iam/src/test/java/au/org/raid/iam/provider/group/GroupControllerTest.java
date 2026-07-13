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

    // Uses the package-private test-seam constructor so the flat group-admin fallback flag can be
    // set directly instead of via the RAID_FLAT_GROUP_ADMIN_FALLBACK environment variable.
    private GroupController createAuthenticatedController(final boolean flatGroupAdminFallbackEnabled) {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(authResult))) {
            when(authResult.session()).thenReturn(userSession);
            when(userSession.getUser()).thenReturn(user);
            return new GroupController(session, flatGroupAdminFallbackEnabled);
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

    private void setupScopedServicePointAdminRole(final String groupId) {
        var scopedRole = mock(RoleModel.class);
        when(scopedRole.getName()).thenReturn("service-point-admin:" + groupId);
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(scopedRole));
    }

    private void setupGroupMembership(final String groupId) {
        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn(groupId);
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.of(group));
    }

    private void setupNoGroupMembership() {
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.empty());
    }

    private void setupServicePointUserRoleGrantTarget(final UserModel targetUser) {
        when(session.users()).thenReturn(userProvider);
        when(session.roles()).thenReturn(roleProvider);
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenAnswer(inv -> Stream.of(servicePointUserRole));
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

    // --- scoped group-admin authorization tests (RAID-720) ---

    @Test
    void grant_allowsScopedServicePointAdminOfGroup() {
        var controller = createAuthenticatedController();
        setupScopedServicePointAdminRole("g1");
        setupNoGroupMembership();

        var targetUser = mock(UserModel.class);
        setupServicePointUserRoleGrantTarget(targetUser);

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        var response = controller.grant(grant);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(any(RoleModel.class));
    }

    @Test
    void grant_deniesScopedServicePointAdminOfDifferentGroup() {
        var controller = createAuthenticatedController();
        setupScopedServicePointAdminRole("other-group");
        setupNoGroupMembership();

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        assertThrows(NotAuthorizedException.class, () -> controller.grant(grant));
    }

    @Test
    void grant_allowsFlatGroupAdminMemberWhenFallbackEnabled() {
        var controller = createAuthenticatedController(true);
        setupGroupAdminRole();
        setupGroupMembership("g1");

        var targetUser = mock(UserModel.class);
        setupServicePointUserRoleGrantTarget(targetUser);

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        var response = controller.grant(grant);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(any(RoleModel.class));
    }

    @Test
    void grant_deniesFlatGroupAdminNonMemberWhenFallbackEnabled() {
        var controller = createAuthenticatedController(true);
        setupGroupAdminRole();
        setupNoGroupMembership();

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        assertThrows(NotAuthorizedException.class, () -> controller.grant(grant));
    }

    @Test
    void grant_deniesFlatGroupAdminMemberWhenFallbackDisabled() {
        var controller = createAuthenticatedController(false);
        setupGroupAdminRole();
        setupGroupMembership("g1");

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        assertThrows(NotAuthorizedException.class, () -> controller.grant(grant));
    }

    @Test
    void grant_allowsScopedServicePointAdminWhenFallbackDisabled() {
        var controller = createAuthenticatedController(false);
        setupScopedServicePointAdminRole("g1");
        setupNoGroupMembership();

        var targetUser = mock(UserModel.class);
        setupServicePointUserRoleGrantTarget(targetUser);

        var grant = new Grant();
        grant.setUserId("u1");
        grant.setGroupId("g1");

        var response = controller.grant(grant);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(any(RoleModel.class));
    }

    @Test
    void get_allowsScopedServicePointAdminOfGroup() throws Exception {
        var controller = createAuthenticatedController();
        setupScopedServicePointAdminRole("g1");
        setupNoGroupMembership();
        when(session.groups()).thenReturn(groupProvider);
        when(session.users()).thenReturn(userProvider);

        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn("g1");
        when(group.getName()).thenReturn("Test Group");
        when(group.getAttributes()).thenReturn(Map.of());
        when(groupProvider.getGroupById(realm, "g1")).thenReturn(group);
        when(userProvider.getGroupMembersStream(realm, group)).thenReturn(Stream.empty());
        when(user.getId()).thenReturn("current-user");

        var response = controller.get("g1");
        assertThat(response.getStatus(), is(200));
    }

    @Test
    void get_deniesScopedServicePointAdminOfDifferentGroup() {
        var controller = createAuthenticatedController();
        setupScopedServicePointAdminRole("other-group");
        setupNoGroupMembership();

        assertThrows(NotAuthorizedException.class, () -> controller.get("g1"));
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

        when(realm.getRole("service-point-admin:new-id")).thenReturn(null);
        var servicePointAdminRole = mock(RoleModel.class);
        when(realm.addRole("service-point-admin:new-id"))
                .thenReturn(servicePointAdminRole);

        var request = new CreateGroupRequest("New Group", "/new-group");
        var response = controller.createGroup(request);
        assertThat(response.getStatus(), is(201));
        verify(user).joinGroup(newGroup);
        verify(user).grantRole(groupAdminRole);
        verify(user).grantRole(servicePointAdminRole);
        verify(servicePointAdminRole).setDescription("Service point admin for group new-id");
    }

    @Test
    void createGroup_grantsBothGroupAdminAndScopedServicePointAdminRoles() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(session.roles()).thenReturn(roleProvider);

        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.empty());

        var newGroup = mock(GroupModel.class);
        when(newGroup.getId()).thenReturn("new-group-id");
        when(newGroup.getName()).thenReturn("New Group");
        when(newGroup.getAttributes()).thenReturn(Map.of());
        when(groupProvider.createGroup(realm, "New Group")).thenReturn(newGroup);

        var groupAdminRole = mock(RoleModel.class);
        when(groupAdminRole.getName()).thenReturn("group-admin");
        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.of(groupAdminRole));

        when(realm.getRole("service-point-admin:new-group-id")).thenReturn(null);
        var servicePointAdminRole = mock(RoleModel.class);
        when(realm.addRole("service-point-admin:new-group-id"))
                .thenReturn(servicePointAdminRole);

        var request = new CreateGroupRequest("New Group", "/new-group");
        var response = controller.createGroup(request);

        assertThat(response.getStatus(), is(201));
        verify(user).grantRole(groupAdminRole);
        verify(user).grantRole(servicePointAdminRole);
    }

    @Test
    void createGroup_createsScopedServicePointAdminRoleWhenAbsent() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(session.roles()).thenReturn(roleProvider);

        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.empty());

        var newGroup = mock(GroupModel.class);
        when(newGroup.getId()).thenReturn("new-group-id");
        when(newGroup.getName()).thenReturn("New Group");
        when(newGroup.getAttributes()).thenReturn(Map.of());
        when(groupProvider.createGroup(realm, "New Group")).thenReturn(newGroup);

        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.empty());

        when(realm.getRole("service-point-admin:new-group-id")).thenReturn(null);
        var servicePointAdminRole = mock(RoleModel.class);
        when(realm.addRole("service-point-admin:new-group-id"))
                .thenReturn(servicePointAdminRole);

        var request = new CreateGroupRequest("New Group", "/new-group");
        var response = controller.createGroup(request);

        assertThat(response.getStatus(), is(201));
        // Verifies the single-arg addRole(name) overload is used - RoleContainerModel's two-arg
        // overload is addRole(id, name), not addRole(name, description), so calling it with the
        // role name and a description sentence would silently corrupt the role's id/name.
        verify(realm).addRole("service-point-admin:new-group-id");
        verify(realm, never()).addRole(anyString(), anyString());
        verify(servicePointAdminRole).setDescription("Service point admin for group new-group-id");
        verify(user).grantRole(servicePointAdminRole);
    }

    @Test
    void createGroup_reusesExistingScopedServicePointAdminRole() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(session.roles()).thenReturn(roleProvider);

        when(groupProvider.getGroupsStream(realm)).thenReturn(Stream.empty());

        var newGroup = mock(GroupModel.class);
        when(newGroup.getId()).thenReturn("new-group-id");
        when(newGroup.getName()).thenReturn("New Group");
        when(newGroup.getAttributes()).thenReturn(Map.of());
        when(groupProvider.createGroup(realm, "New Group")).thenReturn(newGroup);

        when(roleProvider.getRealmRolesStream(eq(realm), any(), any()))
                .thenReturn(Stream.empty());

        var existingServicePointAdminRole = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:new-group-id")).thenReturn(existingServicePointAdminRole);

        var request = new CreateGroupRequest("New Group", "/new-group");
        var response = controller.createGroup(request);

        assertThat(response.getStatus(), is(201));
        verify(realm, never()).addRole(anyString());
        verify(realm, never()).addRole(anyString(), anyString());
        verify(existingServicePointAdminRole, never()).setDescription(anyString());
        verify(user).grantRole(existingServicePointAdminRole);
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
    void grantGroupAdmin_grantsFlatAndScopedRoles() throws Exception {
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

        when(realm.getRole("service-point-admin:g1")).thenReturn(null);
        var scopedRole = mock(RoleModel.class);
        when(realm.addRole("service-point-admin:g1")).thenReturn(scopedRole);

        var request = new AddGroupAdminRequest();
        request.setUserId("u1");
        request.setGroupId("g1");

        var response = controller.grant(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(groupAdminRole);
        verify(targetUser).grantRole(scopedRole);
        verify(scopedRole).setDescription("Service point admin for group g1");
        verify(realm, never()).addRole(anyString(), anyString());
    }

    @Test
    void grantGroupAdmin_reusesExistingScopedRole() throws Exception {
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

        var scopedRole = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:g1")).thenReturn(scopedRole);

        var request = new AddGroupAdminRequest();
        request.setUserId("u1");
        request.setGroupId("g1");

        var response = controller.grant(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(groupAdminRole);
        verify(targetUser).grantRole(scopedRole);
        verify(realm, never()).addRole(anyString());
        verify(realm, never()).addRole(anyString(), anyString());
    }

    @Test
    void removeGroupAdmin_removesFlatAndScopedRoles() throws Exception {
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

        var scopedRole = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:g1")).thenReturn(scopedRole);

        var request = new RemoveGroupAdminRequest();
        request.setUserId("u1");
        request.setGroupId("g1");

        var response = controller.grant(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).deleteRoleMapping(groupAdminRole);
        verify(targetUser).deleteRoleMapping(scopedRole);
    }

    @Test
    void removeGroupAdmin_skipsScopedRoleWhenAbsent() throws Exception {
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

        when(realm.getRole("service-point-admin:g1")).thenReturn(null);

        var request = new RemoveGroupAdminRequest();
        request.setUserId("u1");
        request.setGroupId("g1");

        var response = controller.grant(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).deleteRoleMapping(groupAdminRole);
        verify(targetUser, times(1)).deleteRoleMapping(any(RoleModel.class));
    }
    // --- deleteGroup tests ---

    @Test
    void deleteGroup_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        try (var response = controller.deleteGroup("g1")) {
            assertThat(response.getStatus(), is(401));
        }
    }

    @Test
    void deleteGroup_throwsNotAuthorizedForNonOperator() {
        var controller = createAuthenticatedController();
        setupNoRoles();

        assertThrows(NotAuthorizedException.class, () -> controller.deleteGroup("g1"));
    }

    @Test
    void deleteGroup_throwsNotAuthorizedForGroupAdmin() {
        var controller = createAuthenticatedController();
        setupGroupAdminRole();

        assertThrows(NotAuthorizedException.class, () -> controller.deleteGroup("g1"));
    }

    @Test
    void deleteGroup_returnsBadRequestWhenGroupIdIsNull() {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        try (var response = controller.deleteGroup(null)) {
            assertThat(response.getStatus(), is(400));
        }
    }

    @Test
    void deleteGroup_returnsBadRequestWhenGroupIdIsEmpty() {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        try (var response = controller.deleteGroup("")) {
            assertThat(response.getStatus(), is(400));
        }
    }

    @Test
    void deleteGroup_returnsBadRequestWhenGroupIdIsBlank() {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        try (var response = controller.deleteGroup("   ")) {
            assertThat(response.getStatus(), is(400));
        }
    }

    @Test
    void deleteGroup_returnsNotFoundWhenGroupDoesNotExist() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);
        when(groupProvider.getGroupById(realm, "missing")).thenReturn(null);

        try (var response = controller.deleteGroup("missing")) {
            assertThat(response.getStatus(), is(404));
        }
    }

    @Test
    void deleteGroup_deletesGroupSuccessfully() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);

        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn("g1");
        when(group.getName()).thenReturn("Test Group");
        when(groupProvider.getGroupById(realm, "g1")).thenReturn(group);
        when(groupProvider.removeGroup(realm, group)).thenReturn(true);

        try (var response = controller.deleteGroup("g1")) {
            assertThat(response.getStatus(), is(200));
            verify(groupProvider).removeGroup(realm, group);
        }
    }

    @Test
    void deleteGroup_returnsServerErrorWhenRemoveGroupFails() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(session.groups()).thenReturn(groupProvider);

        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn("g1");
        when(group.getName()).thenReturn("Test Group");
        when(groupProvider.getGroupById(realm, "g1")).thenReturn(group);
        when(groupProvider.removeGroup(realm, group)).thenThrow(new RuntimeException("DB error"));

        try (var response = controller.deleteGroup("g1")) {
            assertThat(response.getStatus(), is(500));
        }
    }

    // --- deleteGroup preflight test ---

    @Test
    void deleteGroupPreflight_returns200() {
        var controller = createAuthenticatedController();
        try (var response = controller.deleteGroupPreflight()) {
            assertThat(response.getStatus(), is(200));
        }
    }

    // --- migrateServicePointAdmins tests (RAID-721) ---

    @Test
    void migrate_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();

        var response = controller.migrateServicePointAdmins();
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void migrate_throwsNotAuthorizedForNonOperator() {
        var controller = createAuthenticatedController();
        setupNoRoles();

        assertThrows(NotAuthorizedException.class, controller::migrateServicePointAdmins);
    }

    @Test
    void migrate_throwsNotAuthorizedForGroupAdmin() {
        var controller = createAuthenticatedController();
        setupGroupAdminRole();

        assertThrows(NotAuthorizedException.class, controller::migrateServicePointAdmins);
    }

    @Test
    void migrate_returnsZeroCountsWhenFlatRoleAbsent() {
        var controller = createAuthenticatedController();
        setupOperatorRole();
        when(realm.getRole("group-admin")).thenReturn(null);

        var response = controller.migrateServicePointAdmins();
        assertThat(response.getStatus(), is(200));

        var body = response.getEntity().toString();
        assertThat(body, containsString("\"flatGroupAdminUsers\":0"));
        assertThat(body, containsString("\"rolesCreated\":0"));
        assertThat(body, containsString("\"grantsAdded\":0"));
        assertThat(body, containsString("\"grantsSkipped\":0"));
        assertThat(body, containsString("Flat group-admin role not present; nothing to migrate"));

        verify(session, never()).users();
    }

    @Test
    void migrate_grantsScopedRoleForEachGroupOfFlatAdmin() {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        var flatGroupAdminRole = mock(RoleModel.class);
        when(realm.getRole("group-admin")).thenReturn(flatGroupAdminRole);

        when(session.users()).thenReturn(userProvider);

        var member = mock(UserModel.class);
        when(userProvider.getRoleMembersStream(eq(realm), eq(flatGroupAdminRole), anyInt(), anyInt()))
                .thenReturn(Stream.of(member), Stream.empty());

        var g1 = mock(GroupModel.class);
        when(g1.getId()).thenReturn("g1");
        var g2 = mock(GroupModel.class);
        when(g2.getId()).thenReturn("g2");
        when(member.getGroupsStream()).thenAnswer(inv -> Stream.of(g1, g2));

        when(realm.getRole("service-point-admin:g1")).thenReturn(null);
        var scopedRole1 = mock(RoleModel.class);
        when(realm.addRole("service-point-admin:g1")).thenReturn(scopedRole1);

        when(realm.getRole("service-point-admin:g2")).thenReturn(null);
        var scopedRole2 = mock(RoleModel.class);
        when(realm.addRole("service-point-admin:g2")).thenReturn(scopedRole2);

        when(member.hasDirectRole(scopedRole1)).thenReturn(false);
        when(member.hasDirectRole(scopedRole2)).thenReturn(false);

        var response = controller.migrateServicePointAdmins();
        assertThat(response.getStatus(), is(200));

        verify(member).grantRole(scopedRole1);
        verify(member).grantRole(scopedRole2);
        verify(realm).addRole("service-point-admin:g1");
        verify(realm).addRole("service-point-admin:g2");
        verify(realm, never()).addRole(anyString(), anyString());
        verify(scopedRole1).setDescription("Service point admin for group g1");
        verify(scopedRole2).setDescription("Service point admin for group g2");

        var body = response.getEntity().toString();
        assertThat(body, containsString("\"flatGroupAdminUsers\":1"));
        assertThat(body, containsString("\"rolesCreated\":2"));
        assertThat(body, containsString("\"grantsAdded\":2"));
        assertThat(body, containsString("\"grantsSkipped\":0"));
    }

    @Test
    void migrate_isIdempotentOnSecondRun() {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        var flatGroupAdminRole = mock(RoleModel.class);
        when(realm.getRole("group-admin")).thenReturn(flatGroupAdminRole);

        when(session.users()).thenReturn(userProvider);

        var member = mock(UserModel.class);
        when(userProvider.getRoleMembersStream(eq(realm), eq(flatGroupAdminRole), anyInt(), anyInt()))
                .thenReturn(Stream.of(member), Stream.empty());

        var g1 = mock(GroupModel.class);
        when(g1.getId()).thenReturn("g1");
        when(member.getGroupsStream()).thenAnswer(inv -> Stream.of(g1));

        var scopedRole1 = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:g1")).thenReturn(scopedRole1);
        when(member.hasDirectRole(scopedRole1)).thenReturn(true);

        var response = controller.migrateServicePointAdmins();
        assertThat(response.getStatus(), is(200));

        verify(member, never()).grantRole(any(RoleModel.class));
        verify(realm, never()).addRole(anyString());
        verify(realm, never()).addRole(anyString(), anyString());

        var body = response.getEntity().toString();
        assertThat(body, containsString("\"flatGroupAdminUsers\":1"));
        assertThat(body, containsString("\"rolesCreated\":0"));
        assertThat(body, containsString("\"grantsAdded\":0"));
        assertThat(body, containsString("\"grantsSkipped\":1"));
    }

    @Test
    void migrate_addsGrantWhenScopedRoleAlreadyExists() {
        var controller = createAuthenticatedController();
        setupOperatorRole();

        var flatGroupAdminRole = mock(RoleModel.class);
        when(realm.getRole("group-admin")).thenReturn(flatGroupAdminRole);

        when(session.users()).thenReturn(userProvider);

        var member = mock(UserModel.class);
        when(userProvider.getRoleMembersStream(eq(realm), eq(flatGroupAdminRole), anyInt(), anyInt()))
                .thenReturn(Stream.of(member), Stream.empty());

        var g1 = mock(GroupModel.class);
        when(g1.getId()).thenReturn("g1");
        when(member.getGroupsStream()).thenAnswer(inv -> Stream.of(g1));

        var scopedRole1 = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:g1")).thenReturn(scopedRole1);
        when(member.hasDirectRole(scopedRole1)).thenReturn(false);

        var response = controller.migrateServicePointAdmins();
        assertThat(response.getStatus(), is(200));

        verify(member).grantRole(scopedRole1);
        verify(realm, never()).addRole(anyString());
        verify(realm, never()).addRole(anyString(), anyString());

        var body = response.getEntity().toString();
        assertThat(body, containsString("\"flatGroupAdminUsers\":1"));
        assertThat(body, containsString("\"rolesCreated\":0"));
        assertThat(body, containsString("\"grantsAdded\":1"));
        assertThat(body, containsString("\"grantsSkipped\":0"));
    }

    @Test
    void migratePreflight_returns200() {
        var controller = createAuthenticatedController();
        var response = controller.migrateServicePointAdminsPreflight();
        assertThat(response.getStatus(), is(200));
    }
}
