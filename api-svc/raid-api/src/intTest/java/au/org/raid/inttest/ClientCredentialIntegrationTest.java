package au.org.raid.inttest;

import au.org.raid.inttest.config.AuthConfig;
import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.dto.keycloak.CreateCredentialRequest;
import au.org.raid.inttest.dto.keycloak.CredentialSecretResponse;
import au.org.raid.inttest.dto.keycloak.Group;
import au.org.raid.inttest.dto.keycloak.RotateCredentialRequest;
import feign.FeignException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * RAID-848: proves the RAID-827 acceptance criteria for the scoped client credential lifecycle,
 * end to end, through real calls to the Keycloak SPI, the Keycloak token endpoint and the Admin API.
 *
 * <p>Unit tests in the {@code iam} module cover the same authorisation matrix against mocks. That
 * overlap is deliberate: those guard the implementation, these demonstrate the accepted behaviour.
 * Several scenarios here are simply not expressible against mocks, notably that a minted credential
 * can actually authenticate and carries the right claims.
 */
@DisplayName("Client Credential Integration Tests")
class ClientCredentialIntegrationTest extends AbstractIntegrationTest {

    private static final int MAX_CREDENTIALS_PER_SERVICE_POINT = 10;

    @org.springframework.beans.factory.annotation.Autowired
    private AuthConfig authConfig;

    private UserContext operator;
    private Group groupA;
    private Group groupB;
    private UserContext adminA;

    /**
     * Keycloak's Admin API needs {@code realm-management} permissions, which the RAiD realm's
     * "operator" role does not confer - an operator is an application-level administrator, not a
     * Keycloak realm administrator. Admin API assertions therefore go through the
     * integration-test-client service account, as {@code UserService} does.
     */
    private au.org.raid.inttest.client.keycloak.KeycloakApi adminApi() {
        return keycloakClient.keycloakApi(authConfig.getIntegrationTestClient());
    }

    @BeforeEach
    void setUpCredentialFixtures() {
        operator = userService.createUser("raid-au", "operator");
        // Fresh groups per test so credential counts cannot leak between tests, which matters for
        // the cap scenarios in particular.
        groupA = createGroup("cred-a");
        groupB = createGroup("cred-b");
        adminA = userService.createUser(groupA.getName(), servicePointAdminRole(groupA.getId()));
    }

    @AfterEach
    void tearDownCredentialFixtures() {
        deleteUserQuietly(adminA);
        deleteGroupQuietly(groupA);
        deleteGroupQuietly(groupB);
        deleteUserQuietly(operator);
    }

    // ------------------------------------------------------------------ helpers

    private static String servicePointAdminRole(final String groupId) {
        return "service-point-admin:" + groupId;
    }

    private static String servicePointUserRole(final String groupId) {
        return "service-point-user:" + groupId;
    }

    /**
     * Creates a group through the SPI, which also creates its scoped
     * {@code service-point-admin:<groupId>} role as a side effect, so a scoped admin user can then
     * be granted it.
     */
    private Group createGroup(final String namePrefix) {
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

    private void deleteGroupQuietly(final Group group) {
        if (operator == null || group == null) {
            return;
        }
        try {
            keycloakClient.keycloakApi(operator.getToken()).deleteGroup(group.getId());
        } catch (Exception e) {
            // Already gone, or cleanup raced another test - not a failure of the test itself.
        }
    }

    private void deleteUserQuietly(final UserContext user) {
        if (user != null) {
            userService.deleteUser(user.getId());
        }
    }

    private CredentialSecretResponse createCredential(final UserContext as, final String groupId, final String label) {
        final var response = keycloakClient.keycloakApi(as.getToken())
                .createClientCredential(new CreateCredentialRequest(groupId, label));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    /** Decoded claims of an access token, so assertions can be made on what the token actually carries. */
    private Map<String, Object> claimsOf(final String accessToken) {
        final var payload = accessToken.split("\\.")[1];
        final var decoded = new String(Base64.getUrlDecoder().decode(payload));
        try {
            return objectMapper.readValue(decoded, Map.class);
        } catch (Exception e) {
            fail("Could not decode token payload: " + e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> realmRolesOf(final String accessToken) {
        final var realmAccess = (Map<String, Object>) claimsOf(accessToken).get("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        return (List<String>) realmAccess.getOrDefault("roles", List.of());
    }

    /** Denials surface as 401 or 403 depending on which check rejects first; either is acceptable. */
    private void assertDenied(final ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(FeignException.class)
                .satisfies(e -> assertThat(((FeignException) e).status())
                        .describedAs("expected an authorisation failure")
                        .isIn(401, 403));
    }

    // ------------------------------------------------------------------ tests

    @Nested
    @DisplayName("A minted credential works and is correctly scoped")
    class CreateAndUse {

        @Test
        @DisplayName("credential obtains a token carrying its service point and scoped role")
        void mintedCredentialObtainsCorrectlyScopedToken() {
            final var credential = createCredential(adminA, groupA.getId(), "ci pipeline");

            // The whole point of the feature: the credential must actually authenticate.
            final var token = tokenService.getClientToken(credential.clientId(), credential.secret());

            assertThat(claimsOf(token).get("service_point_group_id"))
                    .describedAs("token must identify the owning service point")
                    .isEqualTo(groupA.getId());
            assertThat(realmRolesOf(token))
                    .describedAs("token must carry the scoped usage role")
                    .contains(servicePointUserRole(groupA.getId()));
        }

        @Test
        @DisplayName("credential never receives an admin role")
        void credentialNeverReceivesAnAdminRole() {
            final var credential = createCredential(adminA, groupA.getId(), "ci pipeline");
            final var token = tokenService.getClientToken(credential.clientId(), credential.secret());

            assertThat(realmRolesOf(token))
                    .noneMatch(role -> role.contains("admin") || role.equals("operator"));
        }

        @Test
        @DisplayName("service account role mappings verified via the Admin API, not inferred")
        void serviceAccountRoleMappingsVerifiedViaAdminApi() {
            final var credential = createCredential(adminA, groupA.getId(), "ci pipeline");
            final var admin = adminApi();

            // Reached by service account username rather than via /admin/realms/raid/clients,
            // because integration-test-client holds only realm-management [manage-users,
            // view-realm] and the clients endpoint needs view-clients.
            final var serviceAccounts =
                    admin.findUserByUsername("service-account-" + credential.clientId()).getBody();
            assertThat(serviceAccounts)
                    .describedAs("every credential must have a service account user")
                    .isNotNull().hasSize(1);

            final var roles = admin.getRealmRoleMappings(serviceAccounts.get(0).getId()).getBody();
            assertThat(roles).isNotNull();
            assertThat(roles).extracting("name").contains(servicePointUserRole(groupA.getId()));
            assertThat(roles).extracting("name").noneMatch(name -> String.valueOf(name).contains("admin"));
        }

        @Test
        @DisplayName("create response sets Cache-Control: no-store and returns the label")
        void createResponseIsUncacheableAndCarriesTheLabel() {
            final var response = keycloakClient.keycloakApi(adminA.getToken())
                    .createClientCredential(new CreateCredentialRequest(groupA.getId(), "labelled"));

            assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().label()).isEqualTo("labelled");
            assertThat(response.getBody().clientId()).isNotBlank();
            assertThat(response.getBody().secret()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Rotation")
    class Rotate {

        @Test
        @DisplayName("old secret stops working immediately and the new one works")
        void rotationInvalidatesTheOldSecret() {
            final var original = createCredential(adminA, groupA.getId(), "rotate me");
            // Prove it worked before rotating, so a failure afterwards is attributable to rotation.
            tokenService.getClientToken(original.clientId(), original.secret());

            final var rotated = keycloakClient.keycloakApi(adminA.getToken())
                    .rotateClientCredential(new RotateCredentialRequest(original.clientId()))
                    .getBody();
            assertThat(rotated).isNotNull();
            assertThat(rotated.secret()).isNotEqualTo(original.secret());

            assertThatThrownBy(() -> tokenService.getClientToken(original.clientId(), original.secret()))
                    .describedAs("the previous secret must stop working immediately");

            final var token = tokenService.getClientToken(rotated.clientId(), rotated.secret());
            assertThat(claimsOf(token).get("service_point_group_id")).isEqualTo(groupA.getId());
        }

        @Test
        @DisplayName("clientId, label and scoped role survive rotation")
        void rotationPreservesIdentityAndScope() {
            final var original = createCredential(adminA, groupA.getId(), "stable label");

            final var rotated = keycloakClient.keycloakApi(adminA.getToken())
                    .rotateClientCredential(new RotateCredentialRequest(original.clientId()))
                    .getBody();

            assertThat(rotated).isNotNull();
            assertThat(rotated.clientId()).isEqualTo(original.clientId());
            assertThat(rotated.label()).isEqualTo(original.label());

            final var token = tokenService.getClientToken(rotated.clientId(), rotated.secret());
            assertThat(realmRolesOf(token)).contains(servicePointUserRole(groupA.getId()));
        }

        @Test
        @DisplayName("rotating a revoked credential is rejected")
        void rotatingARevokedCredentialIsRejected() {
            final var credential = createCredential(adminA, groupA.getId(), "doomed");
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            api.revokeClientCredential(credential.clientId());

            assertThatThrownBy(() -> api.rotateClientCredential(new RotateCredentialRequest(credential.clientId())))
                    .isInstanceOf(FeignException.class)
                    .satisfies(e -> assertThat(((FeignException) e).status()).isEqualTo(409));
        }
    }

    @Nested
    @DisplayName("Revocation")
    class Revoke {

        @Test
        @DisplayName("a revoked credential can no longer obtain a token")
        void revokedCredentialCannotAuthenticate() {
            final var credential = createCredential(adminA, groupA.getId(), "revoke me");
            tokenService.getClientToken(credential.clientId(), credential.secret());

            keycloakClient.keycloakApi(adminA.getToken()).revokeClientCredential(credential.clientId());

            assertThatThrownBy(() -> tokenService.getClientToken(credential.clientId(), credential.secret()))
                    .describedAs("a revoked credential must not authenticate");
        }

        @Test
        @DisplayName("revoking twice is a no-op success, not an error")
        void revokeIsIdempotent() {
            final var credential = createCredential(adminA, groupA.getId(), "twice");
            final var api = keycloakClient.keycloakApi(adminA.getToken());

            assertThat(api.revokeClientCredential(credential.clientId()).getStatusCode().value()).isEqualTo(200);
            assertThat(api.revokeClientCredential(credential.clientId()).getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Listing and secret retrieval")
    class ListAndSecret {

        @Test
        @DisplayName("listing returns only the service point's own credentials")
        void listingIsScopedToTheServicePoint() {
            final var mine = createCredential(adminA, groupA.getId(), "mine");
            final var theirs = createCredential(operator, groupB.getId(), "theirs");

            final var listed = keycloakClient.keycloakApi(adminA.getToken())
                    .listClientCredentials(groupA.getId()).getBody();

            assertThat(listed).isNotNull();
            assertThat(listed).extracting("clientId").contains(mine.clientId());
            assertThat(listed).extracting("clientId").doesNotContain(theirs.clientId());
        }

        @Test
        @DisplayName("listing never includes the realm's own clients")
        void listingNeverIncludesRealmClients() {
            createCredential(adminA, groupA.getId(), "mine");

            final var listed = keycloakClient.keycloakApi(adminA.getToken())
                    .listClientCredentials(groupA.getId()).getBody();

            assertThat(listed).isNotNull();
            assertThat(listed).extracting("clientId")
                    .doesNotContain("raid-api", "raid-dumper", "integration-test-client", "admin-cli");
        }

        @Test
        @DisplayName("get-secret returns the current secret and is uncacheable")
        void getSecretReturnsTheCurrentSecret() {
            final var credential = createCredential(adminA, groupA.getId(), "peek");

            final var response = keycloakClient.keycloakApi(adminA.getToken())
                    .getClientCredentialSecret(credential.clientId());

            assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().secret()).isEqualTo(credential.secret());
            // And it is genuinely usable, not just echoed back.
            tokenService.getClientToken(credential.clientId(), response.getBody().secret());
        }
    }

    @Nested
    @DisplayName("Authorisation")
    class Authorisation {

        @Test
        @DisplayName("an admin of one service point cannot create for another")
        void cannotCreateForAnotherServicePoint() {
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            assertDenied(() -> api.createClientCredential(new CreateCredentialRequest(groupB.getId(), "not mine")));
        }

        @Test
        @DisplayName("an admin of one service point cannot list another's credentials")
        void cannotListAnotherServicePoint() {
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            assertDenied(() -> api.listClientCredentials(groupB.getId()));
        }

        @Test
        @DisplayName("an admin of one service point cannot read another's secret")
        void cannotReadAnotherServicePointsSecret() {
            final var theirs = createCredential(operator, groupB.getId(), "theirs");
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            assertDenied(() -> api.getClientCredentialSecret(theirs.clientId()));
        }

        @Test
        @DisplayName("an admin of one service point cannot revoke another's credential")
        void cannotRevokeAnotherServicePointsCredential() {
            final var theirs = createCredential(operator, groupB.getId(), "theirs");
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            assertDenied(() -> api.revokeClientCredential(theirs.clientId()));
        }

        @Test
        @DisplayName("the flat legacy group-admin role is rejected, with no fallback")
        void flatGroupAdminIsRejected() {
            // GroupController still honours the flat role behind a fallback flag; these endpoints
            // deliberately do not.
            final var flatAdmin = userService.createUser(groupA.getName(), "group-admin", "service-point-user");
            try {
                final var api = keycloakClient.keycloakApi(flatAdmin.getToken());
                assertDenied(() -> api.createClientCredential(new CreateCredentialRequest(groupA.getId(), "nope")));
                assertDenied(() -> api.listClientCredentials(groupA.getId()));
            } finally {
                deleteUserQuietly(flatAdmin);
            }
        }

        @Test
        @DisplayName("an operator can manage any service point via the uniform short-circuit")
        void operatorCanManageAnyServicePoint() {
            final var credential = createCredential(operator, groupB.getId(), "operator made this");

            final var listed = keycloakClient.keycloakApi(operator.getToken())
                    .listClientCredentials(groupB.getId()).getBody();
            assertThat(listed).isNotNull();
            assertThat(listed).extracting("clientId").contains(credential.clientId());

            // Even an operator-created credential gets only a scoped usage role.
            final var token = tokenService.getClientToken(credential.clientId(), credential.secret());
            assertThat(realmRolesOf(token)).contains(servicePointUserRole(groupB.getId()));
            assertThat(realmRolesOf(token)).noneMatch(r -> r.contains("admin") || r.equals("operator"));
        }
    }

    @Nested
    @DisplayName("Per-service-point cap")
    class Cap {

        @Test
        @DisplayName("creating beyond the cap is rejected with 409")
        void creatingBeyondTheCapIsRejected() {
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            for (var i = 0; i < MAX_CREDENTIALS_PER_SERVICE_POINT; i++) {
                createCredential(adminA, groupA.getId(), "cap-" + i);
            }

            assertThatThrownBy(() -> api.createClientCredential(
                    new CreateCredentialRequest(groupA.getId(), "one too many")))
                    .isInstanceOf(FeignException.class)
                    .satisfies(e -> {
                        assertThat(((FeignException) e).status()).isEqualTo(409);
                        assertThat(((FeignException) e).contentUTF8())
                                .describedAs("error body should name the limit")
                                .contains(String.valueOf(MAX_CREDENTIALS_PER_SERVICE_POINT));
                    });
        }

        @Test
        @DisplayName("revoking frees a slot")
        void revokingFreesASlot() {
            final var api = keycloakClient.keycloakApi(adminA.getToken());
            CredentialSecretResponse first = null;
            for (var i = 0; i < MAX_CREDENTIALS_PER_SERVICE_POINT; i++) {
                final var created = createCredential(adminA, groupA.getId(), "cap-" + i);
                if (i == 0) {
                    first = created;
                }
            }

            api.revokeClientCredential(first.clientId());

            final var afterRevoke = api.createClientCredential(
                    new CreateCredentialRequest(groupA.getId(), "reused slot"));
            assertThat(afterRevoke.getStatusCode().value()).isEqualTo(201);
        }
    }
}
