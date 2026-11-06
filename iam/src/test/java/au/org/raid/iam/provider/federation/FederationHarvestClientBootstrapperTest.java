package au.org.raid.iam.provider.federation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static au.org.raid.iam.provider.federation.FederationHarvestClientProvisioner.Outcome.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** RAID-982: the harvest client is provisioned unattended at boot, in every RAiD realm. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FederationHarvestClientBootstrapperTest {

    @Mock private KeycloakSessionFactory sessionFactory;
    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmProvider realmProvider;

    private final List<String> reachabilityChecks = new ArrayList<>();
    private final List<Boolean> embeddedKeysAvailable = new ArrayList<>();
    private final FederationHarvestClientBootstrapper.ReachabilityCheck reachabilityCheck = (url, embedded) -> {
        reachabilityChecks.add(url);
        embeddedKeysAvailable.add(embedded);
    };

    private ProviderEventListener register(final Map<String, String> environment) {
        FederationHarvestClientBootstrapper.register(sessionFactory, environment::get, reachabilityCheck);
        var captor = ArgumentCaptor.forClass(ProviderEventListener.class);
        verify(sessionFactory).register(captor.capture());
        return captor.getValue();
    }

    private static void runTasksInline(final MockedStatic<KeycloakModelUtils> mocked, final KeycloakSession session) {
        mocked.when(() -> KeycloakModelUtils.runJobInTransaction(any(), any()))
                .thenAnswer(invocation -> {
                    KeycloakSessionTask task = invocation.getArgument(1);
                    task.run(session);
                    return null;
                });
    }

    private RealmModel realm(final String id, final String name) {
        var realm = mock(RealmModel.class);
        when(realm.getId()).thenReturn(id);
        when(realm.getName()).thenReturn(name);
        when(realmProvider.getRealm(id)).thenReturn(realm);
        return realm;
    }

    private void realmsAre(final RealmModel... realms) {
        when(session.realms()).thenReturn(realmProvider);
        when(session.getContext()).thenReturn(context);
        when(realmProvider.getRealmsStream()).thenAnswer(i -> Stream.of(realms));
    }

    @Test
    void provisionsEveryRealmWithTheDefaultUrlAndChecksReachabilityOnce() {
        var master = realm("m", "master");
        var raid = realm("r", "raid");
        realmsAre(master, raid);
        var listener = register(Map.of());

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), eq(master), any()))
                    .thenReturn(NOT_A_RAID_REALM);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), eq(raid), any()))
                    .thenReturn(CREATED);

            listener.onEvent(new PostMigrationEvent(sessionFactory));

            var expected = FederationHarvestKeySource.url("https://keys.raid.org/federation/jwks.json");
            provisioner.verify(() -> FederationHarvestClientProvisioner.provision(session, master, expected));
            provisioner.verify(() -> FederationHarvestClientProvisioner.provision(session, raid, expected));
        }
        verify(context).setRealm(raid);
        assertThat(reachabilityChecks, org.hamcrest.Matchers.contains("https://keys.raid.org/federation/jwks.json"));
        // The bundled key set ships empty, so the warning must not suggest switching to it.
        assertThat(embeddedKeysAvailable, org.hamcrest.Matchers.contains(false));
    }

    @Test
    void overrideUrlIsUsedAndChecked() {
        var raid = realm("r", "raid");
        realmsAre(raid);
        var listener = register(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "https://example.test/jwks.json"));

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(UNCHANGED);

            listener.onEvent(new PostMigrationEvent(sessionFactory));

            provisioner.verify(() -> FederationHarvestClientProvisioner.provision(
                    session, raid, FederationHarvestKeySource.url("https://example.test/jwks.json")));
        }
        assertThat(reachabilityChecks, org.hamcrest.Matchers.contains("https://example.test/jwks.json"));
    }

    @Test
    void noReachabilityCheckWhenNoRealmIsARaidRealm() {
        realmsAre(realm("m", "master"));
        var listener = register(Map.of());

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(NOT_A_RAID_REALM);

            listener.onEvent(new PostMigrationEvent(sessionFactory));
        }
        assertThat(reachabilityChecks, is(empty()));
    }

    @Test
    void embeddedModeProvisionsWithoutAReachabilityCheck() {
        var raid = realm("r", "raid");
        realmsAre(raid);
        var listener = register(Map.of("RAID_FEDERATION_HARVEST_USE_JWKS_URL", "false"));

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(CREATED);

            listener.onEvent(new PostMigrationEvent(sessionFactory));

            provisioner.verify(() -> FederationHarvestClientProvisioner.provision(eq(session), eq(raid),
                    argThat(keys -> !keys.usesUrl())));
        }
        assertThat(reachabilityChecks, is(empty()));
    }

    @Test
    void plainHttpOverrideWithoutOptInProvisionsNothing() {
        var listener = register(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "http://example.test/jwks.json"));

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            listener.onEvent(new PostMigrationEvent(sessionFactory));

            provisioner.verifyNoInteractions();
        }
    }

    @Test
    void plainHttpOverrideWithOptInIsUsed() {
        var raid = realm("r", "raid");
        realmsAre(raid);
        var listener = register(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "http://jwks:8080/jwks.json",
                "RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL", "true"));

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(CREATED);

            listener.onEvent(new PostMigrationEvent(sessionFactory));

            provisioner.verify(() -> FederationHarvestClientProvisioner.provision(
                    session, raid, FederationHarvestKeySource.url("http://jwks:8080/jwks.json")));
        }
    }

    @Test
    void invalidOverrideProvisionsNothing() {
        var listener = register(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "not a url"));

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));

            utils.verifyNoInteractions();
            provisioner.verifyNoInteractions();
        }
        assertThat(reachabilityChecks, is(empty()));
    }

    @Test
    void oneFailingRealmDoesNotStopTheOthersOrBoot() {
        var broken = realm("b", "broken");
        var raid = realm("r", "raid");
        realmsAre(broken, raid);
        var listener = register(Map.of());

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), eq(broken), any()))
                    .thenThrow(new RuntimeException("boom"));
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), eq(raid), any()))
                    .thenReturn(CREATED);

            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));

            provisioner.verify(() -> FederationHarvestClientProvisioner.provision(eq(session), eq(raid), any()));
        }
        assertThat(reachabilityChecks, hasSize(1));
    }

    @Test
    void failureListingRealmsDoesNotStopBoot() {
        var listener = register(Map.of());

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            utils.when(() -> KeycloakModelUtils.runJobInTransaction(any(), any()))
                    .thenThrow(new RuntimeException("db down"));

            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));
            provisioner.verifyNoInteractions();
        }
    }

    @Test
    void reachabilityCheckThatThrowsDoesNotStopBoot() {
        realmsAre(realm("r", "raid"));
        FederationHarvestClientBootstrapper.register(sessionFactory, Map.<String, String>of()::get, (url, embedded) -> {
            throw new IllegalStateException("no threads");
        });
        var captor = ArgumentCaptor.forClass(ProviderEventListener.class);
        verify(sessionFactory).register(captor.capture());

        try (var utils = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(FederationHarvestClientProvisioner.class)) {
            runTasksInline(utils, session);
            provisioner.when(() -> FederationHarvestClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(CREATED);

            assertDoesNotThrow(() -> captor.getValue().onEvent(new PostMigrationEvent(sessionFactory)));
        }
    }

    @Test
    void otherEventsAreIgnored() {
        var listener = register(Map.of());

        try (var utils = mockStatic(KeycloakModelUtils.class)) {
            listener.onEvent(mock(ProviderEvent.class));
            utils.verifyNoInteractions();
        }
    }
}
