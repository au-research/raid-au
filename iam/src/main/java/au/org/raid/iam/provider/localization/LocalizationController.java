// LocalizationController.java
package au.org.raid.iam.provider.localization;

import au.org.raid.iam.provider.cors.Cors;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.HashMap;

@Slf4j
@Provider
public class LocalizationController {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KeycloakSession session;
    private final Cors cors;

    public LocalizationController(final KeycloakSession session) {
        this.session = session;
        this.cors = new Cors(session, objectMapper);
    }

    @OPTIONS
    @Path("")
    public Response localizationPreflight() {
        return cors.buildOptionsResponse("GET", "OPTIONS");
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response getLocalizationMessage(
            @QueryParam("key") String key,
            @QueryParam("locale") @DefaultValue("en") String locale) {
        log.debug("Fetching localization for key: {}, locale: {}", key, locale);

        // No auth check — this serves public UI text (see comment 3)
        // Any authenticated or unauthenticated user can access localization strings

        if (key == null || key.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"key parameter is required\"}")
                    .build();
        }

        final var realm = session.getContext().getRealm();
        var localizationTexts = realm.getRealmLocalizationTextsByLocale(locale);
        var value = localizationTexts.get(key);

        if (value == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Localization key not found\"}")
                    .build();
        }

        var responseBody = new HashMap<String, String>();
        responseBody.put("key", key);
        responseBody.put("value", value);
        responseBody.put("locale", locale);

        return cors.buildCorsResponse("GET",
                Response.ok().entity(objectMapper.writeValueAsString(responseBody)));
    }
}
