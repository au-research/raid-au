package au.org.raid.inttest;

import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.dto.keycloak.Grant;
import au.org.raid.inttest.dto.keycloak.Group;
import au.org.raid.inttest.dto.keycloak.GroupJoinRequest;
import feign.FeignException;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * RAID-724: covers per-service-point authorization on the Keycloak group SPI - the scoped
 * "service-point-admin:&lt;groupId&gt;" role, the flat group-admin fallback, operator bypass,
 * admin access on newly created groups, and the /group/migrate-service-point-admins backfill.
 */
@DisplayName("Group Service Point Admin Integration Tests")
class GroupServicePointAdminIntegrationTest extends AbstractIntegrationTest {

    private static String servicePointAdminRoleName(final String groupId) {
        return "service-point-admin:" + groupId;
    }

    private UserContext createOperator() {
        return userService.createUser("raid-au", "operator");
    }

    /**
     * Creates a group via the "/realms/raid/group/create" SPI endpoint (as GroupDeleteIntegrationTest
     * does), then looks it up by name via allGroups() to recover its id. The creating operator
     * automatically becomes a member, flat group-admin, and scoped service-point-admin of the
     * new group as a side effect of the SPI endpoint - irrelevant here since operators bypass all
     * authorization checks regardless.
     */
    private Group createGroup(final UserContext operator, final String namePrefix) {
        final var api = keycloakClient.keycloakApi(operator.getToken());
        final var groupName = namePrefix + "-" + UUID.randomUUID();

        api.createGroupViaSpi(Map.of("name", groupName, "path", "/groups/" + groupName));

        final var groups = api.allGroups().getBody();
        assertThat(groups).isNotNull();

        return groups.getGroups().stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test group was not created: " + groupName));
    }

    private void deleteGroupQuietly(final UserContext operator, final String groupId) {
        // Both null-checked independently: if @BeforeEach failed before either was assigned,
        // cleanup of this particular resource is simply skipped rather than NPEing and masking
        // the original failure (see GroupDeleteIntegrationTest's testGroupId guard for the same
        // pattern).
        if (operator == null || groupId == null) {
            return;
        }
        try {
            keycloakClient.keycloakApi(operator.getToken()).deleteGroup(groupId);
        } catch (Exception e) {
            // Group may already be deleted, ignore.
        }
    }

    /** Null-safe id accessor so tearDown methods can pass a possibly-unset Group field straight
     * through to deleteGroupQuietly without NPEing on {@code .getId()}. */
    private static String idOf(final Group group) {
        return group == null ? null : group.getId();
    }

    /** Null-safe user cleanup - UserService.deleteUser already swallows Keycloak-side failures
     * internally, so the only NPE risk here is a null UserContext (e.g. @BeforeEach failed
     * before this field was assigned), which this guards against. */
    private void deleteUserQuietly(final UserContext user) {
        if (user != null) {
            userService.deleteUser(user.getId());
        }
    }

    private Grant grant(final String userId, final String groupId) {
        final var g = new Grant();
        g.setUserId(userId);
        g.setGroupId(groupId);
        return g;
    }

    /**
     * Makes the given user a real Keycloak member of the group (distinct from the "activeGroupId"
     * attribute set by UserService.createUser's groupName argument) - required for the flat
     * group-admin fallback's membership check, and for a user to appear in the group's member
     * listing at all.
     */
    private void joinGroup(final UserContext user, final String groupId) {
        final var request = new GroupJoinRequest();
        request.setGroupId(groupId);
        keycloakClient.keycloakApi(user.getToken()).joinGroup(request);
    }

    private void assertDenied(final Runnable action) {
        try {
            action.run();
            fail("Expected 401/403");
        } catch (FeignException e) {
            assertThat(e.status()).isIn(401, 403);
        }
    }

    @Nested
    @DisplayName("Scoped service-point-admin role")
    class ScopedRoleAdminTests {

        private UserContext operator;
        private UserContext scopedAdmin;
        private UserContext targetUser;
        private Group groupA;
        private Group groupB;

        @BeforeEach
        void setUp() {
            operator = createOperator();
            groupA = createGroup(operator, "scoped-a");
            groupB = createGroup(operator, "scoped-b");

            // Holds only the scoped role for groupA - not a flat group-admin, not a member of
            // either group.
            scopedAdmin = userService.createUser("raid-au", servicePointAdminRoleName(groupA.getId()));

            targetUser = userService.createUser("raid-au", "service-point-user");
            joinGroup(targetUser, groupA.getId());
        }

        @AfterEach
        void tearDown() {
            // Each field is null-checked independently (via the deleteXQuietly helpers) so that
            // a partial @BeforeEach failure still cleans up whatever WAS created, instead of
            // NPEing on the first unset field and burying the real setup failure.
            deleteUserQuietly(scopedAdmin);
            deleteUserQuietly(targetUser);
            deleteGroupQuietly(operator, idOf(groupA));
            deleteGroupQuietly(operator, idOf(groupB));
            deleteUserQuietly(operator);
        }

        @Test
        @DisplayName("Scoped admin can list members and grant/revoke service-point-user in their own group")
        void scopedAdminCanAdministerOwnGroup() {
            final var api = keycloakClient.keycloakApi(scopedAdmin.getToken());

            final var group = api.findById(groupA.getId()).getBody();
            assertThat(group).isNotNull();
            assertThat(group.getId()).isEqualTo(groupA.getId());

            api.revoke(grant(targetUser.getId(), groupA.getId()));
            final var afterRevoke = api.findById(groupA.getId()).getBody();
            assertThat(afterRevoke).isNotNull();
            final var memberAfterRevoke = afterRevoke.getMembers().stream()
                    .filter(m -> m.getId().equals(targetUser.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(memberAfterRevoke.getRoles()).doesNotContain("service-point-user");

            api.grant(grant(targetUser.getId(), groupA.getId()));
            final var afterGrant = api.findById(groupA.getId()).getBody();
            assertThat(afterGrant).isNotNull();
            final var memberAfterGrant = afterGrant.getMembers().stream()
                    .filter(m -> m.getId().equals(targetUser.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(memberAfterGrant.getRoles()).contains("service-point-user");
        }

        @Test
        @DisplayName("Scoped admin of groupA is denied access to groupB")
        void scopedAdminDeniedForOtherGroup() {
            final var api = keycloakClient.keycloakApi(scopedAdmin.getToken());

            assertDenied(() -> api.findById(groupB.getId()));
            assertDenied(() -> api.grant(grant(targetUser.getId(), groupB.getId())));
            assertDenied(() -> api.revoke(grant(targetUser.getId(), groupB.getId())));
        }
    }

    @Nested
    @DisplayName("Flat group-admin fallback")
    class FlatGroupAdminFallbackTests {

        private UserContext operator;
        private UserContext flatAdminMemberOfA;
        private UserContext flatAdminNotMemberOfB;
        private UserContext targetUser;
        private Group groupA;
        private Group groupB;

        @BeforeEach
        void setUp() {
            operator = createOperator();
            groupA = createGroup(operator, "flat-a");
            groupB = createGroup(operator, "flat-b");

            flatAdminMemberOfA = userService.createUser("raid-au", "group-admin");
            joinGroup(flatAdminMemberOfA, groupA.getId());

            // Flat group-admin, but never joins groupA or groupB - fallback requires both the
            // flat role AND membership.
            flatAdminNotMemberOfB = userService.createUser("raid-au", "group-admin");

            targetUser = userService.createUser("raid-au", "service-point-user");
            joinGroup(targetUser, groupA.getId());
        }

        @AfterEach
        void tearDown() {
            deleteUserQuietly(flatAdminMemberOfA);
            deleteUserQuietly(flatAdminNotMemberOfB);
            deleteUserQuietly(targetUser);
            deleteGroupQuietly(operator, idOf(groupA));
            deleteGroupQuietly(operator, idOf(groupB));
            deleteUserQuietly(operator);
        }

        @Test
        @DisplayName("Flat group-admin who is a member of groupA can administer groupA")
        void flatAdminMemberCanAdministerOwnGroup() {
            final var api = keycloakClient.keycloakApi(flatAdminMemberOfA.getToken());

            final var group = api.findById(groupA.getId()).getBody();
            assertThat(group).isNotNull();
            assertThat(group.getId()).isEqualTo(groupA.getId());

            api.revoke(grant(targetUser.getId(), groupA.getId()));
            api.grant(grant(targetUser.getId(), groupA.getId()));

            final var after = api.findById(groupA.getId()).getBody();
            assertThat(after).isNotNull();
            final var member = after.getMembers().stream()
                    .filter(m -> m.getId().equals(targetUser.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(member.getRoles()).contains("service-point-user");
        }

        @Test
        @DisplayName("Flat group-admin who is not a member of groupB is denied on groupB")
        void flatAdminNonMemberDeniedForOtherGroup() {
            final var api = keycloakClient.keycloakApi(flatAdminNotMemberOfB.getToken());

            assertDenied(() -> api.findById(groupB.getId()));
        }
    }

    @Nested
    @DisplayName("Operator bypass")
    class OperatorBypassTests {

        private UserContext operatorCreator;
        private UserContext operatorTester;
        private UserContext targetUser;
        private Group groupA;

        @BeforeEach
        void setUp() {
            operatorCreator = createOperator();
            groupA = createGroup(operatorCreator, "operator-bypass");

            // A separate operator, with no scoped role and no membership in groupA, to prove
            // the bypass is purely role-based.
            operatorTester = createOperator();

            targetUser = userService.createUser("raid-au", "service-point-user");
            joinGroup(targetUser, groupA.getId());
        }

        @AfterEach
        void tearDown() {
            deleteUserQuietly(targetUser);
            deleteGroupQuietly(operatorCreator, idOf(groupA));
            deleteUserQuietly(operatorTester);
            deleteUserQuietly(operatorCreator);
        }

        @Test
        @DisplayName("Operator can administer a group regardless of membership or scoped roles")
        void operatorCanAdministerAnyGroup() {
            final var api = keycloakClient.keycloakApi(operatorTester.getToken());

            final var group = api.findById(groupA.getId()).getBody();
            assertThat(group).isNotNull();
            assertThat(group.getId()).isEqualTo(groupA.getId());

            api.revoke(grant(targetUser.getId(), groupA.getId()));
            api.grant(grant(targetUser.getId(), groupA.getId()));

            final var after = api.findById(groupA.getId()).getBody();
            assertThat(after).isNotNull();
            final var member = after.getMembers().stream()
                    .filter(m -> m.getId().equals(targetUser.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(member.getRoles()).contains("service-point-user");
        }
    }

    @Nested
    @DisplayName("Group-admin grant/revoke dual-write")
    class GroupAdminDualWriteTests {

        private UserContext operator;
        private UserContext targetUser;
        private Group groupA;

        @BeforeEach
        void setUp() {
            operator = createOperator();
            groupA = createGroup(operator, "dual-write");

            targetUser = userService.createUser("raid-au", "service-point-user");
            joinGroup(targetUser, groupA.getId());
        }

        @AfterEach
        void tearDown() {
            deleteUserQuietly(targetUser);
            deleteGroupQuietly(operator, idOf(groupA));
            deleteUserQuietly(operator);
        }

        @Test
        @DisplayName("PUT grants a usable scoped role; DELETE revokes both the flat and scoped role")
        void grantThenRevokeGroupAdmin() {
            final var operatorApi = keycloakClient.keycloakApi(operator.getToken());

            operatorApi.addGroupAdmin(grant(targetUser.getId(), groupA.getId()));

            // Prove the PUT dual-write actually granted a usable scoped role (not just the flat
            // group-admin role) by having the target user - who should now be able to
            // self-administer groupA - call the group SPI with their OWN token, rather than
            // inspecting role mappings directly.
            final var targetApi = keycloakClient.keycloakApi(targetUser.getToken());
            final var group = targetApi.findById(groupA.getId()).getBody();
            assertThat(group).isNotNull();
            assertThat(group.getId()).isEqualTo(groupA.getId());

            operatorApi.removeGroupAdmin(grant(targetUser.getId(), groupA.getId()));

            // The target user is still a real member of groupA at this point, and nothing else
            // has been changed by hand. If DELETE /group-admin only revoked one of the two roles
            // (e.g. left the scoped role in place, or left the flat role while the flat+membership
            // fallback is enabled), the user would still satisfy isGroupAdminOf and this call
            // would succeed. A 401/403 here is only possible if the DELETE actually revoked BOTH
            // the flat group-admin role and the scoped service-point-admin:<groupId> role.
            assertDenied(() -> targetApi.findById(groupA.getId()));
        }
    }

    @Nested
    @DisplayName("Newly created service point group")
    class NewGroupCreationTests {

        private UserContext creatorOperator;
        private UserContext otherOperator;
        private UserContext otherScopedAdmin;
        private Group existingGroup;

        @BeforeEach
        void setUp() {
            creatorOperator = createOperator();
            otherOperator = createOperator();
            existingGroup = createGroup(otherOperator, "existing");

            // Scoped admin of a pre-existing, unrelated group.
            otherScopedAdmin = userService.createUser("raid-au", servicePointAdminRoleName(existingGroup.getId()));
        }

        @AfterEach
        void tearDown() {
            deleteUserQuietly(otherScopedAdmin);
            deleteGroupQuietly(otherOperator, idOf(existingGroup));
            deleteUserQuietly(otherOperator);
            deleteUserQuietly(creatorOperator);
        }

        @Test
        @DisplayName("Creator can immediately administer a newly created group")
        void creatorCanAdministerNewGroupImmediately() {
            final var newGroup = createGroup(creatorOperator, "brand-new");
            try {
                final var api = keycloakClient.keycloakApi(creatorOperator.getToken());
                final var group = api.findById(newGroup.getId()).getBody();
                assertThat(group).isNotNull();
                assertThat(group.getId()).isEqualTo(newGroup.getId());
            } finally {
                deleteGroupQuietly(creatorOperator, newGroup.getId());
            }
        }

        @Test
        @DisplayName("A different service point's scoped admin is denied on the new group")
        void otherServicePointAdminDeniedForNewGroup() {
            final var newGroup = createGroup(creatorOperator, "brand-new-2");
            try {
                final var otherApi = keycloakClient.keycloakApi(otherScopedAdmin.getToken());
                assertDenied(() -> otherApi.findById(newGroup.getId()));
            } finally {
                deleteGroupQuietly(creatorOperator, newGroup.getId());
            }
        }
    }

    @Nested
    @DisplayName("Migration endpoint")
    class MigrationEndpointTests {

        @Test
        @DisplayName("Non-operator caller is denied")
        void nonOperatorDenied() {
            final var nonOperator = userService.createUser("raid-au", "service-point-user");
            try {
                assertDenied(() -> keycloakClient.keycloakApi(nonOperator.getToken()).migrateServicePointAdmins());
            } finally {
                userService.deleteUser(nonOperator.getId());
            }
        }

        @Test
        @DisplayName("Second consecutive run is idempotent (zero new roles/grants)")
        void secondRunIsIdempotent() {
            final var operator = createOperator();
            // Created directly via the admin API (bypassing the SPI /create endpoint) so it has
            // no scoped role yet, giving migration real work to do on the first run.
            final var freshGroupName = "migrate-idem-" + UUID.randomUUID();
            final var freshGroupId = userService.createGroup(freshGroupName, "/" + freshGroupName);
            final var flatAdminUser = userService.createUser("raid-au", "group-admin");
            joinGroup(flatAdminUser, freshGroupId);

            try {
                final var api = keycloakClient.keycloakApi(operator.getToken());

                final var firstRun = api.migrateServicePointAdmins().getBody();
                assertThat(firstRun).isNotNull();
                assertThat(firstRun.getRolesCreated()).isGreaterThanOrEqualTo(1);
                assertThat(firstRun.getGrantsAdded()).isGreaterThanOrEqualTo(1);

                final var secondRun = api.migrateServicePointAdmins().getBody();
                assertThat(secondRun).isNotNull();
                assertThat(secondRun.getRolesCreated()).isEqualTo(0);
                assertThat(secondRun.getGrantsAdded()).isEqualTo(0);
            } finally {
                userService.deleteUser(flatAdminUser.getId());
                deleteGroupQuietly(operator, freshGroupId);
                userService.deleteUser(operator.getId());
            }
        }

        @Test
        @DisplayName("Migration grants a scoped role that is independently sufficient for admin access")
        void migrationGrantsUsableScopedRole() {
            final var operator = createOperator();
            final var freshGroupName = "migrate-verify-" + UUID.randomUUID();
            final var freshGroupId = userService.createGroup(freshGroupName, "/" + freshGroupName);
            final var flatAdminUser = userService.createUser("raid-au", "group-admin");
            joinGroup(flatAdminUser, freshGroupId);

            try {
                keycloakClient.keycloakApi(operator.getToken()).migrateServicePointAdmins();

                // Strip the flat role directly via the admin API (not via the SPI's dual-revoke,
                // which would also remove the scoped role) so only the scoped role remains.
                userService.removeRole(flatAdminUser.getId(), "group-admin");

                final var userApi = keycloakClient.keycloakApi(flatAdminUser.getToken());
                final var group = userApi.findById(freshGroupId).getBody();
                assertThat(group).isNotNull();
                assertThat(group.getId()).isEqualTo(freshGroupId);
            } finally {
                userService.deleteUser(flatAdminUser.getId());
                deleteGroupQuietly(operator, freshGroupId);
                userService.deleteUser(operator.getId());
            }
        }
    }
}
