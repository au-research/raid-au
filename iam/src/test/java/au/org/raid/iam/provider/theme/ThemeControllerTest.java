package au.org.raid.iam.provider.theme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// The shared setUp stubs more than each test reads (Cors reads the request origin, only some tests
// look up a client), so unused stubs are expected here.
@MockitoSettings(strictness = Strictness.LENIENT)
class ThemeControllerTest {
    private static final String WEB_APP_ORIGIN = "http://localhost:7080";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private HttpHeaders headers;
    @Mock private ClientProvider clientProvider;

    private ThemeController controller;

    @BeforeEach
    void setUp() {
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(session.clients()).thenReturn(clientProvider);

        controller = new ThemeController(session);
    }

    private JsonNode body(final Response response) throws Exception {
        return objectMapper.readTree((String) response.getEntity());
    }

    private ClientModel clientWithTheme(final String clientId, final String loginTheme) {
        final var client = mock(ClientModel.class);
        when(client.getAttribute("login_theme")).thenReturn(loginTheme);
        when(clientProvider.getClientByClientId(realm, clientId)).thenReturn(client);
        return client;
    }

    @Test
    void returnsTheRealmLoginThemeWhenNoClientIsGiven() throws Exception {
        when(realm.getLoginTheme()).thenReturn("ardc-branding");

        final var response = controller.getLoginTheme(null);

        assertThat(response.getStatus(), is(200));
        assertThat(body(response).get("loginTheme").asText(), is("ardc-branding"));
        verifyNoInteractions(clientProvider);
    }

    @Test
    void returnsNullWhenTheRealmHasNoLoginTheme() throws Exception {
        when(realm.getLoginTheme()).thenReturn(null);

        final var response = controller.getLoginTheme(null);

        assertThat(response.getStatus(), is(200));
        assertThat(body(response).get("loginTheme").isNull(), is(true));
    }

    @Test
    void returnsNullWhenTheRealmLoginThemeIsEmpty() throws Exception {
        when(realm.getLoginTheme()).thenReturn("");

        final var response = controller.getLoginTheme(null);

        assertThat(body(response).get("loginTheme").isNull(), is(true));
    }

    @Test
    void clientLoginThemeWinsOverTheRealmTheme() throws Exception {
        when(realm.getLoginTheme()).thenReturn("raid-custom");
        clientWithTheme("raid-api", "ardc-branding");

        final var response = controller.getLoginTheme("raid-api");

        assertThat(body(response).get("loginTheme").asText(), is("ardc-branding"));
    }

    @Test
    void clientWithoutAThemeFallsBackToTheRealmTheme() throws Exception {
        when(realm.getLoginTheme()).thenReturn("raid-custom");
        clientWithTheme("raid-api", null);

        final var response = controller.getLoginTheme("raid-api");

        assertThat(body(response).get("loginTheme").asText(), is("raid-custom"));
    }

    @Test
    void clientWithAnEmptyThemeFallsBackToTheRealmTheme() throws Exception {
        when(realm.getLoginTheme()).thenReturn("raid-custom");
        clientWithTheme("raid-api", "");

        final var response = controller.getLoginTheme("raid-api");

        assertThat(body(response).get("loginTheme").asText(), is("raid-custom"));
    }

    @Test
    void unknownClientFallsBackToTheRealmTheme() throws Exception {
        when(realm.getLoginTheme()).thenReturn("raid-custom");
        when(clientProvider.getClientByClientId(realm, "no-such-client")).thenReturn(null);

        final var response = controller.getLoginTheme("no-such-client");

        assertThat(response.getStatus(), is(200));
        assertThat(body(response).get("loginTheme").asText(), is("raid-custom"));
    }

    @Test
    void blankClientIdIsIgnored() throws Exception {
        when(realm.getLoginTheme()).thenReturn("raid-custom");

        final var response = controller.getLoginTheme("   ");

        assertThat(body(response).get("loginTheme").asText(), is("raid-custom"));
        verify(clientProvider, never()).getClientByClientId(any(), any());
    }

    @Test
    void onlyTheThemeNameIsExposed() throws Exception {
        when(realm.getLoginTheme()).thenReturn("ardc-branding");
        when(realm.getName()).thenReturn("raid");
        when(realm.getAccountTheme()).thenReturn("account-theme");
        when(realm.getEmailTheme()).thenReturn("email-theme");

        final var response = controller.getLoginTheme(null);

        final var names = new ArrayList<String>();
        body(response).fieldNames().forEachRemaining(names::add);
        assertThat(names, is(List.of("loginTheme")));
    }

    @Test
    void preflightAllowsGet() {
        when(headers.getHeaderString("Origin")).thenReturn(WEB_APP_ORIGIN);
        allowOrigin(WEB_APP_ORIGIN);

        final var response = controller.themePreflight();

        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaderString("Access-Control-Allow-Methods"), is("GET, OPTIONS"));
        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is(WEB_APP_ORIGIN));
    }

    @Test
    void getResponseCarriesCorsHeadersForAnAllowedOrigin() {
        when(realm.getLoginTheme()).thenReturn("ardc-branding");
        when(headers.getHeaderString("Origin")).thenReturn(WEB_APP_ORIGIN);
        allowOrigin(WEB_APP_ORIGIN);

        final var response = controller.getLoginTheme(null);

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is(WEB_APP_ORIGIN));
        assertThat(response.getHeaderString("Access-Control-Allow-Methods"), is("GET"));
    }

    @Test
    void getResponseOmitsCorsOriginForAnOriginThatIsNotAllowed() {
        when(realm.getLoginTheme()).thenReturn("ardc-branding");
        when(headers.getHeaderString("Origin")).thenReturn("http://evil.example");
        allowOrigin(WEB_APP_ORIGIN);

        final var response = controller.getLoginTheme(null);

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is(nullValue()));
    }

    private void allowOrigin(final String origin) {
        final var client = mock(ClientModel.class);
        when(client.getWebOrigins()).thenReturn(Set.of(origin));
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.of(client));
    }
}
