package au.org.raid.api.service.keycloak;

import au.org.raid.api.config.RaidPermissionsAuthProps;
import au.org.raid.api.service.keycloak.dto.RaidPermissionsResponse;
import au.org.raid.api.service.keycloak.dto.TokenRequest;
import au.org.raid.api.service.keycloak.dto.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    private static final String CLIENT_ID = "raid-permissions-admin";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String USER_ID = "test-user-id";

    @Mock
    private KeycloakApi keycloakApi;

    @Mock
    private RaidPermissionsAuthProps authProps;

    @InjectMocks
    private KeycloakService keycloakService;

    @Test
    @DisplayName("getRaidPermissions authenticates with client credentials and returns permissions")
    void getRaidPermissions() {
        final var tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);

        final var permissionsResponse = new RaidPermissionsResponse(
                List.of("test/handle-1"),
                List.of("test/handle-2")
        );

        when(authProps.getClientId()).thenReturn(CLIENT_ID);
        when(authProps.getClientSecret()).thenReturn(CLIENT_SECRET);
        when(keycloakApi.getToken(any(TokenRequest.class))).thenReturn(ResponseEntity.ok(tokenResponse));
        when(keycloakApi.getRaidPermissions(eq("Bearer " + ACCESS_TOKEN), eq(USER_ID)))
                .thenReturn(ResponseEntity.ok(permissionsResponse));

        final var result = keycloakService.getRaidPermissions(USER_ID);

        assertThat(result.getUserRaids(), is(List.of("test/handle-1")));
        assertThat(result.getAdminRaids(), is(List.of("test/handle-2")));

        verify(keycloakApi).getToken(TokenRequest.builder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .grantType("client_credentials")
                .build());
    }

    @Test
    @DisplayName("getRaidPermissions returns empty lists when user has no permissions")
    void getRaidPermissions_emptyLists() {
        final var tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(ACCESS_TOKEN);

        final var permissionsResponse = new RaidPermissionsResponse(List.of(), List.of());

        when(authProps.getClientId()).thenReturn(CLIENT_ID);
        when(authProps.getClientSecret()).thenReturn(CLIENT_SECRET);
        when(keycloakApi.getToken(any(TokenRequest.class))).thenReturn(ResponseEntity.ok(tokenResponse));
        when(keycloakApi.getRaidPermissions(eq("Bearer " + ACCESS_TOKEN), eq(USER_ID)))
                .thenReturn(ResponseEntity.ok(permissionsResponse));

        final var result = keycloakService.getRaidPermissions(USER_ID);

        assertThat(result.getUserRaids(), is(List.of()));
        assertThat(result.getAdminRaids(), is(List.of()));
    }
}
