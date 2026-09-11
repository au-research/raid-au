package au.org.raid.iam.provider.group;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RAID-884: the scoped service-point-admin roles must be provisioned unattended at boot rather
 * than by an operator calling the migration endpoint by hand.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicePointAdminRoleBootstrapperTest {

    @Mock private KeycloakSessionFactory sessionFactory;
    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmProvider realmProvider;
    @Mock private UserProvider userProvider;

    private ProviderEventListener registerAndCaptureListener() {
        ServicePointAdminRoleBootstrapper.register(sessionFactory);
        var captor = ArgumentCaptor.forClass(ProviderEventListener.class);
        verify(sessionFactory).register(captor.capture());
        return captor.getValue();
    }

    /**
     * Stands in for Keycloak's transaction plumbing: every runJobInTransaction call is executed
     * immediately against the mock session, so the test exercises the real backfill logic.
     */
    private static void runTasksInline(org.mockito.MockedStatic<KeycloakModelUtils> mocked,
                                       KeycloakSession session) {
        mocked.when(() -> KeycloakModelUtils.runJobInTransaction(any(), any()))
                .thenAnswer(invocation -> {
                    KeycloakSessionTask task = invocation.getArgument(1);
                    task.run(session);
                    return null;
                });
    }

    private RealmModel realmWithFlatAdmins(final String id, final String name,
                                           final List<UserModel> flatAdmins) {
        var realm = mock(RealmModel.class);
        when(realm.getId()).thenReturn(id);
        when(realm.getName()).thenReturn(name);

        var flatRole = mock(RoleModel.class);
        when(realm.getRole(ServicePointAdminRoleProvisioner.GROUP_ADMIN_ROLE_NAME)).thenReturn(flatRole);
        when(userProvider.getRoleMembersStream(eq(realm), eq(flatRole), anyInt(), anyInt()))
                .thenAnswer(inv -> (int) inv.getArgument(2) == 0 ? flatAdmins.stream() : Stream.empty());

        return realm;
    }

    private UserModel approvedAdminOf(final GroupModel... groups) {
        var user = mock(UserModel.class);
        var servicePointUser = mock(RoleModel.class);
        when(servicePointUser.getName()).thenReturn(ServicePointAdminRoleProvisioner.SERVICE_POINT_USER_ROLE);
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(servicePointUser));
        when(user.getGroupsStream()).thenAnswer(inv -> Stream.of(groups));
        when(user.hasDirectRole(any())).thenReturn(false);
        return user;
    }

    private static GroupModel group(final String id) {
        var group = mock(GroupModel.class);
        when(group.getId()).thenReturn(id);
        return group;
    }

    @Test
    void ignoresEventsOtherThanPostMigration() {
        var listener = registerAndCaptureListener();

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            listener.onEvent(mock(ProviderEvent.class));

            mocked.verifyNoInteractions();
        }
    }

    @Test
    void onPostMigration_grantsScopedRoleToFlatAdminsOfEveryRealm() {
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);

        var groupA = group("group-a");
        var admin = approvedAdminOf(groupA);
        var realm = realmWithFlatAdmins("realm-1", "raid", List.of(admin));

        var createdRole = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:group-a")).thenReturn(null);
        when(realm.addRole("service-point-admin:group-a")).thenReturn(createdRole);

        when(realmProvider.getRealmsStream()).thenAnswer(inv -> Stream.of(realm));
        when(realmProvider.getRealm("realm-1")).thenReturn(realm);

        var listener = registerAndCaptureListener();

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            runTasksInline(mocked, session);

            listener.onEvent(new PostMigrationEvent(sessionFactory));
        }

        verify(realm).addRole("service-point-admin:group-a");
        verify(admin).grantRole(createdRole);
        verify(context).setRealm(realm);
    }

    @Test
    void onPostMigration_isNoOpForRealmWithoutFlatGroupAdminRole() {
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);

        // e.g. the master realm, or any realm an agency runs alongside RAiD
        var realm = mock(RealmModel.class);
        when(realm.getId()).thenReturn("realm-master");
        when(realm.getName()).thenReturn("master");
        when(realm.getRole(anyString())).thenReturn(null);

        when(realmProvider.getRealmsStream()).thenAnswer(inv -> Stream.of(realm));
        when(realmProvider.getRealm("realm-master")).thenReturn(realm);

        var listener = registerAndCaptureListener();

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            runTasksInline(mocked, session);

            listener.onEvent(new PostMigrationEvent(sessionFactory));
        }

        verify(realm, never()).addRole(anyString());
        verifyNoInteractions(userProvider);
    }

    @Test
    void onPostMigration_isIdempotentWhenScopedRoleAlreadyGranted() {
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);

        var groupA = group("group-a");
        var admin = approvedAdminOf(groupA);
        var realm = realmWithFlatAdmins("realm-1", "raid", List.of(admin));

        var existingRole = mock(RoleModel.class);
        when(realm.getRole("service-point-admin:group-a")).thenReturn(existingRole);
        when(admin.hasDirectRole(existingRole)).thenReturn(true);

        when(realmProvider.getRealmsStream()).thenAnswer(inv -> Stream.of(realm));
        when(realmProvider.getRealm("realm-1")).thenReturn(realm);

        var listener = registerAndCaptureListener();

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            runTasksInline(mocked, session);

            listener.onEvent(new PostMigrationEvent(sessionFactory));
        }

        verify(realm, never()).addRole(anyString());
        verify(admin, never()).grantRole(any());
    }

    /**
     * A realm that cannot be backfilled must not stop Keycloak booting, and must not stop the
     * remaining realms being backfilled.
     */
    @Test
    void onPostMigration_oneFailingRealmDoesNotStopTheOthersOrPropagate() {
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(session.users()).thenReturn(userProvider);

        var failingRealm = mock(RealmModel.class);
        when(failingRealm.getId()).thenReturn("realm-bad");
        when(failingRealm.getRole(anyString())).thenThrow(new RuntimeException("boom"));

        var groupA = group("group-a");
        var admin = approvedAdminOf(groupA);
        var healthyRealm = realmWithFlatAdmins("realm-good", "raid", List.of(admin));
        var createdRole = mock(RoleModel.class);
        when(healthyRealm.getRole("service-point-admin:group-a")).thenReturn(null);
        when(healthyRealm.addRole("service-point-admin:group-a")).thenReturn(createdRole);

        when(realmProvider.getRealmsStream()).thenAnswer(inv -> Stream.of(failingRealm, healthyRealm));
        when(realmProvider.getRealm("realm-bad")).thenReturn(failingRealm);
        when(realmProvider.getRealm("realm-good")).thenReturn(healthyRealm);

        var listener = registerAndCaptureListener();

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            runTasksInline(mocked, session);

            listener.onEvent(new PostMigrationEvent(sessionFactory));
        }

        verify(admin).grantRole(createdRole);
    }

    @Test
    void onPostMigration_failureListingRealmsDoesNotPropagate() {
        when(session.realms()).thenReturn(realmProvider);
        when(realmProvider.getRealmsStream()).thenThrow(new RuntimeException("db down"));

        var listener = registerAndCaptureListener();

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            runTasksInline(mocked, session);

            // Must not propagate: a thrown exception here would abort Keycloak startup.
            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));

            // The realm listing is the only transaction attempted; no per-realm work follows.
            mocked.verify(() -> KeycloakModelUtils.runJobInTransaction(any(), any()), times(1));
        }
    }
}
