package au.org.raid.iam.provider.orcidupdater;

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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RAID-1011: the ORCID updater's client is provisioned unattended at boot, only when the RA has
 * set the updater's JWKS URL, and never stops Keycloak starting.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrcidUpdaterClientBootstrapperTest {

    private static final String JWKS_URL = "https://orcid-updater.example.org/.well-known/jwks.json";

    @Mock private KeycloakSessionFactory sessionFactory;
    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmProvider realmProvider;

    private ProviderEventListener registerAndCaptureListener(final String jwksUrl) {
        final var environment = new java.util.HashMap<String, String>();
        environment.put(OrcidUpdaterClientBootstrapper.JWKS_URL_ENV_VAR, jwksUrl);
        OrcidUpdaterClientBootstrapper.register(sessionFactory, environment::get);
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

    private void realms(final RealmModel... realms) {
        when(session.getContext()).thenReturn(context);
        when(session.realms()).thenReturn(realmProvider);
        when(realmProvider.getRealmsStream()).thenAnswer(inv -> Stream.of(realms));
    }

    @Test
    void ignoresEventsOtherThanPostMigration() {
        var listener = registerAndCaptureListener(JWKS_URL);

        try (var mocked = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(OrcidUpdaterClientProvisioner.class)) {
            listener.onEvent(mock(ProviderEvent.class));

            mocked.verifyNoInteractions();
            provisioner.verifyNoInteractions();
        }
    }

    @Test
    void provisionsEveryRealmWithTheConfiguredJwksUrl() {
        var master = realm("realm-master", "master");
        var raid = realm("realm-raid", "raid");
        realms(master, raid);
        var listener = registerAndCaptureListener(JWKS_URL);

        try (var mocked = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(OrcidUpdaterClientProvisioner.class)) {
            runTasksInline(mocked, session);
            provisioner.when(() -> OrcidUpdaterClientProvisioner.invalidJwksUrlReason(JWKS_URL)).thenReturn(null);
            provisioner.when(() -> OrcidUpdaterClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(OrcidUpdaterClientProvisioner.Outcome.CREATED);

            listener.onEvent(new PostMigrationEvent(sessionFactory));

            provisioner.verify(() -> OrcidUpdaterClientProvisioner.provision(session, master, JWKS_URL));
            provisioner.verify(() -> OrcidUpdaterClientProvisioner.provision(session, raid, JWKS_URL));
        }
        verify(context).setRealm(raid);
    }

    @Test
    void trimsTheConfiguredUrl() {
        var raid = realm("realm-raid", "raid");
        realms(raid);
        var listener = registerAndCaptureListener("  " + JWKS_URL + "\n");

        try (var mocked = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(OrcidUpdaterClientProvisioner.class)) {
            runTasksInline(mocked, session);
            provisioner.when(() -> OrcidUpdaterClientProvisioner.invalidJwksUrlReason(JWKS_URL)).thenReturn(null);
            provisioner.when(() -> OrcidUpdaterClientProvisioner.provision(any(), any(), any()))
                    .thenReturn(OrcidUpdaterClientProvisioner.Outcome.UNCHANGED);

            listener.onEvent(new PostMigrationEvent(sessionFactory));

            provisioner.verify(() -> OrcidUpdaterClientProvisioner.provision(session, raid, JWKS_URL));
        }
    }

    /** The ORCID updater is optional for an RA: unset means do nothing at all. */
    @Test
    void doesNothingWhenTheJwksUrlIsUnset() {
        for (final var unset : new String[]{null, "", "   "}) {
            reset(sessionFactory);
            var listener = registerAndCaptureListener(unset);

            try (var mocked = mockStatic(KeycloakModelUtils.class);
                 var provisioner = mockStatic(OrcidUpdaterClientProvisioner.class)) {
                listener.onEvent(new PostMigrationEvent(sessionFactory));

                mocked.verifyNoInteractions();
                provisioner.verify(() -> OrcidUpdaterClientProvisioner.provision(any(), any(), any()), never());
            }
        }
    }

    @Test
    void doesNothingWhenTheJwksUrlIsInvalid() {
        var listener = registerAndCaptureListener("not a url");

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));

            mocked.verifyNoInteractions();
        }
    }

    /** One failing realm must not stop the others, nor Keycloak starting. */
    @Test
    void oneFailingRealmDoesNotStopTheOthersOrPropagate() {
        var bad = realm("realm-bad", "bad");
        var raid = realm("realm-raid", "raid");
        realms(bad, raid);
        var listener = registerAndCaptureListener(JWKS_URL);

        try (var mocked = mockStatic(KeycloakModelUtils.class);
             var provisioner = mockStatic(OrcidUpdaterClientProvisioner.class)) {
            runTasksInline(mocked, session);
            provisioner.when(() -> OrcidUpdaterClientProvisioner.invalidJwksUrlReason(JWKS_URL)).thenReturn(null);
            provisioner.when(() -> OrcidUpdaterClientProvisioner.provision(any(), eq(bad), any()))
                    .thenThrow(new IllegalStateException("boom"));
            provisioner.when(() -> OrcidUpdaterClientProvisioner.provision(any(), eq(raid), any()))
                    .thenReturn(OrcidUpdaterClientProvisioner.Outcome.CREATED);

            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));

            provisioner.verify(() -> OrcidUpdaterClientProvisioner.provision(session, raid, JWKS_URL));
        }
    }

    @Test
    void failingToListRealmsDoesNotPropagate() {
        var listener = registerAndCaptureListener(JWKS_URL);

        try (var mocked = mockStatic(KeycloakModelUtils.class)) {
            mocked.when(() -> KeycloakModelUtils.runJobInTransaction(any(), any()))
                    .thenThrow(new RuntimeException("database down"));

            assertDoesNotThrow(() -> listener.onEvent(new PostMigrationEvent(sessionFactory)));
        }
    }
}
