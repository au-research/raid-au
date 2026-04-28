package au.org.raid.iam.provider.localization;

import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalizationControllerTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private ClientProvider clientProvider;
    @Mock private HttpHeaders headers;

    @BeforeEach
    void setupSessionContext() {
        when(session.getContext()).thenReturn(context);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(context.getRealm()).thenReturn(realm);
        when(session.clients()).thenReturn(clientProvider);

        var client = mock(ClientModel.class);
        when(client.getWebOrigins()).thenReturn(Set.of("http://localhost:7080"));
        when(clientProvider.getClientsStream(realm)).thenAnswer(inv -> Stream.of(client));

        when(headers.getHeaderString("Origin")).thenReturn("http://localhost:7080");
    }

    private LocalizationController createController() {
        return new LocalizationController(session);
    }

    // --- getLocalizationMessage tests ---

    @Test
    void getLocalizationMessage_returnsBadRequestWhenKeyIsNull() {
        var controller = createController();
        var response = controller.getLocalizationMessage(null, "en");
        assertThat(response.getStatus(), is(400));
    }

    @Test
    void getLocalizationMessage_returnsBadRequestWhenKeyIsEmpty() {
        var controller = createController();
        var response = controller.getLocalizationMessage("", "en");
        assertThat(response.getStatus(), is(400));
    }

    @Test
    void getLocalizationMessage_returnsBadRequestWhenKeyIsBlank() {
        var controller = createController();
        var response = controller.getLocalizationMessage("   ", "en");
        assertThat(response.getStatus(), is(400));
    }

    @Test
    void getLocalizationMessage_returnsNotFoundWhenKeyDoesNotExist() {
        var controller = createController();
        when(realm.getRealmLocalizationTextsByLocale("en")).thenReturn(Map.of());

        var response = controller.getLocalizationMessage("missingKey", "en");
        assertThat(response.getStatus(), is(404));
    }

    @Test
    void getLocalizationMessage_returnsValueWhenKeyExists() {
        var controller = createController();
        when(realm.getRealmLocalizationTextsByLocale("en"))
                .thenReturn(Map.of("welcomeMessage", "Welcome to RAiD"));

        var response = controller.getLocalizationMessage("welcomeMessage", "en");
        assertThat(response.getStatus(), is(200));
    }

    @Test
    void getLocalizationMessage_usesDefaultLocaleWhenNotProvided() {
        var controller = createController();
        when(realm.getRealmLocalizationTextsByLocale("en"))
                .thenReturn(Map.of("welcomeMessage", "Welcome to RAiD"));

        // Simulates @DefaultValue("en") behavior - JAX-RS would pass "en" when locale param is absent
        var response = controller.getLocalizationMessage("welcomeMessage", "en");
        assertThat(response.getStatus(), is(200));
    }

    @Test
    void getLocalizationMessage_supportsNonEnglishLocales() {
        var controller = createController();
        when(realm.getRealmLocalizationTextsByLocale("fr"))
                .thenReturn(Map.of("welcomeMessage", "Bienvenue à RAiD"));

        var response = controller.getLocalizationMessage("welcomeMessage", "fr");
        assertThat(response.getStatus(), is(200));
    }

    // --- preflight tests ---

    @Test
    void localizationPreflight_returns200() {
        var controller = createController();
        var response = controller.localizationPreflight();
        assertThat(response.getStatus(), is(200));
    }
}
