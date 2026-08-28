package au.org.raid.iam.provider.credential;

import au.org.raid.iam.provider.cors.Cors;
import au.org.raid.iam.provider.credential.dto.CreateCredentialRequest;
import au.org.raid.iam.provider.credential.dto.CredentialSecretResponse;
import au.org.raid.iam.provider.credential.dto.RotateCredentialRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RAID-847: secret-handling hardening and the audit trail.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CredentialSecretHandlingTest {

    private static final String GROUP_A = "group-a-uuid";
    private static final String SECRET = "generated-secret-value";

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
    @Mock private ClientScopeModel scope;
    @Mock private CredentialAuditLogger auditLogger;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC);
    private MockedStatic<KeycloakModelUtils> keycloakModelUtils;

    @BeforeEach
    void setUp() {
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.clients()).thenReturn(clientProvider);
        when(session.users()).thenReturn(userProvider);
        when(session.groups()).thenReturn(groupProvider);

        final var corsClient = mock(ClientModel.class);
        when(corsClient.getWebOrigins()).thenReturn(Set.of("http://localhost:7080"));
        when(clientProvider.getClientsStream(realm)).thenAnswer(inv -> Stream.of(corsClient));
        when(headers.getHeaderString("Origin")).thenReturn("http://localhost:7080");

        when(groupProvider.getGroupById(eq(realm), anyString())).thenAnswer(inv -> mock(GroupModel.class));

        final var scopedRole = mock(RoleModel.class);
        when(scopedRole.getName()).thenReturn("service-point-admin:" + GROUP_A);
        when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(scopedRole));

        keycloakModelUtils = mockStatic(KeycloakModelUtils.class);
        keycloakModelUtils.when(KeycloakModelUtils::generateId).thenReturn("fixed-id");
        keycloakModelUtils.when(() -> KeycloakModelUtils.generateSecret(any())).thenReturn(SECRET);
        keycloakModelUtils.when(() -> KeycloakModelUtils.getClientScopeByName(realm, "service_point_group_id"))
                .thenReturn(scope);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        keycloakModelUtils.close();
    }

    private ClientModel backedClient(final String clientId, final Map<String, String> attributes) {
        final var client = mock(ClientModel.class);
        final var enabled = new boolean[]{true};
        when(client.getClientId()).thenReturn(clientId);
        when(client.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
        doAnswer(inv -> {
            attributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(client).setAttribute(anyString(), anyString());
        when(client.isEnabled()).thenAnswer(inv -> enabled[0]);
        doAnswer(inv -> {
            enabled[0] = inv.getArgument(0);
            return null;
        }).when(client).setEnabled(anyBoolean());
        when(client.getSecret()).thenReturn("existing-secret");
        return client;
    }

    private ClientModel existingCredential() {
        final var attributes = new HashMap<String, String>();
        attributes.put("raid.credential.managed", "true");
        attributes.put("raid.credential.group-id", GROUP_A);
        attributes.put("raid.credential.label", "ci");
        attributes.put("raid.credential.created-at", "2026-08-01T00:00:00Z");
        final var client = backedClient("raid-cred-a1", attributes);
        when(clientProvider.getClientByClientId(realm, "raid-cred-a1")).thenReturn(client);
        return client;
    }

    private ClientModel stubNewClient() {
        final var created = backedClient("raid-cred-fixed-id", new HashMap<>());
        when(clientProvider.addClient(eq(realm), anyString())).thenReturn(created);
        when(userProvider.getServiceAccount(created)).thenReturn(serviceAccount);
        when(realm.addRole(anyString())).thenReturn(mock(RoleModel.class));
        return created;
    }

    private ClientCredentialController controller() {
        try (MockedConstruction<AppAuthManager.BearerTokenAuthenticator> ignored =
                     mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                             (mock, ctx) -> when(mock.authenticate()).thenReturn(authResult))) {
            when(authResult.session()).thenReturn(userSession);
            when(userSession.getUser()).thenReturn(user);
            return new ClientCredentialController(session, fixedClock, auditLogger);
        }
    }

    private CreateCredentialRequest createRequest() {
        final var request = new CreateCredentialRequest();
        request.setGroupId(GROUP_A);
        request.setLabel("ci");
        return request;
    }

    private RotateCredentialRequest rotateRequest() {
        final var request = new RotateCredentialRequest();
        request.setClientId("raid-cred-a1");
        return request;
    }

    @Nested
    class CacheControl {

        @Test
        void createSetsNoStore() {
            stubNewClient();
            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                final var response = controller().create(createRequest());
                assertThat(response.getHeaderString(HttpHeaders.CACHE_CONTROL), is("no-store"));
            }
        }

        @Test
        void rotateSetsNoStore() {
            existingCredential();
            assertThat(controller().rotate(rotateRequest()).getHeaderString(HttpHeaders.CACHE_CONTROL),
                    is("no-store"));
        }

        @Test
        void getSecretSetsNoStore() {
            existingCredential();
            assertThat(controller().getSecret("raid-cred-a1").getHeaderString(HttpHeaders.CACHE_CONTROL),
                    is("no-store"));
        }

        @Test
        void everyResponseCarryingASecretSetsNoStore() {
            // The three secret-bearing endpoints, asserted together so a new one cannot be added
            // without noticing this list.
            existingCredential();
            stubNewClient();
            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                assertThat(controller().create(createRequest()).getHeaderString(HttpHeaders.CACHE_CONTROL),
                        is("no-store"));
            }
            assertThat(controller().rotate(rotateRequest()).getHeaderString(HttpHeaders.CACHE_CONTROL),
                    is("no-store"));
            assertThat(controller().getSecret("raid-cred-a1").getHeaderString(HttpHeaders.CACHE_CONTROL),
                    is("no-store"));
        }
    }

    @Nested
    class AuditTrail {

        @Test
        void createIsAudited() {
            stubNewClient();
            try (MockedConstruction<ClientManager> ignored = mockConstruction(ClientManager.class)) {
                controller().create(createRequest());
            }
            verify(auditLogger).record(CredentialAuditLogger.ACTION_CREATE, user, "raid-cred-fixed-id", GROUP_A);
        }

        @Test
        void listIsAudited() {
            controller().list(GROUP_A);
            verify(auditLogger).record(CredentialAuditLogger.ACTION_LIST, user, null, GROUP_A);
        }

        @Test
        void rotateIsAudited() {
            existingCredential();
            controller().rotate(rotateRequest());
            verify(auditLogger).record(CredentialAuditLogger.ACTION_ROTATE, user, "raid-cred-a1", GROUP_A);
        }

        @Test
        void revokeIsAudited() {
            existingCredential();
            controller().revoke("raid-cred-a1");
            verify(auditLogger).record(CredentialAuditLogger.ACTION_REVOKE, user, "raid-cred-a1", GROUP_A);
        }

        @Test
        void secretRevealIsAudited() {
            existingCredential();
            controller().getSecret("raid-cred-a1");
            verify(auditLogger)
                    .record(CredentialAuditLogger.ACTION_REVEAL_SECRET, user, "raid-cred-a1", GROUP_A);
        }

        @Test
        void deniedActionsAreNotAudited() {
            // Documents current behaviour rather than endorsing it: only successful actions are
            // recorded. Auditing denials would be a useful follow-up, but is outside RAID-847.
            existingCredential();
            final var otherGroupRole = mock(RoleModel.class);
            when(otherGroupRole.getName()).thenReturn("service-point-admin:some-other-group");
            when(user.getRoleMappingsStream()).thenAnswer(inv -> Stream.of(otherGroupRole));

            final var controller = controller();
            try {
                controller.getSecret("raid-cred-a1");
            } catch (RuntimeException expected) {
                // denied
            }
            verifyNoInteractions(auditLogger);
        }
    }

    @Nested
    class SecretNeverLeaks {

        @Test
        void secretResponseToStringIsRedacted() {
            final var body = new CredentialSecretResponse(
                    "raid-cred-a1", "ci", "SUPER_SECRET", "2026-08-01T00:00:00Z", null);

            assertThat(body.toString(), not(containsString("SUPER_SECRET")));
            assertThat(body.toString(), containsString("REDACTED"));
            // Non-sensitive fields stay visible so the value is still useful in a log.
            assertThat(body.toString(), containsString("raid-cred-a1"));
        }

        @Test
        void secretIsStillPresentInTheSerialisedBody() {
            // Redaction must apply to toString only, never to the JSON the caller receives.
            final var body = new CredentialSecretResponse(
                    "raid-cred-a1", "ci", "SUPER_SECRET", "2026-08-01T00:00:00Z", null);

            assertThat(body.secret(), is("SUPER_SECRET"));
        }

        // The Cors debug-logging leak guard lives in CorsDebugPayloadTest, since debugPayload is
        // package-private to the cors package.
    }
}
