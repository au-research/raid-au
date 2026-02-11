package au.org.raid.iam.provider.raid;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;

import java.util.Map;

@RequiredArgsConstructor
public class RaidClient {
    private static final String URL_FORMAT = "http://%s/raid/%s/permissions";
    private final ObjectMapper objectMapper;
    private final KeycloakSession session;
    private static final Map<String, String> HOSTNAME_MAP = Map.of(
            "localhost", "host.docker.internal:8080",
            "iam.test.raid.org.au", "api.test.raid.org.au",
            "iam.demo.raid.org.au", "api.demo.raid.org.au",
            "iam.stage.raid.org.au", "api.stage.raid.org.au",
            "iam.prod.raid.org.au", "api.prod.raid.org.au"
    );

    @SneakyThrows
    public RaidPermissionsDto getPermissions(final String handle, final AccessToken token, final KeyWrapper key) {
        var hostname = System.getenv("KC_HOSTNAME");
        var url = URL_FORMAT.formatted(HOSTNAME_MAP.get(hostname), handle);

        final var http = SimpleHttp.create(session).doGet(url);

        String jwt = new JWSBuilder()
                .type("JWT")
                .jsonContent(token)
                .sign(new AsymmetricSignatureSignerContext(key));

        http.auth(jwt);
        try (final var response = http.asResponse()) {
            return objectMapper.readValue(response.asString(), RaidPermissionsDto.class);
        }
    }
}
