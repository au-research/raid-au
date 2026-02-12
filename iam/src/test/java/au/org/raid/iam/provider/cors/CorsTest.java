package au.org.raid.iam.provider.cors;

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

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorsTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private ClientProvider clientProvider;
    @Mock private HttpHeaders httpHeaders;
    @Mock private ObjectMapper objectMapper;

    private Cors cors;

    @BeforeEach
    void setUp() throws Exception {
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(context.getRequestHeaders()).thenReturn(httpHeaders);
        when(session.clients()).thenReturn(clientProvider);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        cors = new Cors(session, objectMapper);
    }

    @Test
    void buildOptionsResponse_returnsCorrectMethodHeaders() {
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.empty());

        Response response = cors.buildOptionsResponse("GET", "PUT");

        assertEquals(200, response.getStatus());
        assertEquals("GET, PUT", response.getHeaderString("Access-Control-Allow-Methods"));
        assertEquals("Authorization,Content-Type", response.getHeaderString("Access-Control-Allow-Headers"));
        assertEquals("3600", response.getHeaderString("Access-Control-Max-Age"));
    }

    @Test
    void buildCorsResponse_addsOriginHeader_whenOriginIsAllowed() {
        String origin = "http://localhost:7080";
        when(httpHeaders.getHeaderString("Origin")).thenReturn(origin);

        ClientModel client = mock(ClientModel.class);
        when(client.getWebOrigins()).thenReturn(Set.of(origin));
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.of(client));

        Response response = cors.buildCorsResponse("GET", Response.ok());

        assertEquals(origin, response.getHeaderString("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeaderString("Access-Control-Allow-Credentials"));
    }

    @Test
    void buildCorsResponse_doesNotAddOriginHeader_whenOriginNotAllowed() {
        when(httpHeaders.getHeaderString("Origin")).thenReturn("http://evil.com");
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.empty());

        Response response = cors.buildCorsResponse("GET", Response.ok());

        assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void buildCorsResponse_doesNotAddOriginHeader_whenNoOriginInRequest() {
        when(httpHeaders.getHeaderString("Origin")).thenReturn(null);
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.empty());

        Response response = cors.buildCorsResponse("GET", Response.ok());

        assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void buildCorsResponse_filtersClientsWithNullWebOrigins() {
        String origin = "http://localhost:7080";
        when(httpHeaders.getHeaderString("Origin")).thenReturn(origin);

        ClientModel clientWithOrigins = mock(ClientModel.class);
        when(clientWithOrigins.getWebOrigins()).thenReturn(Set.of(origin));

        ClientModel clientWithoutOrigins = mock(ClientModel.class);
        when(clientWithoutOrigins.getWebOrigins()).thenReturn(null);

        when(clientProvider.getClientsStream(realm))
                .thenReturn(Stream.of(clientWithOrigins, clientWithoutOrigins));

        Response response = cors.buildCorsResponse("GET", Response.ok());

        assertEquals(origin, response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void buildCorsResponse_returnsResponseWithoutOrigin_whenClientStreamThrows() {
        when(httpHeaders.getHeaderString("Origin")).thenReturn("http://localhost:7080");
        when(clientProvider.getClientsStream(realm)).thenThrow(new RuntimeException("DB error"));

        Response response = cors.buildCorsResponse("GET", Response.ok());

        assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
    }

    @Test
    void buildCorsResponse_deduplicatesOrigins() {
        String origin = "http://localhost:7080";
        when(httpHeaders.getHeaderString("Origin")).thenReturn(origin);

        ClientModel client1 = mock(ClientModel.class);
        when(client1.getWebOrigins()).thenReturn(Set.of(origin));

        ClientModel client2 = mock(ClientModel.class);
        when(client2.getWebOrigins()).thenReturn(Set.of(origin));

        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.of(client1, client2));

        Response response = cors.buildCorsResponse("GET", Response.ok());

        assertEquals(origin, response.getHeaderString("Access-Control-Allow-Origin"));
    }
}
