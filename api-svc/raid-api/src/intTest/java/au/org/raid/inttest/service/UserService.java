package au.org.raid.inttest.service;

import au.org.raid.inttest.client.keycloak.KeycloakClient;
import au.org.raid.inttest.config.AuthConfig;
import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.dto.keycloak.CreateGroupRequest;
import au.org.raid.inttest.dto.keycloak.KeycloakCredentialsFactory;
import au.org.raid.inttest.dto.keycloak.KeycloakUserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserService {
    private final AuthConfig authConfig;
    private final KeycloakClient keycloakClient;
    private final KeycloakUserFactory userFactory;
    private final KeycloakCredentialsFactory credentialsFactory;
    private final TokenService tokenService;

    public UserContext createUser(final String groupName, final String... roleNames) {
        final var username = UUID.randomUUID().toString();
        final var password = UUID.randomUUID().toString();

        final var groups = keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).listGroups().getBody();

        assert groups != null;

        final var group = groups.stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Group %s not found".formatted(groupName)));

        final var user = userFactory.create(username, group.getId());

        final var userResponse = keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).createUser(user);
        final var location = userResponse.getHeaders().get("Location").get(0);
        log.info("User created at {}", location);
        final var userId = location.substring(location.lastIndexOf("/") + 1);
        log.info("User id = {}", userId);

        final var credentials = credentialsFactory.createPassword(password);

        keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).resetPassword(userId, credentials);

        Arrays.stream(roleNames)
                .map(roleName ->
                        keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).findRoleByName(roleName).getBody())
                .filter(Objects::nonNull)
                .forEach(role -> keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).addUserToRole(userId, Collections.singletonList(role)));

        final var token = tokenService.getUserToken(username, password);

        return UserContext.builder()
                .token(token)
                .id(userId)
                .username(username)
                .password(password)
                .build();
    }

    public void deleteUser(final String userId) {
        try {
            keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).deleteUser(userId);
        } catch (Exception e) {
            log.error("Failed to delete user", e);

        }
    }

    /**
     * RAID-724: removes a realm role mapping from a user via the Keycloak admin API, mirroring
     * the role-granting loop in {@link #createUser}. Used by tests that need to strip a role
     * (e.g. the flat group-admin role) after user creation, to prove a scoped
     * "service-point-admin:&lt;groupId&gt;" role is independently sufficient for authorization.
     */
    public void removeRole(final String userId, final String roleName) {
        final var role = keycloakClient.keycloakApi(authConfig.getIntegrationTestClient())
                .findRoleByName(roleName)
                .getBody();

        if (role != null) {
            keycloakClient.keycloakApi(authConfig.getIntegrationTestClient())
                    .removeUserFromRole(userId, Collections.singletonList(role));
        }
    }

    /**
     * RAID-724: creates a group directly via the Keycloak admin API, bypassing the
     * "/realms/raid/group/create" SPI endpoint entirely. Unlike the SPI endpoint, this does not
     * auto-create a scoped "service-point-admin:&lt;groupId&gt;" role or grant any roles to a
     * creator - used by tests that need a group in a known "not yet migrated" state.
     *
     * @return the id of the newly created group
     */
    public String createGroup(final String name, final String path) {
        final var request = new CreateGroupRequest(name, path);
        final var response = keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).createGroup(request);
        final var location = response.getHeaders().get("Location").get(0);
        return location.substring(location.lastIndexOf("/") + 1);
    }
}
