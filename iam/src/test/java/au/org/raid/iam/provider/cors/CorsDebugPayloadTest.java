package au.org.raid.iam.provider.cors;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

/**
 * RAID-847 regression guard.
 *
 * <p>{@code Cors} used to log {@code objectMapper.writeValueAsString(response)} at debug level.
 * Jackson serialises {@code Response.getEntity()}, so that wrote every response body from every SPI
 * controller into the application log, including the client secrets returned by the credential
 * endpoints. The debug payload must never include the entity.
 */
class CorsDebugPayloadTest {

    private final Cors cors = new Cors(mock(KeycloakSession.class), new ObjectMapper());

    private Response responseCarryingASecret() {
        return Response.ok()
                .entity("{\"clientId\":\"raid-cred-a1\",\"secret\":\"SUPER_SECRET\"}")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @Test
    void neverIncludesTheResponseEntity() {
        final var payload = cors.debugPayload(responseCarryingASecret());

        assertThat(payload, not(containsString("SUPER_SECRET")));
        assertThat(payload, not(containsString("clientId")));
        assertThat(payload, not(containsStringIgnoringCase("entity")));
    }

    @Test
    void stillReportsStatusAndHeadersSoItRemainsUsefulForDebugging() {
        final var payload = cors.debugPayload(responseCarryingASecret());

        assertThat(payload, containsString("200"));
        assertThat(payload, containsString("Cache-Control"));
    }

    @Test
    void handlesAResponseWithNoEntity() {
        final var payload = cors.debugPayload(Response.noContent().build());

        assertThat(payload, containsString("204"));
    }
}
