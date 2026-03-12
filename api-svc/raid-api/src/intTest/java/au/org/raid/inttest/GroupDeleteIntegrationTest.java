package au.org.raid.inttest;

import au.org.raid.inttest.dto.keycloak.Group;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("Group Delete Integration Tests")
class GroupDeleteIntegrationTest extends AbstractIntegrationTest {

    private String testGroupId;

    @BeforeEach
    void createTestGroup() {
        // Create a group via the Keycloak admin API for deletion tests
        var groups = keycloakClient
                .keycloakApi(userContext.getToken())
                .allGroups()
                .getBody();

        // Store initial group count for later assertions
        assertThat(groups).isNotNull();
    }

    @AfterEach
    void cleanupTestGroup() {
        // Attempt to clean up if group still exists
        if (testGroupId != null) {
            try {
                var operatorContext = userService.createUser("raid-au", "operator");
                try {
                    keycloakClient
                            .keycloakApi(operatorContext.getToken())
                            .deleteGroup(testGroupId);
                } finally {
                    userService.deleteUser(operatorContext.getId());
                }
            } catch (Exception e) {
                // Group may already be deleted, ignore
            }
        }
    }

    @Nested
    @DisplayName("Authentication & Authorization")
    class AuthTests {

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() {
            try {
                var unauthenticatedApi = keycloakClient.keycloakApi("invalid-token");
                unauthenticatedApi.deleteGroup("some-group-id");
                fail("Expected an exception");
            } catch (Exception e) {
                assertThat(e.getMessage()).contains("401");
            }
        }

        @Test
        @DisplayName("Should return 403 when user is not an operator")
        void shouldReturn403WhenNotOperator() {
            // Default user from AbstractIntegrationTest has service-point-user role, not operator
            try {
                keycloakClient
                        .keycloakApi(userContext.getToken())
                        .deleteGroup("some-group-id");
                fail("Expected an exception");
            } catch (Exception e) {
                assertThat(e.getMessage()).containsAnyOf("403", "401");
            }
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        private String operatorToken;
        private String operatorUserId;

        @BeforeEach
        void createOperator() {
            var operatorContext = userService.createUser("raid-au", "operator");
            operatorToken = operatorContext.getToken();
            operatorUserId = operatorContext.getId();
        }

        @AfterEach
        void cleanupOperator() {
            userService.deleteUser(operatorUserId);
        }

        @Test
        @DisplayName("Should return 400 when groupId is empty")
        void shouldReturn400WhenGroupIdEmpty() {
            try {
                keycloakClient
                        .keycloakApi(operatorToken)
                        .deleteGroup("");
                fail("Expected an exception");
            } catch (Exception e) {
                assertThat(e.getMessage()).containsAnyOf("400", "groupId");
            }
        }

        @Test
        @DisplayName("Should return 404 when group does not exist")
        void shouldReturn404WhenGroupNotFound() {
            try {
                keycloakClient
                        .keycloakApi(operatorToken)
                        .deleteGroup("non-existent-group-id");
                fail("Expected an exception");
            } catch (Exception e) {
                assertThat(e.getMessage()).containsAnyOf("404", "not found");
            }
        }
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessTests {

        private String operatorToken;
        private String operatorUserId;

        @BeforeEach
        void createOperator() {
            var operatorContext = userService.createUser("raid-au", "operator");
            operatorToken = operatorContext.getToken();
            operatorUserId = operatorContext.getId();
        }

        @AfterEach
        void cleanupOperator() {
            userService.deleteUser(operatorUserId);
        }

        @Test
        @DisplayName("Should delete group successfully")
        void shouldDeleteGroupSuccessfully() {
            // Create a group first via the SPI create endpoint
            var createApi = keycloakClient.keycloakApi(operatorToken);

            // Get all groups before
            var groupsBefore = createApi.allGroups().getBody();
            assertThat(groupsBefore).isNotNull();
            var countBefore = groupsBefore.getGroups().size();

            // Create a test group using the SPI endpoint
            // We need to use the custom SPI create endpoint
            var joinRequest = new au.org.raid.inttest.dto.keycloak.GroupJoinRequest();

            // Use admin API to create a group for testing
            var adminApi = keycloakClient.keycloakApi(operatorToken);

            // Find a group to delete - create one via admin API
            var groupName = "test-delete-group-" + System.currentTimeMillis();

            var createGroupRequest = java.util.Map.of("name", groupName);

            // Use RestTemplate directly to create the group via admin API
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + operatorToken);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            var body = java.util.Map.of("name", groupName, "path", "/groups/" + groupName);
            var entity = new org.springframework.http.HttpEntity<>(body, headers);

            new org.springframework.web.client.RestTemplate()
                    .postForEntity("http://localhost:8001/realms/raid/group/create", entity, String.class);

            // Find the created group
            var groupsAfterCreate = createApi.allGroups().getBody();
            assertThat(groupsAfterCreate).isNotNull();

            var createdGroup = groupsAfterCreate.getGroups().stream()
                    .filter(g -> g.getName().equals(groupName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Test group was not created"));

            testGroupId = createdGroup.getId();

            // Delete the group
            // Delete the group
            var deleteResponse = keycloakClient
                    .keycloakApi(operatorToken)
                    .deleteGroup(testGroupId);

            assertThat(deleteResponse.getStatusCode().value()).isEqualTo(200);
            assertThat(deleteResponse.getBody()).containsEntry("message", "Group deleted successfully");

            // Verify group no longer exists
            var groupsAfterDelete = createApi.allGroups().getBody();
            assertThat(groupsAfterDelete).isNotNull();

            var deletedGroup = groupsAfterDelete.getGroups().stream()
                    .filter(g -> g.getId().equals(testGroupId))
                    .findFirst();

            assertThat(deletedGroup).isEmpty();

            // Clear testGroupId since it's already deleted
            testGroupId = null;
        }
    }
}