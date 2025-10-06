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

    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/grant")
    ResponseEntity<String> grant(@RequestBody final Grant grant);

    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/revoke")
    ResponseEntity<String> revoke(@RequestBody final Grant grant);

    @RequestMapping(method = RequestMethod.PUT, value = "/realms/raid/group/join")
    ResponseEntity<String> joinGroup(@RequestBody final GroupJoinRequest groupJoinRequest);

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


}
