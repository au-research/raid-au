package au.org.raid.iam.provider.raid;

import au.org.raid.iam.provider.exception.UserNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RaidPermissionsControllerTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private UserModel user;
    @Mock private UserProvider userProvider;
    @Mock private AuthenticationManager.AuthResult authResult;
    @Mock private UserSessionModel userSession;
    @Mock private ClientModel client;
    @Mock private Cors keycloakCors;

    @BeforeEach
    void setupSession() {
        lenient().when(session.getContext()).thenReturn(context);
        lenient().when(context.getRealm()).thenReturn(realm);
        lenient().when(session.getProvider(Cors.class)).thenReturn(keycloakCors);
        lenient().when(keycloakCors.allowedOrigins(any(String[].class))).thenReturn(keycloakCors);
        lenient().when(keycloakCors.allowedMethods(any(String[].class))).thenReturn(keycloakCors);
        lenient().when(keycloakCors.auth()).thenReturn(keycloakCors);
        lenient().when(keycloakCors.preflight()).thenReturn(keycloakCors);
        lenient().when(keycloakCors.add(any(Response.ResponseBuilder.class)))
                .thenAnswer(inv -> ((Response.ResponseBuilder) inv.getArgument(0)).build());
    }

    private RaidPermissionsController createAuthenticatedController() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(authResult))) {
            when(authResult.session()).thenReturn(userSession);
            when(authResult.client()).thenReturn(client);
            return new RaidPermissionsController(session);
        }
    }

    private RaidPermissionsController createUnauthenticatedController() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(null))) {
            return new RaidPermissionsController(session);
        }
    }

    private void setupRaidPermissionsAdminRole() {
        var role = mock(RoleModel.class);
        when(role.getName()).thenReturn("raid-permissions-admin");
        when(client.getRolesStream()).thenReturn(Stream.of(role));
    }

    private void setupNoClientRoles() {
        when(client.getRolesStream()).thenReturn(Stream.empty());
    }

    // --- addRaidUserPreflight ---

    @Test
    void addRaidUserPreflight_returnsOk() {
        var controller = createAuthenticatedController();
        var response = controller.addRaidUserPreflight();
        assertThat(response.getStatus(), is(200));
        verify(keycloakCors).preflight();
    }

    // --- addRaidUser ---

    @Test
    void addRaidUser_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.addRaidUser(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void addRaidUser_returnsUnauthorizedWithoutAdminRole() {
        var controller = createAuthenticatedController();
        setupNoClientRoles();
        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.addRaidUser(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void addRaidUser_addsUserToRaid() {
        var controller = createAuthenticatedController();
        setupRaidPermissionsAdminRole();
        when(session.users()).thenReturn(userProvider);

        var targetUser = mock(UserModel.class);
        when(targetUser.getAttributeStream("userRaids")).thenReturn(Stream.empty());
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var raidUserRole = mock(RoleModel.class);
        when(realm.getRole("raid-user")).thenReturn(raidUserRole);

        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");

        var response = controller.addRaidUser(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).setAttribute(eq("userRaids"), any());
        verify(targetUser).grantRole(raidUserRole);
    }

    @Test
    void addRaidUser_throwsUserNotFoundWhenUserDoesNotExist() {
        var controller = createAuthenticatedController();
        setupRaidPermissionsAdminRole();
        when(session.users()).thenReturn(userProvider);
        when(userProvider.getUserById(realm, "missing")).thenReturn(null);

        var request = new RaidUserPermissionsRequest();
        request.setUserId("missing");
        request.setHandle("test-handle");

        assertThrows(UserNotFoundException.class, () -> controller.addRaidUser(request));
    }

    // --- removeRaidUser ---

    @Test
    void removeRaidUser_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.removeRaidUser(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void removeRaidUser_removesHandleFromUser() {
        var controller = createAuthenticatedController();
        setupRaidPermissionsAdminRole();
        when(session.users()).thenReturn(userProvider);

        var targetUser = mock(UserModel.class);
        when(targetUser.getAttributeStream("userRaids")).thenReturn(Stream.of("test-handle", "other-handle"));
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");

        var response = controller.removeRaidUser(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).setAttribute(eq("userRaids"), any());
    }

    // --- addRaidAdmin ---

    @Test
    void addRaidAdmin_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.addRaidAdmin(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void addRaidAdmin_grantsRaidAdminRole() {
        var controller = createAuthenticatedController();
        var servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(userSession.getUser()).thenReturn(user);
        when(user.getRoleMappingsStream()).thenReturn(Stream.of(servicePointUserRole));
        when(userSession.getRealm()).thenReturn(realm);

        var raidAdminRole = mock(RoleModel.class);
        when(realm.getRole("raid-admin")).thenReturn(raidAdminRole);

        var targetUser = mock(UserModel.class);
        when(session.users()).thenReturn(userProvider);
        when(userProvider.getUserByUsername(realm, "u1")).thenReturn(targetUser);

        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");

        var response = controller.addRaidAdmin(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).grantRole(raidAdminRole);
    }

    @Test
    void addRaidAdmin_returnsUnauthorizedWithoutServicePointUserRole() {
        var controller = createAuthenticatedController();
        when(userSession.getUser()).thenReturn(user);
        when(user.getRoleMappingsStream()).thenReturn(Stream.empty());

        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");

        var response = controller.addRaidAdmin(request);
        assertThat(response.getStatus(), is(401));
    }

    // --- removeRaidAdmin ---

    @Test
    void removeRaidAdmin_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.removeRaidAdmin(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void removeRaidAdmin_removesRaidAdminRole() {
        var controller = createAuthenticatedController();
        var servicePointUserRole = mock(RoleModel.class);
        when(servicePointUserRole.getName()).thenReturn("service-point-user");
        when(userSession.getUser()).thenReturn(user);
        when(user.getRoleMappingsStream()).thenReturn(Stream.of(servicePointUserRole));
        when(userSession.getRealm()).thenReturn(realm);

        var raidAdminRole = mock(RoleModel.class);
        when(realm.getRole("raid-admin")).thenReturn(raidAdminRole);

        var targetUser = mock(UserModel.class);
        when(session.users()).thenReturn(userProvider);
        when(userProvider.getUserByUsername(realm, "u1")).thenReturn(targetUser);

        var request = new RaidUserPermissionsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");

        var response = controller.removeRaidAdmin(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).deleteRoleMapping(raidAdminRole);
    }

    // --- addAdminRaid ---

    @Test
    void addAdminRaid_returnsUnauthorizedWhenNotAuthenticated() {
        var controller = createUnauthenticatedController();
        var request = new AdminRaidsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.addAdminRaid(request);
        assertThat(response.getStatus(), is(401));
    }

    @Test
    void addAdminRaid_addsHandleToAdminRaids() {
        var controller = createAuthenticatedController();
        setupRaidPermissionsAdminRole();
        when(session.users()).thenReturn(userProvider);

        var targetUser = mock(UserModel.class);
        when(targetUser.getAttributeStream("adminRaids")).thenReturn(Stream.empty());
        when(userProvider.getUserById(realm, "u1")).thenReturn(targetUser);

        var request = new AdminRaidsRequest();
        request.setUserId("u1");
        request.setHandle("new-admin-handle");

        var response = controller.addAdminRaid(request);
        assertThat(response.getStatus(), is(200));
        verify(targetUser).setAttribute(eq("adminRaids"), any());
    }

    @Test
    void addAdminRaid_returnsUnauthorizedWithoutAdminRole() {
        var controller = createAuthenticatedController();
        setupNoClientRoles();
        var request = new AdminRaidsRequest();
        request.setUserId("u1");
        request.setHandle("test-handle");
        var response = controller.addAdminRaid(request);
        assertThat(response.getStatus(), is(401));
    }
}
