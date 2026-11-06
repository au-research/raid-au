package au.org.raid.iam.provider.federation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static au.org.raid.iam.provider.federation.FederationHarvestClientProvisioner.Outcome.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAID-982: the federation harvest client is created, and reconciled on every boot, entirely through
 * the model layer.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FederationHarvestClientProvisionerTest {

    private static final FederationHarvestKeySource URL_KEYS =
            FederationHarvestKeySource.url("https://keys.raid.org/federation/jwks.json");
    private static final FederationHarvestKeySource EMBEDDED_KEYS =
            FederationHarvestKeySource.embedded("{\"keys\":[]}");

    @Mock private KeycloakSession session;
    @Mock private RealmModel realm;
    @Mock private ClientProvider clientProvider;
    @Mock private UserProvider userProvider;
    @Mock private RoleModel raidDumper;
    @Mock private RoleModel defaultRole;
    @Mock private UserModel serviceAccount;

    private Map<String, String> attributes;

    @BeforeEach
    void setUp() {
        when(session.clients()).thenReturn(clientProvider);
        when(session.users()).thenReturn(userProvider);
        when(raidDumper.getId()).thenReturn("raid-dumper-id");
        when(raidDumper.getName()).thenReturn("raid-dumper");
        when(defaultRole.getId()).thenReturn("default-roles-id");
        when(realm.getRole("raid-dumper")).thenReturn(raidDumper);
        when(realm.getDefaultRole()).thenReturn(defaultRole);
        when(realm.getDefaultClientScopesStream(true)).thenReturn(Stream.empty());
        when(serviceAccount.getRoleMappingsStream()).thenReturn(Stream.empty());
        when(serviceAccount.getGroupsStream()).thenAnswer(i -> Stream.empty());
        attributes = new HashMap<>();
    }

    /** A client mock that stores its attributes, so reconcile reads back what it wrote. */
    private ClientModel statefulClient() {
        var client = mock(ClientModel.class);
        when(client.getAttribute(anyString())).thenAnswer(i -> attributes.get(i.<String>getArgument(0)));
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1)))
                .when(client).setAttribute(anyString(), anyString());
        doAnswer(i -> attributes.remove(i.<String>getArgument(0))).when(client).removeAttribute(anyString());
        when(client.getAttributes()).thenAnswer(i -> new HashMap<>(attributes));
        return client;
    }

    /** An existing client already configured exactly as the jar wants, in URL mode. */
    private ClientModel upToDateClient() {
        var client = statefulClient();
        attributes.putAll(Map.of(
                "use.jwks.url", "true",
                "jwks.url", URL_KEYS.jwksUrl(),
                "use.jwks.string", "false",
                "token.endpoint.auth.signing.alg", "RS256",
                "access.token.lifespan", "300"));
        when(client.isFullScopeAllowed()).thenReturn(true);
        when(client.getClientAuthenticatorType()).thenReturn("client-jwt");
        when(client.isServiceAccountsEnabled()).thenReturn(true);
        when(userProvider.getServiceAccount(client)).thenReturn(serviceAccount);
        when(serviceAccount.hasDirectRole(raidDumper)).thenReturn(true);
        when(realm.getClientByClientId("raid-federation-harvester")).thenReturn(client);
        return client;
    }

    private MockedConstruction<ClientManager> clientManagerThatEnables(final ClientModel client) {
        return mockConstruction(ClientManager.class, (manager, context) ->
                doAnswer(i -> {
                    when(client.isServiceAccountsEnabled()).thenReturn(true);
                    when(userProvider.getServiceAccount(client)).thenReturn(serviceAccount);
                    return null;
                }).when(manager).enableServiceAccount(client));
    }

    @Test
    void realmWithoutRaidDumperIsLeftAlone() {
        when(realm.getRole("raid-dumper")).thenReturn(null);

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(NOT_A_RAID_REALM));
        verify(clientProvider, never()).addClient(any(), anyString());
        verify(realm, never()).getClientByClientId(anyString());
        verify(realm, never()).addRole(anyString());
        verify(realm, never()).addRole(anyString(), anyString());
    }

    @Test
    void createsTheClientWithSignedJwtFromTheJwksUrl() {
        var client = statefulClient();
        // A real addClient() leaves the standard (browser) flow on; the harvester must not have it.
        when(client.isStandardFlowEnabled()).thenReturn(true);
        var defaultScope = mock(ClientScopeModel.class);
        when(realm.getDefaultClientScopesStream(true)).thenReturn(Stream.of(defaultScope));
        when(clientProvider.addClient(realm, "raid-federation-harvester")).thenReturn(client);

        try (var ignoredRealmManager = mockConstruction(RealmManager.class);
             var clientManagers = clientManagerThatEnables(client)) {
            var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

            assertThat(outcome, is(CREATED));
            verify(clientManagers.constructed().get(0)).enableServiceAccount(client);
        }

        verify(client).setEnabled(true);
        verify(client).addClientScope(defaultScope, true);
        verify(client).setFullScopeAllowed(true);
        verify(client).setClientAuthenticatorType("client-jwt");
        verify(client).setStandardFlowEnabled(false);
        verify(client).updateClient();
        assertThat(attributes, allOf(
                hasEntry("use.jwks.url", "true"),
                hasEntry("jwks.url", "https://keys.raid.org/federation/jwks.json"),
                hasEntry("use.jwks.string", "false"),
                hasEntry("token.endpoint.auth.signing.alg", "RS256"),
                hasEntry("access.token.lifespan", "300")));
        assertThat(attributes, not(hasKey("jwks.string")));
        verify(serviceAccount).grantRole(raidDumper);
    }

    @Test
    void createsNoClientSecret() {
        var client = statefulClient();
        when(clientProvider.addClient(realm, "raid-federation-harvester")).thenReturn(client);

        try (var ignoredRealmManager = mockConstruction(RealmManager.class);
             var ignoredClientManager = clientManagerThatEnables(client)) {
            FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);
        }

        // The real-Keycloak suite (RAID-1044) confirms the stored client has no secret; here the
        // provisioner must never set one.
        verify(client, never()).setSecret(argThat(secret -> secret != null));
    }

    @Test
    void createsTheClientWithTheEmbeddedKeySet() {
        var client = statefulClient();
        when(clientProvider.addClient(realm, "raid-federation-harvester")).thenReturn(client);

        try (var ignoredRealmManager = mockConstruction(RealmManager.class);
             var ignoredClientManager = clientManagerThatEnables(client)) {
            FederationHarvestClientProvisioner.provision(session, realm, EMBEDDED_KEYS);
        }

        assertThat(attributes, allOf(
                hasEntry("use.jwks.url", "false"),
                hasEntry("use.jwks.string", "true"),
                hasEntry("jwks.string", "{\"keys\":[]}")));
        assertThat(attributes, not(hasKey("jwks.url")));
    }

    @Test
    void upToDateClientIsUnchanged() {
        var client = upToDateClient();

        try (var clientManagers = mockConstruction(ClientManager.class)) {
            var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

            assertThat(outcome, is(UNCHANGED));
            assertThat(clientManagers.constructed(), is(empty()));
        }
        verify(clientProvider, never()).addClient(any(), anyString());
        verify(client, never()).setAttribute(anyString(), anyString());
        verify(client, never()).updateClient();
        verify(serviceAccount, never()).grantRole(any());
        verify(serviceAccount, never()).deleteRoleMapping(any());
    }

    @Test
    void neverReenablesAClientTheAgencyDisabled() {
        var client = upToDateClient();
        when(client.isEnabled()).thenReturn(false);
        attributes.put("jwks.url", "https://old.example/jwks.json");

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(client, never()).setEnabled(anyBoolean());
    }

    @Test
    void driftedClientIsPutBack() {
        var client = upToDateClient();
        when(client.isFullScopeAllowed()).thenReturn(false);
        when(client.getClientAuthenticatorType()).thenReturn("client-secret");
        when(client.isDirectAccessGrantsEnabled()).thenReturn(true);
        when(client.isPublicClient()).thenReturn(true);
        attributes.put("access.token.lifespan", "86400");
        attributes.put("jwks.url", "https://old.example/jwks.json");

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(client).setFullScopeAllowed(true);
        verify(client).setClientAuthenticatorType("client-jwt");
        verify(client).setDirectAccessGrantsEnabled(false);
        verify(client).setPublicClient(false);
        assertThat(attributes, hasEntry("access.token.lifespan", "300"));
        assertThat(attributes, hasEntry("jwks.url", "https://keys.raid.org/federation/jwks.json"));
    }

    @Test
    void storedSecretIsCleared() {
        var client = upToDateClient();
        when(client.getSecret()).thenReturn("dormant");

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(client).setSecret(null);
    }

    @Test
    void switchingToTheEmbeddedKeySetDropsTheUrl() {
        upToDateClient();

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, EMBEDDED_KEYS);

        assertThat(outcome, is(UPDATED));
        assertThat(attributes, allOf(
                hasEntry("use.jwks.url", "false"),
                hasEntry("use.jwks.string", "true"),
                hasEntry("jwks.string", "{\"keys\":[]}")));
        assertThat(attributes, not(hasKey("jwks.url")));
    }

    @Test
    void extraRolesOnTheServiceAccountAreRemoved() {
        upToDateClient();
        var operator = mock(RoleModel.class);
        when(operator.getId()).thenReturn("operator-id");
        var clientRole = mock(RoleModel.class);
        when(clientRole.getId()).thenReturn("realm-management-view-users-id");
        when(serviceAccount.getRoleMappingsStream())
                .thenReturn(List.of(raidDumper, defaultRole, operator, clientRole).stream());

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(serviceAccount).deleteRoleMapping(operator);
        verify(serviceAccount).deleteRoleMapping(clientRole);
        verify(serviceAccount, never()).deleteRoleMapping(raidDumper);
        verify(serviceAccount, never()).deleteRoleMapping(defaultRole);
    }

    @Test
    void missingRaidDumperGrantIsRestored() {
        upToDateClient();
        when(serviceAccount.hasDirectRole(raidDumper)).thenReturn(false);

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(serviceAccount).grantRole(raidDumper);
    }

    @Test
    void missingServiceAccountIsReenabled() {
        var client = upToDateClient();
        when(client.isServiceAccountsEnabled()).thenReturn(false);
        when(userProvider.getServiceAccount(client)).thenReturn(null);

        try (var ignoredRealmManager = mockConstruction(RealmManager.class);
             var clientManagers = clientManagerThatEnables(client)) {
            var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

            assertThat(outcome, is(UPDATED));
            verify(clientManagers.constructed().get(0)).enableServiceAccount(client);
        }
    }

    @Test
    void switchingBackToTheUrlDropsTheEmbeddedKeySet() {
        var client = upToDateClient();
        attributes.put("use.jwks.url", "false");
        attributes.remove("jwks.url");
        attributes.put("use.jwks.string", "true");
        attributes.put("jwks.string", "{\"keys\":[]}");

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        assertThat(attributes, allOf(
                hasEntry("use.jwks.url", "true"),
                hasEntry("jwks.url", "https://keys.raid.org/federation/jwks.json"),
                hasEntry("use.jwks.string", "false")));
        assertThat(attributes, not(hasKey("jwks.string")));
        verify(client).updateClient();
    }

    @Test
    void bearerOnlyAndImplicitFlowDriftIsPutBack() {
        var client = upToDateClient();
        when(client.isBearerOnly()).thenReturn(true);
        when(client.isImplicitFlowEnabled()).thenReturn(true);

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(client).setBearerOnly(false);
        verify(client).setImplicitFlowEnabled(false);
    }

    @Test
    void dormantCertificateAndRotatedSecretAreRemoved() {
        var client = upToDateClient();
        attributes.put("jwt.credential.certificate", "MIIC...");
        attributes.put("jwt.credential.kid", "k");
        attributes.put("client.secret.rotated", "old");
        attributes.put("client.secret.rotated.expiration.time", "1");
        attributes.put("client.secret.creation.time", "1");

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        assertThat(attributes.keySet(), not(hasItems("jwt.credential.certificate", "jwt.credential.kid",
                "client.secret.rotated", "client.secret.rotated.expiration.time")));
        // Unrelated attributes are left alone.
        assertThat(attributes, hasKey("client.secret.creation.time"));
        verify(client).updateClient();
    }

    @Test
    void serviceAccountLeavesEveryGroup() {
        upToDateClient();
        var group = mock(GroupModel.class);
        when(serviceAccount.getGroupsStream()).thenAnswer(i -> Stream.of(group));

        var outcome = FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        assertThat(outcome, is(UPDATED));
        verify(serviceAccount).leaveGroup(group);
    }

    @Test
    void realmWithNoDefaultRoleStripsEverythingButRaidDumper() {
        upToDateClient();
        when(realm.getDefaultRole()).thenReturn(null);
        when(serviceAccount.getRoleMappingsStream()).thenReturn(List.of(raidDumper, defaultRole).stream());

        FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS);

        verify(serviceAccount).deleteRoleMapping(defaultRole);
        verify(serviceAccount, never()).deleteRoleMapping(raidDumper);
    }

    @Test
    void failsLoudlyWhenNoServiceAccountIsCreated() {
        var client = statefulClient();
        when(clientProvider.addClient(realm, "raid-federation-harvester")).thenReturn(client);

        try (var ignoredRealmManager = mockConstruction(RealmManager.class);
             var ignoredClientManager = mockConstruction(ClientManager.class)) {
            var error = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> FederationHarvestClientProvisioner.provision(session, realm, URL_KEYS));
            assertThat(error.getMessage(), containsString("raid-federation-harvester"));
        }
    }
}
