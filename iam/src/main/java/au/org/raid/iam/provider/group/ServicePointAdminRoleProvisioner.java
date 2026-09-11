package au.org.raid.iam.provider.group;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * Provisioning of the scoped "service-point-admin:&lt;groupId&gt;" realm roles.
 *
 * <p>Extracted from {@link GroupController} for RAID-884 so the same backfill can run
 * unattended at boot ({@link ServicePointAdminRoleBootstrapper}) as well as from the
 * operator-only endpoint that used to be the only way to run it. The two callers share this
 * code so they cannot drift apart.
 */
@Slf4j
public final class ServicePointAdminRoleProvisioner {
    public static final String GROUP_ADMIN_ROLE_NAME = "group-admin";
    public static final String SERVICE_POINT_USER_ROLE = "service-point-user";
    public static final String SERVICE_POINT_ADMIN_ROLE_PREFIX = "service-point-admin";

    private static final int PAGE_SIZE = 100;

    private ServicePointAdminRoleProvisioner() {
    }

    public static String servicePointAdminRoleName(final String groupId) {
        return SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + groupId;
    }

    /**
     * Looks up the realm role scoped to a single service point group
     * ("service-point-admin:&lt;groupId&gt;"), creating it if it does not already exist.
     */
    public static RoleModel getOrCreateServicePointAdminRole(final RealmModel realm, final String groupId) {
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

    public static boolean isGroupMember(final UserModel user, final String groupId) {
        return user.getGroupsStream().anyMatch(g -> g.getId().equals(groupId));
    }

    /**
     * A user is an approved member of a group if they are a Keycloak group member AND hold the
     * service-point-user role for that membership to mean anything - service-point-user is only
     * ever granted via an explicit grant() approval, never automatically. This distinguishes a
     * legitimate, approved service point affiliation from a raw, unapproved self-join via
     * /group/join (see RAiD-608 / HELP-2844: a self-joined-but-never-approved membership was
     * previously enough to satisfy the flat group-admin fallback).
     */
    public static boolean isApprovedGroupMember(final UserModel user, final String groupId) {
        return isGroupMember(user, groupId) &&
                user.getRoleMappingsStream().anyMatch(r -> r.getName().equals(SERVICE_POINT_USER_ROLE));
    }

    /**
     * Idempotent backfill (RAID-712): grants the scoped "service-point-admin:&lt;groupId&gt;" realm
     * role to every current holder of the legacy flat {@value #GROUP_ADMIN_ROLE_NAME} role, for each
     * group they are an approved member of (see {@link #isApprovedGroupMember}). This preserves each
     * flat group-admin's existing <em>legitimate</em> effective access while service points
     * transition onto scoped roles (see role-permissions.md section 9).
     *
     * <p>Safe to re-run: users who already hold the scoped role for a group are counted as skipped
     * rather than re-granted, and existing scoped roles are reused rather than recreated. A realm
     * with no flat group-admin role - any realm other than the RAiD one, including master - is a
     * no-op.
     *
     * <p>Note: raw/pending memberships (groups the user has self-joined but was never granted
     * service-point-user for) are deliberately excluded - backfilling those would silently grant
     * admin authority the user was never approved for (RAiD-608 / HELP-2844). Within a group the
     * member is genuinely approved for, this may still over-grant relative to what they were
     * originally intended to administer if they are an approved member of more than one group.
     * That breadth is deliberate and unchanged from the endpoint this replaces (RAID-884): it
     * preserves current effective access, since the flat group-admin role was itself unscoped.
     * Pruning it is tracked separately and must not be done as a side effect of this backfill.
     */
    public static MigrationResult backfill(final KeycloakSession session, final RealmModel realm) {
        final var flatGroupAdminRole = realm.getRole(GROUP_ADMIN_ROLE_NAME);

        if (flatGroupAdminRole == null) {
            return new MigrationResult(0, 0, 0, 0,
                    "Flat group-admin role not present; nothing to migrate");
        }

        var flatGroupAdminUsers = 0;
        var rolesCreated = 0;
        var grantsAdded = 0;
        var grantsSkipped = 0;

        var firstResult = 0;
        while (true) {
            final var page = session.users()
                    .getRoleMembersStream(realm, flatGroupAdminRole, firstResult, PAGE_SIZE)
                    .toList();

            if (page.isEmpty()) {
                break;
            }

            for (final var member : page) {
                flatGroupAdminUsers++;

                // Materialise before granting roles below - member.grantRole mutates this user's
                // role mappings, and we must not mutate them while consuming a live stream over
                // the same underlying data. Only backfill groups the member is an approved (not
                // merely raw/pending self-joined) member of - see isApprovedGroupMember and
                // RAiD-608 / HELP-2844.
                final var groups = member.getGroupsStream()
                        .filter(g -> isApprovedGroupMember(member, g.getId()))
                        .toList();

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

            firstResult += PAGE_SIZE;
        }

        return new MigrationResult(
                flatGroupAdminUsers, rolesCreated, grantsAdded, grantsSkipped, "Migration complete");
    }

    public record MigrationResult(
            int flatGroupAdminUsers, int rolesCreated, int grantsAdded, int grantsSkipped, String message) {
    }
}
