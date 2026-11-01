package au.org.raid.iam.provider.orcidupdater;

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

import java.util.function.Supplier;
import java.util.stream.Stream;

import static au.org.raid.iam.provider.orcidupdater.OrcidUpdaterClientProvisioner.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RAID-1011: the ORCID updater's client is created, or put right, in every RAiD realm at boot.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrcidUpdaterClientProvisionerTest {

    private static final String JWKS_URL = "https://orcid-updater.example.org/.well-known/jwks.json";

    @Mock private KeycloakSession session;
    @Mock private RealmModel realm;
    @Mock private ClientProvider clientProvider;
    @Mock private UserProvider userProvider;
    @Mock private RoleModel servicePointUser;
    @Mock private RoleModel contributorWriter;
    @Mock private ClientModel client;
    @Mock private UserModel serviceAccount;

    @BeforeEach
    void raidRealm() {
        when(session.clients()).thenReturn(clientProvider);
        when(session.users()).thenReturn(userProvider);
        when(realm.getRole(SERVICE_POINT_USER_ROLE)).thenReturn(servicePointUser);
        when(realm.getRole(CONTRIBUTOR_WRITER_ROLE)).thenReturn(contributorWriter);
        when(realm.addRole(CONTRIBUTOR_WRITER_ROLE)).thenReturn(contributorWriter);
        when(clientProvider.addClient(realm, CLIENT_ID)).thenReturn(client);
    }

    /** A client as addClient() leaves it: public, standard flow, no full scope, secret auth. */
    private void newClient() {
        when(realm.getClientByClientId(CLIENT_ID)).thenReturn(null);
        when(client.isPublicClient()).thenReturn(true);
        when(client.isStandardFlowEnabled()).thenReturn(true);
        when(client.isFullScopeAllowed()).thenReturn(false);
        when(client.getClientAuthenticatorType()).thenReturn("client-secret");
        when(client.isServiceAccountsEnabled()).thenReturn(false);
    }

    /** The client exactly as this provisioner leaves it. */
    private void provisionedClient(final String jwksUrl) {
        when(realm.getClientByClientId(CLIENT_ID)).thenReturn(client);
        when(client.isFullScopeAllowed()).thenReturn(true);
        when(client.getClientAuthenticatorType()).thenReturn(CLIENT_JWT_AUTHENTICATOR);
        when(client.getAttribute(USE_JWKS_URL_ATTRIBUTE)).thenReturn("true");
        when(client.getAttribute(JWKS_URL_ATTRIBUTE)).thenReturn(jwksUrl);
        when(client.getAttribute(SIGNING_ALG_ATTRIBUTE)).thenReturn(SIGNING_ALG);
        when(client.isServiceAccountsEnabled()).thenReturn(true);
        when(userProvider.getServiceAccount(client)).thenReturn(serviceAccount);
        when(serviceAccount.hasDirectRole(contributorWriter)).thenReturn(true);
    }

    private static <T> T withClientManager(final Supplier<T> action) {
        try (MockedConstruction<RealmManager> ignoredRealmManager = mockConstruction(RealmManager.class);
             MockedConstruction<ClientManager> ignoredClientManager = mockConstruction(ClientManager.class)) {
            return action.get();
        }
    }

    @Test
    void createsConfidentialSignedJwtClientWithContributorWriter() {
        newClient();
        var defaultScope = mock(ClientScopeModel.class);
        when(realm.getDefaultClientScopesStream(true)).thenAnswer(inv -> Stream.of(defaultScope));
        when(userProvider.getServiceAccount(client)).thenReturn(serviceAccount);

        try (MockedConstruction<RealmManager> ignored = mockConstruction(RealmManager.class);
             MockedConstruction<ClientManager> clientManagers = mockConstruction(ClientManager.class)) {
            var outcome = provision(session, realm, JWKS_URL);

            assertThat(outcome, is(Outcome.CREATED));
            verify(clientManagers.constructed().get(0)).enableServiceAccount(client);
        }

        verify(client).setPublicClient(false);
        verify(client).setStandardFlowEnabled(false);
        verify(client).setFullScopeAllowed(true);
        verify(client).setClientAuthenticatorType("client-jwt");
        verify(client).setAttribute("use.jwks.url", "true");
        verify(client).setAttribute("jwks.url", JWKS_URL);
        verify(client).setAttribute("token.endpoint.auth.signing.alg", "RS256");
        verify(client).setEnabled(true);
        verify(client).addClientScope(defaultScope, true);
        verify(serviceAccount).grantRole(contributorWriter);
    }

    @Test
    void neverGeneratesOrSetsASecret() {
        newClient();
        when(realm.getDefaultClientScopesStream(true)).thenAnswer(inv -> Stream.empty());
        when(userProvider.getServiceAccount(client)).thenReturn(serviceAccount);

        withClientManager(() -> provision(session, realm, JWKS_URL));

        verify(client, never()).setSecret(any());
    }

    @Test
    void createsContributorWriterRoleWhenTheRealmLacksIt() {
        provisionedClient(JWKS_URL);
        when(realm.getRole(CONTRIBUTOR_WRITER_ROLE)).thenReturn(null);
        when(serviceAccount.hasDirectRole(contributorWriter)).thenReturn(false);

        var outcome = provision(session, realm, JWKS_URL);

        assertThat(outcome, is(Outcome.UPDATED));
        verify(realm).addRole(CONTRIBUTOR_WRITER_ROLE);
        // The two-argument form is (id, name) and would corrupt the role
        verify(realm, never()).addRole(anyString(), anyString());
        verify(contributorWriter).setDescription(anyString());
        verify(serviceAccount).grantRole(contributorWriter);
    }

    @Test
    void leavesARealmWithoutServicePointUserAlone() {
        // e.g. master, or any realm an agency runs alongside RAiD
        when(realm.getRole(SERVICE_POINT_USER_ROLE)).thenReturn(null);

        var outcome = provision(session, realm, JWKS_URL);

        assertThat(outcome, is(Outcome.NOT_A_RAID_REALM));
        verify(realm, never()).addRole(anyString());
        verify(realm, never()).getClientByClientId(anyString());
        verifyNoInteractions(clientProvider, userProvider);
    }

    @Test
    void changesNothingOnARestart() {
        provisionedClient(JWKS_URL);

        var outcome = provision(session, realm, JWKS_URL);

        assertThat(outcome, is(Outcome.UNCHANGED));
        verify(clientProvider, never()).addClient(any(), anyString());
        verify(client, never()).setAttribute(anyString(), anyString());
        verify(client, never()).addClientScope(any(), anyBoolean());
        verify(serviceAccount, never()).grantRole(any());
    }

    @Test
    void followsAChangedJwksUrl() {
        provisionedClient("https://old.example.org/.well-known/jwks.json");

        var outcome = provision(session, realm, JWKS_URL);

        assertThat(outcome, is(Outcome.UPDATED));
        verify(client).setAttribute("jwks.url", JWKS_URL);
    }

    @Test
    void putsBackADriftedClientButKeepsItsEnabledFlag() {
        // e.g. someone created it by hand with a secret and a login flow, or switched it off
        provisionedClient(JWKS_URL);
        when(client.getClientAuthenticatorType()).thenReturn("client-secret");
        when(client.isDirectAccessGrantsEnabled()).thenReturn(true);
        when(client.isFullScopeAllowed()).thenReturn(false);
        when(client.isEnabled()).thenReturn(false);

        var outcome = provision(session, realm, JWKS_URL);

        assertThat(outcome, is(Outcome.UPDATED));
        verify(client).setClientAuthenticatorType("client-jwt");
        verify(client).setDirectAccessGrantsEnabled(false);
        verify(client).setFullScopeAllowed(true);
        verify(client, never()).setEnabled(anyBoolean());
    }

    @Test
    void enablesAMissingServiceAccount() {
        provisionedClient(JWKS_URL);
        when(client.isServiceAccountsEnabled()).thenReturn(false);
        when(serviceAccount.hasDirectRole(contributorWriter)).thenReturn(false);

        try (MockedConstruction<RealmManager> ignored = mockConstruction(RealmManager.class);
             MockedConstruction<ClientManager> clientManagers = mockConstruction(ClientManager.class)) {
            var outcome = provision(session, realm, JWKS_URL);

            assertThat(outcome, is(Outcome.UPDATED));
            verify(clientManagers.constructed().get(0)).enableServiceAccount(client);
        }
        verify(serviceAccount).grantRole(contributorWriter);
    }

    @Test
    void failsWhenKeycloakCreatesNoServiceAccount() {
        newClient();
        when(realm.getDefaultClientScopesStream(true)).thenAnswer(inv -> Stream.empty());
        when(userProvider.getServiceAccount(client)).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> withClientManager(() -> provision(session, realm, JWKS_URL)));
    }

    @Test
    void acceptsOnlyAbsoluteHttpUrls() {
        assertThat(invalidJwksUrlReason(JWKS_URL), is(nullValue()));
        assertThat(invalidJwksUrlReason("http://orcid-updater:8090/.well-known/jwks.json"), is(nullValue()));
        assertThat(invalidJwksUrlReason("ftp://example.org/jwks.json"), is(notNullValue()));
        assertThat(invalidJwksUrlReason("/.well-known/jwks.json"), is(notNullValue()));
        assertThat(invalidJwksUrlReason("https:///jwks.json"), is(notNullValue()));
        assertThat(invalidJwksUrlReason("not a url"), is(notNullValue()));
    }
}
