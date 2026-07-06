package au.org.raid.inttest.client.keycloak;

import au.org.raid.inttest.dto.keycloak.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "keycloak", url = "${raid.iam.base-url}")
public interface KeycloakApi {
    @RequestMapping(method = RequestMethod.POST, value = "/realms/raid/raid/raid-user")
    ResponseEntity<Object> addRaidUser(@RequestBody RaidUserPermissionsRequest user);

    @RequestMapping(method = RequestMethod.DELETE, value = "/realms/raid/raid/raid-user")
    ResponseEntity<Object> removeRaidUser(@RequestBody RaidUserPermissionsRequest user);

    @RequestMapping(method = RequestMethod.POST, value = "/realms/raid/raid/raid-admin")
    ResponseEntity<String> addRaidAdmin(@RequestBody RaidUserPermissionsRequest user);

    @RequestMapping(method = RequestMethod.GET, value = "/realms/raid/group/all")
    ResponseEntity<Groups> allGroups();

    @RequestMapping(method = RequestMethod.GET, value = "/realms/raid/group")
    ResponseEntity<Group> findById(@RequestParam final String groupId);

    @PostMapping(path = "/admin/realms/raid/groups")
    ResponseEntity<Void> createGroup(@RequestBody final CreateGroupRequest groupRequest);

    // Note: the SPI's response body for these mutation endpoints is a JSON object ("{}"), not a
    // JSON string - ResponseEntity<Object> (not <String>) is required or Jackson fails to decode
    // it (RAID-724).
    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/grant")
    ResponseEntity<Object> grant(@RequestBody final Grant grant);

    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/revoke")
    ResponseEntity<Object> revoke(@RequestBody final Grant grant);

    // RAID-724: grant/revoke the flat group-admin role (dual-writes/dual-revokes the scoped
    // "service-point-admin:<groupId>" role alongside it - see GroupController#grant(AddGroupAdminRequest)).
    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/group-admin")
    ResponseEntity<Object> addGroupAdmin(@RequestBody final Grant grant);

    @RequestMapping(method = RequestMethod.DELETE, value = "/realms/raid/group/group-admin")
    ResponseEntity<Object> removeGroupAdmin(@RequestBody final Grant grant);

    // RAID-724: idempotent backfill of scoped service-point-admin roles for existing flat
    // group-admin holders. Operator-only.
    @RequestMapping(method = RequestMethod.POST, value = "/realms/raid/group/migrate-service-point-admins")
    ResponseEntity<MigrationResult> migrateServicePointAdmins();

    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/join")
    ResponseEntity<Object> joinGroup(@RequestBody final GroupJoinRequest groupJoinRequest);

    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/active-group")
    ResponseEntity<Object> setActiveGroup(@RequestBody final ActiveGroupRequest groupJoinRequest);

    @RequestMapping(method = RequestMethod.GET, value = "/realms/raid/group/user-groups")
    ResponseEntity<List<Group>> getUserGroups();

    @GetMapping(path = "/admin/realms/raid/groups")
    ResponseEntity<List<Group>> listGroups();

    @GetMapping(path = "/admin/realms/raid/users")
    ResponseEntity<List<KeycloakUser>> findUserByUsername(@RequestParam("username") final String username);

    @PostMapping(path = "/admin/realms/raid/users")
    ResponseEntity<Void> createUser(@RequestBody final KeycloakUser user);

    @DeleteMapping(path = "/admin/realms/raid/users/{userId}")
    ResponseEntity<Void> deleteUser(@PathVariable final String userId);

    @PutMapping(path = "/admin/realms/raid/users/{userId}/reset-password")
    ResponseEntity<List<KeycloakUser>> resetPassword(@PathVariable final String userId,
                                                     @RequestBody final KeycloakCredentials credentials);

    @GetMapping(path = "/admin/realms/raid/roles/{roleName}")
    ResponseEntity<KeycloakRole> findRoleByName(@PathVariable final String roleName);

    @PostMapping(path = "/admin/realms/raid/users/{userId}/role-mappings/realm")
    ResponseEntity<Void> addUserToRole(@PathVariable final String userId, @RequestBody final List<KeycloakRole> roles);

    // RAID-724: mirrors addUserToRole above but removes a realm role mapping, so tests can strip
    // a role (e.g. the flat group-admin role) directly via the Keycloak admin API.
    @DeleteMapping(path = "/admin/realms/raid/users/{userId}/role-mappings/realm")
    ResponseEntity<Void> removeUserFromRole(@PathVariable final String userId, @RequestBody final List<KeycloakRole> roles);

    @RequestMapping(method = RequestMethod.POST, value = "/realms/raid/group/create")
    ResponseEntity<Object> createGroupViaSpi(@RequestBody final java.util.Map<String, String> group);

    @RequestMapping(method = RequestMethod.DELETE, value = "/realms/raid/group/delete")
    ResponseEntity<java.util.Map<String, String>> deleteGroup(@RequestParam final String groupId);
}
