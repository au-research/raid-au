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

import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorsTest {

    @Mock private KeycloakSession session;
    @Mock private KeycloakContext context;
    @Mock private RealmModel realm;
    @Mock private HttpHeaders headers;
    @Mock private ClientProvider clientProvider;

    private Cors cors;

    @BeforeEach
    void setUp() {
        cors = new Cors(session, new ObjectMapper());
    }

    private void setupRequestHeaders(String origin) {
        when(session.getContext()).thenReturn(context);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(headers.getHeaderString("Origin")).thenReturn(origin);
    }

    private void setupOrigin(String origin) {
        setupRequestHeaders(origin);
        when(context.getRealm()).thenReturn(realm);
        when(session.clients()).thenReturn(clientProvider);
    }

    private void setupAllowedOrigins(String... origins) {
        var client = mock(ClientModel.class);
        when(client.getWebOrigins()).thenReturn(Set.of(origins));
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.of(client));
    }

    @Test
    void buildOptionsResponse_returnsOkWithMethodHeaders() {
        setupOrigin("http://localhost:7080");
        setupAllowedOrigins("http://localhost:7080");

        var response = cors.buildOptionsResponse("GET", "PUT", "OPTIONS");

        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaderString("Access-Control-Allow-Methods"), is("GET, PUT, OPTIONS"));
        assertThat(response.getHeaderString("Access-Control-Allow-Headers"), is("Authorization,Content-Type"));
    }

    @Test
    void buildCorsResponse_setsAllowOriginWhenOriginIsAllowed() {
        setupOrigin("http://localhost:7080");
        setupAllowedOrigins("http://localhost:7080");

        var response = cors.buildCorsResponse("GET", Response.ok().entity("{}"));

        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is("http://localhost:7080"));
        assertThat(response.getHeaderString("Access-Control-Allow-Credentials"), is("true"));
    }

    @Test
    void buildCorsResponse_doesNotSetOriginWhenNotAllowed() {
        setupOrigin("http://evil.com");
        setupAllowedOrigins("http://localhost:7080");

        var response = cors.buildCorsResponse("GET", Response.ok());

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is(nullValue()));
        assertThat(response.getHeaderString("Access-Control-Allow-Credentials"), is(nullValue()));
    }

    @Test
    void buildCorsResponse_doesNotSetOriginWhenOriginHeaderIsNull() {
        setupRequestHeaders(null);

        var response = cors.buildCorsResponse("GET", Response.ok());

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is(nullValue()));
    }

    @Test
    void buildCorsResponse_collectsOriginsFromMultipleClients() {
        setupOrigin("https://app.test.raid.org.au");

        var client1 = mock(ClientModel.class);
        when(client1.getWebOrigins()).thenReturn(Set.of("http://localhost:7080"));
        var client2 = mock(ClientModel.class);
        when(client2.getWebOrigins()).thenReturn(Set.of("https://app.test.raid.org.au"));
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.of(client1, client2));

        var response = cors.buildCorsResponse("GET", Response.ok());

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is("https://app.test.raid.org.au"));
    }

    @Test
    void buildCorsResponse_handlesClientWithNullWebOrigins() {
        setupOrigin("http://localhost:7080");

        var clientWithNull = mock(ClientModel.class);
        when(clientWithNull.getWebOrigins()).thenReturn(null);
        var clientWithOrigins = mock(ClientModel.class);
        when(clientWithOrigins.getWebOrigins()).thenReturn(Set.of("http://localhost:7080"));
        when(clientProvider.getClientsStream(realm)).thenReturn(Stream.of(clientWithNull, clientWithOrigins));

        var response = cors.buildCorsResponse("GET", Response.ok());

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is("http://localhost:7080"));
    }

    @Test
    void buildCorsResponse_returnsEmptyOriginsOnException() {
        when(session.getContext()).thenReturn(context);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(headers.getHeaderString("Origin")).thenReturn("http://localhost:7080");
        when(context.getRealm()).thenThrow(new RuntimeException("test error"));

        var response = cors.buildCorsResponse("GET", Response.ok());

        assertThat(response.getHeaderString("Access-Control-Allow-Origin"), is(nullValue()));
    }
}
