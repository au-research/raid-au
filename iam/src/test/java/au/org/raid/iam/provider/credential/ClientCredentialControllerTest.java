package au.org.raid.iam.provider.credential;

import au.org.raid.iam.provider.credential.dto.CreateCredentialRequest;
import au.org.raid.iam.provider.credential.dto.RotateCredentialRequest;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientCredentialControllerTest {

    private static final String GROUP_A = "group-a-uuid";
    private static final String GROUP_B = "group-b-uuid";
    private static final String NEW_SECRET = "generated-secret-value";
    private static final String FIXED_NOW = "2026-08-27T00:00:00Z";

    private static final String MANAGED_ATTRIBUTE = "raid.credential.managed";
    private static final String GROUP_ID_ATTRIBUTE = "raid.credential.group-id";
    private static final String LABEL_ATTRIBUTE = "raid.credential.label";
    private static final String CREATED_AT_ATTRIBUTE = "raid.credential.created-at";
    private static final String ROTATED_AT_ATTRIBUTE = "raid.credential.rotated-at";

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private UserModel user;
    @Mock private UserProvider userProvider;
    @Mock private GroupProvider groupProvider;
    @Mock private ClientProvider clientProvider;
    @Mock private HttpHeaders headers;
    @Mock private AuthenticationManager.AuthResult authResult;
    @Mock private UserSessionModel userSession;
    @Mock private UserModel serviceAccount;
    @Mock private ClientScopeModel servicePointGroupIdScope;

    private final Clock fixedClock = Clock.fixed(Instant.parse(FIXED_NOW), ZoneOffset.UTC);
    private final List<ClientModel> realmClients = new ArrayList<>();

    private MockedStatic<KeycloakModelUtils> keycloakModelUtils;

    @BeforeEach
    void setUp() {
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.clients()).thenReturn(clientProvider);
        when(session.users()).thenReturn(userProvider);
        when(session.groups()).thenReturn(groupProvider);

        // Cors derives allowed origins from the realm's clients, so the stream must also carry a
        // client with web origins. Credential mocks return null web origins and are skipped by Cors,
        // while this one has no managed attribute and is skipped by the controller.
        final var corsClient = mock(ClientModel.class);
        when(corsClient.getWebOrigins()).thenReturn(Set.of("http://localhost:7080"));
        when(headers.getHeaderString("Origin")).thenReturn("http://localhost:7080");

        // thenAnswer, not thenReturn: a Stream is single-use and this is consumed more than once
        // per request (Cors plus the controller's own filtering).
        when(clientProvider.getClientsStream(realm))
                .thenAnswer(inv -> Stream.concat(Stream.of(corsClient), realmClients.stream()));

        when(groupProvider.getGroupById(eq(realm), anyString())).thenAnswer(inv -> {
            final var group = mock(GroupModel.class);
            when(group.getId()).thenReturn(inv.getArgument(1));
            return group;
        });

        keycloakModelUtils = mockStatic(KeycloakModelUtils.class);
        keycloakModelUtils.when(() -> KeycloakModelUtils.generateId()).thenReturn("fixed-id");
        keycloakModelUtils.when(() -> KeycloakModelUtils.generateSecret(any())).thenReturn(NEW_SECRET);
        keycloakModelUtils
                .when(() -> KeycloakModelUtils.getClientScopeByName(realm, "service_point_group_id"))
                .thenReturn(servicePointGroupIdScope);
    }

    @AfterEach
    void tearDown() {
        keycloakModelUtils.close();
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * A ClientModel mock whose attribute map is real, so the controller's setAttribute calls can be
     * asserted on afterwards.
     */
    private ClientModel credentialClient(final String clientId, final String groupId, final boolean enabled) {
        final var attributes = new HashMap<String, String>();
        attributes.put(MANAGED_ATTRIBUTE, "true");
        attributes.put(GROUP_ID_ATTRIBUTE, groupId);
        attributes.put(LABEL_ATTRIBUTE, "label for " + clientId);
        attributes.put(CREATED_AT_ATTRIBUTE, "2026-08-01T00:00:00Z");
        return backedClient(clientId, attributes, enabled);
    }

    private ClientModel backedClient(final String clientId, final Map<String, String> attributes, final boolean enabled) {
        final var client = mock(ClientModel.class);
        final var enabledHolder = new boolean[]{enabled};
        when(client.getClientId()).thenReturn(clientId);
        when(client.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
        when(client.getAttributes()).thenReturn(attributes);
        doAnswer(inv -> {
            attributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(client).setAttribute(anyString(), anyString());
        when(client.isEnabled()).thenAnswer(inv -> enabledHolder[0]);
        doAnswer(inv -> {
            enabledHolder[0] = inv.getArgument(0);
            return null;
        }).when(client).setEnabled(anyBoolean());
        when(client.getSecret()).thenReturn("existing-secret");
        return client;
    }

    private ClientModel registerCredential(final String clientId, final String groupId, final boolean enabled) {
        final var client = credentialClient(clientId, groupId, enabled);
        realmClients.add(client);
        when(clientProvider.getClientByClientId(realm, clientId)).thenReturn(client);
        return client;
    }

    private ClientModel stubNewClient() {
        final var created = backedClient("raid-cred-fixed-id", new HashMap<>(), true);
        when(clientProvider.addClient(eq(realm), anyString())).thenReturn(created);
        when(userProvider.getServiceAccount(created)).thenReturn(serviceAccount);
        when(realm.getRole(anyString())).thenReturn(null);
        final var scopedUserRole = mock(RoleModel.class);
        when(realm.addRole(anyString())).thenReturn(scopedUserRole);
        return created;
    }

    private ClientCredentialController controller() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(authResult))) {
            when(authResult.session()).thenReturn(userSession);
            when(userSession.getUser()).thenReturn(user);
            return new ClientCredentialController(session, fixedClock);
        }
    }

    private ClientCredentialController unauthenticatedController() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(null))) {
            return new ClientCredentialController(session, fixedClock);
        }
    }

    private void givenRoles(final String... roleNames) {
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(roleNames).map(name -> {
            final var role = mock(RoleModel.class);
            when(role.getName()).thenReturn(name);
            return role;
        }));
    }

    private void givenScopedAdminOf(final String groupId) {
        givenRoles("service-point-admin:" + groupId);
    }

    private CreateCredentialRequest createRequest(final String groupId, final String label) {
        final var request = new CreateCredentialRequest();
        request.setGroupId(groupId);
        request.setLabel(label);
        return request;
    }

    private RotateCredentialRequest rotateRequest(final String clientId) {
        final var request = new RotateCredentialRequest();
        request.setClientId(clientId);
        return request;
    }

    // ---------------------------------------------------------------- tests

    @Nested
    class Authentication {

        @Test
        void createReturns401WhenUnauthenticated() {
            final var response = unauthenticatedController().create(createRequest(GROUP_A, "ci"));
            assertThat(response.getStatus(), is(401));
        }

        @Test
        void listReturns401WhenUnauthenticated() {
            assertThat(unauthenticatedController().list(GROUP_A).getStatus(), is(401));
        }

        @Test
        void rotateReturns401WhenUnauthenticated() {
            assertThat(unauthenticatedController().rotate(rotateRequest("x")).getStatus(), is(401));
        }

        @Test
        void revokeReturns401WhenUnauthenticated() {
            assertThat(unauthenticatedController().revoke("x").getStatus(), is(401));
        }

        @Test
        void getSecretReturns401WhenUnauthenticated() {
            assertThat(unauthenticatedController().getSecret("x").getStatus(), is(401));
        }
    }

    @Nested
    class Create {

        @Test
        void scopedAdminCreatesCredentialAndReceivesSecret() {
            givenScopedAdminOf(GROUP_A);
            final var created = stubNewClient();

            try (MockedConstruction<ClientManager> clientManager = mockConstruction(ClientManager.class)) {
                final var response = controller().create(createRequest(GROUP_A, "ci pipeline"));

                assertThat(response.getStatus(), is(201));
                assertThat(response.getEntity().toString(), containsString(NEW_SECRET));
                assertThat(response.getEntity().toString(), containsString("ci pipeline"));
                verify(clientManager.constructed().get(0)).enableServiceAccount(created);
            }

            assertThat(created.getAttribute(MANAGED_ATTRIBUTE), is("true"));
            assertThat(created.getAttribute(GROUP_ID_ATTRIBUTE), is(GROUP_A));
            assertThat(created.getAttribute(LABEL_ATTRIBUTE), is("ci pipeline"));
            assertThat(created.getAttribute(CREATED_AT_ATTRIBUTE), is(FIXED_NOW));
        }

        @Test
        void newCredentialIsConfidentialWithServiceAccountAndNoInteractiveFlows() {
            givenScopedAdminOf(GROUP_A);
            final var created = stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            verify(created).setPublicClient(false);
            verify(created).setServiceAccountsEnabled(true);
            verify(created).setStandardFlowEnabled(false);
            verify(created).setDirectAccessGrantsEnabled(false);
        }

        @Test
        void newCredentialHasFullScopeAllowedSoItsScopedRoleReachesTheToken() {
            // addClient() leaves fullScopeAllowed false, and while false Keycloak filters the token
            // down to the client's explicit scope mappings, stripping the scoped usage role. The
            // credential then authenticates with no roles at all. Caught in RAID-846 only by using
            // a real credential end to end.
            givenScopedAdminOf(GROUP_A);
            final var created = stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            verify(created).setFullScopeAllowed(true);
        }

        @Test
        void newCredentialGetsServicePointGroupIdScopeAndActiveGroupIdAttribute() {
            givenScopedAdminOf(GROUP_A);
            final var created = stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            // Without both of these the token carries no service_point_group_id claim at all.
            verify(created).addClientScope(servicePointGroupIdScope, true);
            verify(serviceAccount).setAttribute("activeGroupId", List.of(GROUP_A));
        }

        @Test
        void clientManagerIsConstructedWithARealmManager() {
            // ClientManager's no-arg constructor leaves its realmManager field null, and
            // enableServiceAccount dereferences it, so `new ClientManager()` compiles but throws
            // NullPointerException at runtime. Mocking the construction hides that, so assert the
            // constructor argument instead. This caught a real 500 in RAID-846.
            givenScopedAdminOf(GROUP_A);
            stubNewClient();
            final var constructorArgs = new ArrayList<List<?>>();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class,
                    (mock, ctx) -> constructorArgs.add(ctx.arguments()))) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            assertThat(constructorArgs, hasSize(1));
            assertThat(constructorArgs.get(0), hasSize(1));
            assertThat(constructorArgs.get(0).get(0), instanceOf(RealmManager.class));
        }

        @Test
        void newCredentialInheritsTheRealmsDefaultClientScopes() {
            // addClient() is a raw model create and does not apply realm default scopes, unlike the
            // Admin API path. Without them the client has no `roles` scope, so realm roles granted
            // to its service account never reach realm_access.roles and the credential is silently
            // useless. This caught a real defect in RAID-846.
            givenScopedAdminOf(GROUP_A);
            final var created = stubNewClient();
            final var rolesScope = mock(ClientScopeModel.class);
            final var basicScope = mock(ClientScopeModel.class);
            when(realm.getDefaultClientScopesStream(true))
                    .thenAnswer(inv -> Stream.of(rolesScope, basicScope));

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            verify(created).addClientScope(rolesScope, true);
            verify(created).addClientScope(basicScope, true);
            // And still the non-default service point scope on top.
            verify(created).addClientScope(servicePointGroupIdScope, true);
        }

        @Test
        void serviceAccountIsGrantedScopedUsageRoleOnly() {
            givenScopedAdminOf(GROUP_A);
            stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            verify(realm).addRole("service-point-user:" + GROUP_A);
            verify(serviceAccount).grantRole(any(RoleModel.class));
            verify(serviceAccount, never()).grantRole(argThat(role ->
                    role != null && role.getName() != null && role.getName().contains("admin")));
        }

        @Test
        void reusesExistingScopedUsageRoleRatherThanRecreatingIt() {
            givenScopedAdminOf(GROUP_A);
            stubNewClient();
            final var existing = mock(RoleModel.class);
            when(realm.getRole("service-point-user:" + GROUP_A)).thenReturn(existing);

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest(GROUP_A, "ci"));
            }

            verify(realm, never()).addRole("service-point-user:" + GROUP_A);
            verify(serviceAccount).grantRole(existing);
        }

        @Test
        void operatorCanCreateForAnyServicePoint() {
            givenRoles("operator");
            stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                assertThat(controller().create(createRequest(GROUP_B, "ci")).getStatus(), is(201));
            }
        }

        @Test
        void adminOfOneServicePointCannotCreateForAnother() {
            givenScopedAdminOf(GROUP_A);
            stubNewClient();

            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.create(createRequest(GROUP_B, "ci")));
            verify(clientProvider, never()).addClient(any(), anyString());
        }

        @Test
        void flatGroupAdminIsRejectedWithNoFallback() {
            givenRoles("group-admin", "service-point-user");
            stubNewClient();

            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.create(createRequest(GROUP_A, "ci")));
            verify(clientProvider, never()).addClient(any(), anyString());
        }

        @Test
        void userWithNoRelevantRolesIsRejected() {
            givenRoles();
            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.create(createRequest(GROUP_A, "ci")));
        }

        @Test
        void missingGroupIdIsRejected() {
            givenScopedAdminOf(GROUP_A);
            assertThat(controller().create(createRequest(null, "ci")).getStatus(), is(400));
        }

        @Test
        void missingLabelIsRejected() {
            givenScopedAdminOf(GROUP_A);
            assertThat(controller().create(createRequest(GROUP_A, "  ")).getStatus(), is(400));
        }

        @Test
        void unknownServicePointIsRejected() {
            givenScopedAdminOf(GROUP_A);
            when(groupProvider.getGroupById(realm, GROUP_A)).thenReturn(null);
            assertThat(controller().create(createRequest(GROUP_A, "ci")).getStatus(), is(404));
        }

        @Test
        void missingClientScopeFailsClosed() {
            givenScopedAdminOf(GROUP_A);
            stubNewClient();
            keycloakModelUtils
                    .when(() -> KeycloakModelUtils.getClientScopeByName(realm, "service_point_group_id"))
                    .thenReturn(null);

            final var controller = controller();
            assertThrows(InternalServerErrorException.class,
                    () -> controller.create(createRequest(GROUP_A, "ci")));
        }

        @Test
        void missingServiceAccountFailsClosed() {
            givenScopedAdminOf(GROUP_A);
            final var created = stubNewClient();
            when(userProvider.getServiceAccount(created)).thenReturn(null);

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                final var controller = controller();
                assertThrows(InternalServerErrorException.class,
                        () -> controller.create(createRequest(GROUP_A, "ci")));
            }
        }
    }

    @Nested
    class Cap {

        @Test
        void creatingAtTheCapIsRejectedWith409() {
            givenScopedAdminOf(GROUP_A);
            for (var i = 0; i < ClientCredentialController.MAX_CREDENTIALS_PER_SERVICE_POINT; i++) {
                registerCredential("raid-cred-" + i, GROUP_A, true);
            }

            final var response = controller().create(createRequest(GROUP_A, "one too many"));

            assertThat(response.getStatus(), is(409));
            assertThat(response.getEntity().toString(),
                    containsString(String.valueOf(ClientCredentialController.MAX_CREDENTIALS_PER_SERVICE_POINT)));
            verify(clientProvider, never()).addClient(any(), anyString());
        }

        @Test
        void creatingJustBelowTheCapSucceeds() {
            givenScopedAdminOf(GROUP_A);
            for (var i = 0; i < ClientCredentialController.MAX_CREDENTIALS_PER_SERVICE_POINT - 1; i++) {
                registerCredential("raid-cred-" + i, GROUP_A, true);
            }
            stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                assertThat(controller().create(createRequest(GROUP_A, "last slot")).getStatus(), is(201));
            }
        }

        @Test
        void revokedCredentialsDoNotCountTowardsTheCap() {
            givenScopedAdminOf(GROUP_A);
            for (var i = 0; i < ClientCredentialController.MAX_CREDENTIALS_PER_SERVICE_POINT; i++) {
                // One of them revoked, so a slot is free.
                registerCredential("raid-cred-" + i, GROUP_A, i != 0);
            }
            stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                assertThat(controller().create(createRequest(GROUP_A, "reused slot")).getStatus(), is(201));
            }
        }

        @Test
        void anotherServicePointsCredentialsDoNotCountTowardsTheCap() {
            givenScopedAdminOf(GROUP_A);
            for (var i = 0; i < ClientCredentialController.MAX_CREDENTIALS_PER_SERVICE_POINT; i++) {
                registerCredential("raid-cred-b-" + i, GROUP_B, true);
            }
            stubNewClient();

            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                assertThat(controller().create(createRequest(GROUP_A, "ci")).getStatus(), is(201));
            }
        }
    }

    @Nested
    class ListCredentials {

        @Test
        void listsOnlyTheServicePointsOwnCredentials() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-a1", GROUP_A, true);
            registerCredential("raid-cred-a2", GROUP_A, true);
            registerCredential("raid-cred-b1", GROUP_B, true);

            final var body = controller().list(GROUP_A).getEntity().toString();

            assertThat(body, containsString("raid-cred-a1"));
            assertThat(body, containsString("raid-cred-a2"));
            assertThat(body, not(containsString("raid-cred-b1")));
        }

        @Test
        void listIncludesRevokedCredentialsFlaggedAsDisabled() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-revoked", GROUP_A, false);

            final var body = controller().list(GROUP_A).getEntity().toString();

            assertThat(body, containsString("raid-cred-revoked"));
            assertThat(body, containsString("\"enabled\":false"));
        }

        @Test
        void listNeverIncludesUnmanagedRealmClients() {
            givenScopedAdminOf(GROUP_A);
            final var appClient = backedClient("raid-api", new HashMap<>(), true);
            realmClients.add(appClient);

            final var body = controller().list(GROUP_A).getEntity().toString();

            assertThat(body, not(containsString("raid-api")));
        }

        @Test
        void adminOfOneServicePointCannotListAnother() {
            givenScopedAdminOf(GROUP_A);
            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.list(GROUP_B));
        }

        @Test
        void missingGroupIdIsRejected() {
            givenScopedAdminOf(GROUP_A);
            assertThat(controller().list(null).getStatus(), is(400));
        }
    }

    @Nested
    class Rotate {

        @Test
        void rotateIssuesNewSecretAndRecordsRotationTime() {
            givenScopedAdminOf(GROUP_A);
            final var client = registerCredential("raid-cred-a1", GROUP_A, true);

            final var response = controller().rotate(rotateRequest("raid-cred-a1"));

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity().toString(), containsString(NEW_SECRET));
            assertThat(client.getAttribute(ROTATED_AT_ATTRIBUTE), is(FIXED_NOW));
        }

        @Test
        void rotateLeavesLabelAndOwnerUnchanged() {
            givenScopedAdminOf(GROUP_A);
            final var client = registerCredential("raid-cred-a1", GROUP_A, true);
            final var labelBefore = client.getAttribute(LABEL_ATTRIBUTE);

            controller().rotate(rotateRequest("raid-cred-a1"));

            assertThat(client.getAttribute(LABEL_ATTRIBUTE), is(labelBefore));
            assertThat(client.getAttribute(GROUP_ID_ATTRIBUTE), is(GROUP_A));
        }

        @Test
        void rotatingRevokedCredentialIsRejectedWith409() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-a1", GROUP_A, false);

            final var response = controller().rotate(rotateRequest("raid-cred-a1"));

            assertThat(response.getStatus(), is(409));
            keycloakModelUtils.verify(() -> KeycloakModelUtils.generateSecret(any()), never());
        }

        @Test
        void adminOfOneServicePointCannotRotateAnothers() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-b1", GROUP_B, true);

            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.rotate(rotateRequest("raid-cred-b1")));
        }

        @Test
        void unknownCredentialIsNotFound() {
            givenScopedAdminOf(GROUP_A);
            assertThat(controller().rotate(rotateRequest("nope")).getStatus(), is(404));
        }

        @Test
        void unmanagedClientIsTreatedAsNotFound() {
            givenScopedAdminOf(GROUP_A);
            final var appClient = backedClient("raid-api", new HashMap<>(), true);
            when(clientProvider.getClientByClientId(realm, "raid-api")).thenReturn(appClient);

            // Must not be usable to probe or mutate the realm's own clients.
            assertThat(controller().rotate(rotateRequest("raid-api")).getStatus(), is(404));
            verify(appClient, never()).setEnabled(anyBoolean());
        }
    }

    @Nested
    class Revoke {

        @Test
        void revokeDisablesTheCredential() {
            givenScopedAdminOf(GROUP_A);
            final var client = registerCredential("raid-cred-a1", GROUP_A, true);

            final var response = controller().revoke("raid-cred-a1");

            assertThat(response.getStatus(), is(200));
            verify(client).setEnabled(false);
            assertThat(client.isEnabled(), is(false));
        }

        @Test
        void revokeIsIdempotent() {
            givenScopedAdminOf(GROUP_A);
            final var client = registerCredential("raid-cred-a1", GROUP_A, false);

            final var response = controller().revoke("raid-cred-a1");

            assertThat(response.getStatus(), is(200));
            verify(client, never()).setEnabled(anyBoolean());
        }

        @Test
        void revokeNeverDeletesTheClient() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-a1", GROUP_A, true);

            controller().revoke("raid-cred-a1");

            verify(clientProvider, never()).removeClient(any(), anyString());
        }

        @Test
        void adminOfOneServicePointCannotRevokeAnothers() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-b1", GROUP_B, true);

            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.revoke("raid-cred-b1"));
        }

        @Test
        void operatorCanRevokeAnyServicePointsCredential() {
            givenRoles("operator");
            final var client = registerCredential("raid-cred-b1", GROUP_B, true);

            assertThat(controller().revoke("raid-cred-b1").getStatus(), is(200));
            verify(client).setEnabled(false);
        }
    }

    @Nested
    class GetSecret {

        @Test
        void returnsCurrentSecretToOwningServicePointAdmin() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-a1", GROUP_A, true);

            final var response = controller().getSecret("raid-cred-a1");

            assertThat(response.getStatus(), is(200));
            assertThat(response.getEntity().toString(), containsString("existing-secret"));
        }

        @Test
        void adminOfOneServicePointCannotReadAnothersSecret() {
            givenScopedAdminOf(GROUP_A);
            registerCredential("raid-cred-b1", GROUP_B, true);

            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.getSecret("raid-cred-b1"));
        }

        @Test
        void flatGroupAdminCannotReadASecret() {
            givenRoles("group-admin", "service-point-user");
            registerCredential("raid-cred-a1", GROUP_A, true);

            final var controller = controller();
            assertThrows(NotAuthorizedException.class, () -> controller.getSecret("raid-cred-a1"));
        }

        @Test
        void unknownCredentialIsNotFound() {
            givenScopedAdminOf(GROUP_A);
            assertThat(controller().getSecret("nope").getStatus(), is(404));
        }
    }

    @Nested
    class Preflight {

        @Test
        void collectionPreflightAdvertisesTheCollectionMethods() {
            final var response = controller().preflight();
            final var allowed = response.getHeaderString("Access-Control-Allow-Methods");
            assertThat(allowed, allOf(containsString("GET"), containsString("POST"), containsString("DELETE")));
        }

        @Test
        void rotatePreflightAdvertisesPost() {
            assertThat(controller().rotatePreflight().getHeaderString("Access-Control-Allow-Methods"),
                    containsString("POST"));
        }

        @Test
        void secretPreflightAdvertisesGet() {
            assertThat(controller().secretPreflight().getHeaderString("Access-Control-Allow-Methods"),
                    containsString("GET"));
        }
    }
}
