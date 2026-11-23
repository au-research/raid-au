// ThemeController.java
package au.org.raid.iam.provider.theme;

import au.org.raid.iam.provider.cors.Cors;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Collections;

/**
 * Tells the RAiD web app and landing pages which login theme the realm uses, so they can choose
 * matching branding (RAID-1073).
 *
 * <p>Mirrors how Keycloak itself picks the login theme: the {@code login_theme} attribute of the
 * client wins when it is set, otherwise the realm's login theme applies. When neither is set the
 * theme is {@code null}, which callers treat as neutral branding.
 */
@Slf4j
@Provider
public class ThemeController {
    /** The client attribute Keycloak reads to override the realm's login theme. */
    static final String LOGIN_THEME_CLIENT_ATTRIBUTE = "login_theme";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeycloakSession session;
    private final Cors cors;

    public ThemeController(final KeycloakSession session) {
        this.session = session;
        this.cors = new Cors(session, objectMapper);
    }

    @OPTIONS
    @Path("")
    public Response themePreflight() {
        return cors.buildOptionsResponse("GET", "OPTIONS");
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response getLoginTheme(@QueryParam("client_id") String clientId) {
        // No auth check: the theme name is not sensitive. It already appears in the resource URLs
        // of the login page, and the web app needs it before the user has signed in.
        final var realm = session.getContext().getRealm();
        final var loginTheme = resolveLoginTheme(realm, clientId);
        log.debug("Resolved login theme for client {}: {}", clientId, loginTheme);

        // Only the theme name is returned, never other realm settings.
        final var responseBody = Collections.singletonMap("loginTheme", loginTheme);

        return cors.buildCorsResponse("GET",
                Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
    }

    private String resolveLoginTheme(final RealmModel realm, final String clientId) {
        if (clientId != null && !clientId.isBlank()) {
            final var client = session.clients().getClientByClientId(realm, clientId);

            if (client != null) {
                final var clientTheme = client.getAttribute(LOGIN_THEME_CLIENT_ATTRIBUTE);

                if (clientTheme != null && !clientTheme.isEmpty()) {
                    return clientTheme;
                }
            }
        }

        final var realmTheme = realm.getLoginTheme();
        return realmTheme == null || realmTheme.isEmpty() ? null : realmTheme;
    }
}
