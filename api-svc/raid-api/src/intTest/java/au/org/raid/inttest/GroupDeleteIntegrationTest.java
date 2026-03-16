package au.org.raid.inttest;

import au.org.raid.inttest.dto.keycloak.CreateGroupRequest;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("Group Delete Integration Tests")
class GroupDeleteIntegrationTest extends AbstractIntegrationTest {

    private String testGroupId;

    @AfterEach
    void cleanupTestGroup() {
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
        @DisplayName("Should fail when user is not an operator")
        void shouldFailWhenNotOperator() {
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
            var groupName = "test-delete-group-" + System.currentTimeMillis();

            // Create a group via the SPI create endpoint
            var createGroupRequest = new CreateGroupRequest();
            createGroupRequest.setName(groupName);

            keycloakClient
                    .keycloakApi(operatorToken)
                    .createGroupViaSpi(createGroupRequest);

            // Find the created group
            var api = keycloakClient.keycloakApi(operatorToken);
            var groupsAfterCreate = api.allGroups().getBody();
            assertThat(groupsAfterCreate).isNotNull();

            var createdGroup = groupsAfterCreate.getGroups().stream()
                    .filter(g -> g.getName().equals(groupName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Test group was not created"));

            testGroupId = createdGroup.getId();

            // Delete the group
            var deleteResponse = api.deleteGroup(testGroupId);

            assertThat(deleteResponse.getStatusCode().value()).isEqualTo(200);
            assertThat(deleteResponse.getBody()).containsEntry("message", "Group deleted successfully");

            // Verify group no longer exists
            var groupsAfterDelete = api.allGroups().getBody();
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