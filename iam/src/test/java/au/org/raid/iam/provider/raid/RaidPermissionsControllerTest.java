package au.org.raid.iam.provider.raid;

import au.org.raid.iam.provider.exception.UserNotFoundException;
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

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RaidPermissionsControllerTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext keycloakContext;
    @Mock private RealmModel realm;
    @Mock private UserProvider userProvider;
    @Mock private org.keycloak.services.cors.Cors keycloakCors;
    @Mock private AuthenticationManager.AuthResult authResult;
    @Mock private UserSessionModel userSession;
    @Mock private UserModel currentUser;
    @Mock private ClientModel client;

    private void setupSession() {
        when(session.getContext()).thenReturn(keycloakContext);
        when(keycloakContext.getRealm()).thenReturn(realm);
        when(session.users()).thenReturn(userProvider);
        when(session.getProvider(org.keycloak.services.cors.Cors.class)).thenReturn(keycloakCors);
        when(keycloakCors.allowedOrigins(ArgumentMatchers.<String>any())).thenReturn(keycloakCors);
        when(keycloakCors.allowedMethods(ArgumentMatchers.<String>any())).thenReturn(keycloakCors);
        when(keycloakCors.auth()).thenReturn(keycloakCors);
        when(keycloakCors.preflight()).thenReturn(keycloakCors);
        when(keycloakCors.add(any(Response.ResponseBuilder.class)))
                .thenAnswer(inv -> ((Response.ResponseBuilder) inv.getArgument(0)).build());
    }

    private RaidPermissionsController createController(AuthenticationManager.AuthResult auth) {
        setupSession();
        try (var ignored = mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                (mock, ctx) -> when(mock.authenticate()).thenReturn(auth))) {
            return new RaidPermissionsController(session);
        }
    }

    private RaidPermissionsController createAuthenticatedController() {
        when(authResult.getSession()).thenReturn(userSession);
        when(userSession.getUser()).thenReturn(currentUser);
        when(userSession.getRealm()).thenReturn(realm);
        when(authResult.getClient()).thenReturn(client);
        return createController(authResult);
    }

    private void setupClientWithRole(String roleName) {
        RoleModel role = mock(RoleModel.class);
        when(role.getName()).thenReturn(roleName);
        when(client.getRolesStream()).thenAnswer(inv -> Stream.of(role));
    }

    private void setupClientWithoutRole() {
        when(client.getRolesStream()).thenAnswer(inv -> Stream.empty());
    }

    // --- addRaidUser ---

    @Test
    void addRaidUser_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.addRaidUser(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void addRaidUser_returnsUnauthorized_whenClientLacksRole() {
        var controller = createAuthenticatedController();
        setupClientWithoutRole();

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.addRaidUser(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void addRaidUser_addsUserToRaid() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);
        when(targetUser.getAttributeStream("userRaids")).thenAnswer(inv -> Stream.empty());

        RoleModel raidUserRole = mock(RoleModel.class);
        when(realm.getRole("raid-user")).thenReturn(raidUserRole);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.addRaidUser(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).setAttribute(eq("userRaids"), argThat(list -> list.contains("https://raid.org/123")));
        verify(targetUser).grantRole(raidUserRole);
    }

    @Test
    void addRaidUser_preservesExistingRaids() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);
        when(targetUser.getAttributeStream("userRaids"))
                .thenAnswer(inv -> Stream.of("https://raid.org/existing"));

        RoleModel raidUserRole = mock(RoleModel.class);
        when(realm.getRole("raid-user")).thenReturn(raidUserRole);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/new");

        Response response = controller.addRaidUser(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).setAttribute(eq("userRaids"), argThat(list ->
                list.contains("https://raid.org/existing") && list.contains("https://raid.org/new")));
    }

    @Test
    void addRaidUser_throwsWhenUserNotFound() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        when(userProvider.getUserById(realm, "nonexistent")).thenReturn(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("nonexistent");
        request.setHandle("https://raid.org/123");

        assertThrows(UserNotFoundException.class, () -> controller.addRaidUser(request));
    }

    @Test
    void addRaidUser_throwsWhenRaidUserRoleNotFound() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);
        when(targetUser.getAttributeStream("userRaids")).thenAnswer(inv -> Stream.empty());
        when(realm.getRole("raid-user")).thenReturn(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        assertThrows(IllegalStateException.class, () -> controller.addRaidUser(request));
    }

    // --- removeRaidUser ---

    @Test
    void removeRaidUser_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.removeRaidUser(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void removeRaidUser_removesHandleFromUserRaids() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);
        when(targetUser.getAttributeStream("userRaids"))
                .thenAnswer(inv -> Stream.of("https://raid.org/123", "https://raid.org/other"));

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.removeRaidUser(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).setAttribute(eq("userRaids"), argThat(list ->
                !list.contains("https://raid.org/123") && list.contains("https://raid.org/other")));
    }

    // --- addRaidAdmin ---

    @Test
    void addRaidAdmin_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("testuser");
        request.setHandle("handle");

        Response response = controller.addRaidAdmin(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void addRaidAdmin_returnsUnauthorized_whenNotServicePointUser() {
        var controller = createAuthenticatedController();
        when(currentUser.getRoleMappingsStream()).thenAnswer(inv -> Stream.empty());

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("testuser");

        Response response = controller.addRaidAdmin(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void addRaidAdmin_grantsRaidAdminRole() {
        var controller = createAuthenticatedController();

        RoleModel servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(currentUser.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(servicePointUserRole));

        RoleModel raidAdminRole = mock(RoleModel.class);
        when(realm.getRole("raid-admin")).thenReturn(raidAdminRole);

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserByUsername(realm, "testuser")).thenReturn(targetUser);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("testuser");

        Response response = controller.addRaidAdmin(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).grantRole(raidAdminRole);
    }

    @Test
    void addRaidAdmin_throwsWhenRoleNotFound() {
        var controller = createAuthenticatedController();

        RoleModel servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(currentUser.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(servicePointUserRole));

        when(realm.getRole("raid-admin")).thenReturn(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("testuser");

        assertThrows(IllegalStateException.class, () -> controller.addRaidAdmin(request));
    }

    // --- removeRaidAdmin ---

    @Test
    void removeRaidAdmin_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("testuser");

        Response response = controller.removeRaidAdmin(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void removeRaidAdmin_revokesRaidAdminRole() {
        var controller = createAuthenticatedController();

        RoleModel servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(currentUser.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(servicePointUserRole));

        RoleModel raidAdminRole = mock(RoleModel.class);
        when(realm.getRole("raid-admin")).thenReturn(raidAdminRole);

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserByUsername(realm, "testuser")).thenReturn(targetUser);

        RaidUserPermissionsRequest request = new RaidUserPermissionsRequest();
        request.setUserId("testuser");

        Response response = controller.removeRaidAdmin(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).deleteRoleMapping(raidAdminRole);
    }

    // --- addAdminRaid ---

    @Test
    void addAdminRaid_returnsUnauthorized_whenAuthIsNull() {
        var controller = createController(null);

        AdminRaidsRequest request = new AdminRaidsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.addAdminRaid(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void addAdminRaid_returnsUnauthorized_whenClientLacksRole() {
        var controller = createAuthenticatedController();
        setupClientWithoutRole();

        AdminRaidsRequest request = new AdminRaidsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.addAdminRaid(request);
        assertEquals(401, response.getStatus());
    }

    @Test
    void addAdminRaid_addsHandleToAdminRaids() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);
        when(targetUser.getAttributeStream("adminRaids")).thenAnswer(inv -> Stream.empty());

        AdminRaidsRequest request = new AdminRaidsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/123");

        Response response = controller.addAdminRaid(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).setAttribute(eq("adminRaids"), argThat(list -> list.contains("https://raid.org/123")));
    }

    @Test
    void addAdminRaid_preservesExistingAdminRaids() {
        var controller = createAuthenticatedController();
        setupClientWithRole("raid-permissions-admin");

        UserModel targetUser = mock(UserModel.class);
        when(userProvider.getUserById(realm, "user-1")).thenReturn(targetUser);
        when(targetUser.getAttributeStream("adminRaids"))
                .thenAnswer(inv -> Stream.of("https://raid.org/existing"));

        AdminRaidsRequest request = new AdminRaidsRequest();
        request.setUserId("user-1");
        request.setHandle("https://raid.org/new");

        Response response = controller.addAdminRaid(request);
        assertEquals(200, response.getStatus());
        verify(targetUser).setAttribute(eq("adminRaids"), argThat(list ->
                list.contains("https://raid.org/existing") && list.contains("https://raid.org/new")));
    }
}
