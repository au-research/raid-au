package au.org.raid.inttest;

import au.org.raid.fixtures.APIFixtures;
import au.org.raid.idl.raidv2.api.RaidApi;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidPatchRequest;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import au.org.raid.inttest.config.AuthConfig;
import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.dto.keycloak.CreateCredentialRequest;
import au.org.raid.inttest.dto.keycloak.CredentialSecretResponse;
import au.org.raid.inttest.dto.keycloak.Group;
import au.org.raid.inttest.dto.keycloak.RotateCredentialRequest;
import au.org.raid.inttest.service.Handle;
import feign.FeignException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static au.org.raid.fixtures.TestConstants.EMBARGOED_ACCESS_TYPE;
import static au.org.raid.fixtures.TestConstants.REAL_TEST_ORCID;
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

    /**
     * RAID-877: a minted credential could authenticate and carry the right claims (proven by the
     * tests above) yet still be rejected 403 insufficient_scope on every RAiD data API endpoint,
     * because SecurityConfig, RaidAuthorizationService and TokenUtil each independently tested for
     * a flat "service-point-user" authority that a scoped "service-point-user:&lt;groupId&gt;" role
     * never produced. These tests call the actual RAiD data API with a minted credential's token -
     * the blind spot that let that bug ship, since every scenario above only asserted on the token
     * and the Keycloak Admin API.
     *
     * <p>Credentials here are scoped to two service points that are pre-existing fixture data (not
     * groups created via the SPI in this test class), because minting a raid requires an
     * {@code api_svc.service_point} row backing the group, which the dynamically-created groupA/
     * groupB do not have. See {@code V32.1__update_service_point_repository.sql} and
     * {@code V40.1__update_service_point_repository.sql} for the group id / service point id
     * pairings. RAID-877: the actual service_point.id values are resolved at runtime via
     * {@code AbstractIntegrationTest.resolveServicePointId(String)} rather than hardcoded, since
     * {@code service_point.id} is a Postgres-sequence-allocated value that differs per
     * environment - only the Keycloak group ids ({@code RAID_AU_GROUP_ID} /
     * {@code RAID_AU_REGISTRY_2_GROUP_ID}, both inherited from {@code AbstractIntegrationTest})
     * are stable everywhere.
     */
    @Nested
    @DisplayName("Data API access")
    class DataApiAccess {

        // These two fixture service points are shared, persistent Keycloak groups (unlike groupA/
        // groupB above, which this class creates and deletes per test) and are capped at 10 active
        // credentials each, so every credential minted against them here must be revoked - tracked
        // and cleaned up in tearDown() rather than left for a future run to trip the cap.
        private final List<String> credentialClientIdsToRevoke = new ArrayList<>();

        @AfterEach
        void revokeCredentials() {
            final var api = keycloakClient.keycloakApi(operator.getToken());
            for (final var clientId : credentialClientIdsToRevoke) {
                try {
                    api.revokeClientCredential(clientId);
                } catch (Exception e) {
                    // Already gone, or the test that created it failed before revocation mattered.
                }
            }
        }

        private CredentialSecretResponse credential(final String groupId, final String label) {
            final var created = createCredential(operator, groupId, label);
            credentialClientIdsToRevoke.add(created.clientId());
            return created;
        }

        private RaidApi credentialRaidApi(final CredentialSecretResponse credential) {
            final var token = tokenService.getClientToken(credential.clientId(), credential.secret());
            return testClient.raidApi(token);
        }

        /** Mirrors RaidIntegrationTest's private helper of the same name - not shared between test classes. */
        private RaidUpdateRequest mapReadToUpdate(final RaidDto read) {
            return new RaidUpdateRequest()
                    .metadata(read.getMetadata())
                    .identifier(read.getIdentifier())
                    .title(read.getTitle())
                    .date(read.getDate())
                    .description(read.getDescription())
                    .access(read.getAccess())
                    .alternateUrl(read.getAlternateUrl())
                    .contributor(read.getContributor())
                    .organisation(read.getOrganisation())
                    .subject(read.getSubject())
                    .relatedRaid(read.getRelatedRaid())
                    .relatedObject(read.getRelatedObject())
                    .alternateIdentifier(read.getAlternateIdentifier())
                    .spatialCoverage(read.getSpatialCoverage());
        }

        @Test
        @DisplayName("GET /raid/ succeeds for a scoped credential (previously 403 insufficient_scope)")
        void getAllRaidsSucceeds() {
            final var credential = credential(RAID_AU_GROUP_ID, "data api - list");
            final var api = credentialRaidApi(credential);

            assertThat(api.findAllRaids(null, null, null).getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("POST /raid/ mints into the credential's own service point")
        void mintMintsIntoOwnServicePoint() {
            final var credential = credential(RAID_AU_GROUP_ID, "data api - mint");
            final var api = credentialRaidApi(credential);

            final var minted = api.mintRaid(APIFixtures.newCreateRequest()).getBody();

            assertThat(minted).isNotNull();
            assertThat(minted.getIdentifier().getOwner().getServicePoint().longValue())
                    .isEqualTo(raidAuServicePointId());
        }

        @Test
        @DisplayName("GET/PUT/PATCH succeed on the credential's own minted record")
        void readUpdateAndPatchSucceedOnOwnRecord() {
            final var credential = credential(RAID_AU_GROUP_ID, "data api - crud");
            final var api = credentialRaidApi(credential);

            final var minted = api.mintRaid(APIFixtures.newCreateRequest()).getBody();
            assertThat(minted).isNotNull();
            final var handle = new Handle(minted.getIdentifier().getId());

            final var read = api.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
            assertThat(read).isNotNull();

            final var updateRequest = mapReadToUpdate(read);
            final var updatedTitle = updateRequest.getTitle().get(0).getText() + " updated by credential";
            updateRequest.getTitle().get(0).setText(updatedTitle);

            final var updated = api.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assertThat(updated).isNotNull();
            assertThat(updated.getTitle().get(0).getText()).isEqualTo(updatedTitle);

            final var contributor = updated.getContributor().get(0);
            contributor.setId(REAL_TEST_ORCID);
            contributor.setStatus("AUTHENTICATED");
            final var patchRequest = new RaidPatchRequest().addContributorItem(contributor);

            final var patched = api.patchRaid(handle.getPrefix(), handle.getSuffix(), patchRequest).getBody();
            assertThat(patched).isNotNull();
            assertThat(patched.getContributor().get(0).getId()).isEqualTo(REAL_TEST_ORCID);
        }

        @Test
        @DisplayName("a raid owned by a different group's service point is denied to this credential")
        void deniedForRaidOwnedByAnotherServicePoint() {
            // Paired with readUpdateAndPatchSucceedOnOwnRecord above: that test proves the same kind
            // of fixture (default access type, default fixture data) IS readable by its owning
            // service point, so a denial here isolates the tenancy signal specifically - it can't be
            // explained away by the record's embargo/access-type, only by service point ownership.
            //
            // Minted by the default fixture user - a plain service-point-user of the "raid-au"
            // group / service point 20000000.
            final var minted = raidApi.mintRaid(createRequest).getBody();
            assertThat(minted).isNotNull();
            final var handle = new Handle(minted.getIdentifier().getId());

            final var credential = credential(RAID_AU_REGISTRY_2_GROUP_ID, "data api - other group");
            final var api = credentialRaidApi(credential);

            // Assert 403 specifically rather than via the shared assertDenied(401-or-403) helper: a
            // 401 here would mean the token was never accepted at all, a different bug from the
            // tenancy-isolation failure this test actually targets.
            assertThatThrownBy(() -> api.findRaidByName(handle.getPrefix(), handle.getSuffix()))
                    .isInstanceOf(FeignException.class)
                    .satisfies(e -> assertThat(((FeignException) e).status())
                            .describedAs("expected a tenancy-isolation denial, not an authentication failure")
                            .isEqualTo(403));
        }

        @Test
        @DisplayName("a non-open-access raid owned by the credential's own service point still " +
                "appears in GET /raid/ (RAID-877 truncation guard)")
        void nonOpenAccessRaidOwnedByOwnServicePointIsNotTruncated() {
            // The v2 AccessTypeIdEnum only accepts open or embargoed (there is no v2 "closed"
            // value - CLOSED_ACCESS_TYPE is a legacy-schema constant the v2 mint endpoint rejects
            // with an IllegalArgumentException), so this uses embargoed - APIFixtures.newCreateRequest()'s
            // default - which is exactly as good a guard: RaidRepository.findAllViewable's
            // "OPEN_ACCESS_LEGACY_ID, OPEN_ACCESS_COAR_ID" allow-list for isServicePointUser=false
            // excludes it the same way it would exclude a closed record.
            final var credential = credential(RAID_AU_GROUP_ID, "data api - embargoed");
            final var api = credentialRaidApi(credential);

            final var minted = api.mintRaid(APIFixtures.newCreateRequest()).getBody();
            assertThat(minted).isNotNull();
            assertThat(minted.getAccess().getType().getId())
                    .describedAs("sanity check: this fixture's default access type must not be in " +
                            "the open-access allow-list, or this test would not be exercising the guard")
                    .isEqualTo(AccessTypeIdEnum.fromValue(EMBARGOED_ACCESS_TYPE));

            final var listed = api.findAllRaids(null, null, null).getBody();
            assertThat(listed).isNotNull();
            assertThat(listed)
                    .describedAs("mechanism #3 (RaidIngestService.isServicePointUser) must be true for " +
                            "this credential, or its own non-open-access records are silently truncated " +
                            "out of GET /raid/ instead of the caller seeing an error")
                    .extracting(raid -> raid.getIdentifier().getId())
                    .contains(minted.getIdentifier().getId());
        }
    }
}
