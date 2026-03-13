package au.org.raid.iam.provider.cors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class Cors {
    private final KeycloakSession session;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public Response buildCorsResponse(String method, Response.ResponseBuilder responseBuilder) {
        return addHeaders(responseBuilder, method);
    }

    @SneakyThrows
    public Response buildOptionsResponse(String... methods) {
        Response.ResponseBuilder builder = Response.ok();

        return addHeaders(builder, methods);
    }

    @SneakyThrows
    private Response addHeaders(final Response.ResponseBuilder responseBuilder, String... methods) {
        String origin = session.getContext().getRequestHeaders().getHeaderString("Origin");

        // Only set the Origin header if it's in our allowed list
        if (origin != null && getAllowedOrigins().contains(origin)) {
            responseBuilder.header("Access-Control-Allow-Origin", origin);
            responseBuilder.header("Access-Control-Allow-Credentials", "true");
        }

        responseBuilder.header("Access-Control-Allow-Methods", String.join(", ", methods));
        responseBuilder.header("Access-Control-Allow-Headers", "Authorization,Content-Type");
        responseBuilder.header("Access-Control-Max-Age", "3600");

        final var response = responseBuilder.build();
        log.debug("Returning response {}", objectMapper.writeValueAsString(response));
        return response;

    }

    private List<String> getAllowedOrigins() {
        try {
            final var realm = session.getContext().getRealm();
            final var allowedOrigins =  session.clients().getClientsStream(realm)
                    .filter(client -> client.getWebOrigins() != null)
                    .flatMap(client -> client.getWebOrigins().stream())
                    .filter(origin -> origin != null && !origin.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            log.debug("found allowed origins: {}", String.join(", ", allowedOrigins));

            return allowedOrigins;
        } catch (Exception e) {
            log.error("Error getting origins", e);
            return Collections.emptyList();
        }
    }
}
