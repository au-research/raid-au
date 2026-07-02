package au.org.raid.api.service.keycloak;

import au.org.raid.api.config.RaidPermissionsAuthProps;
import au.org.raid.api.service.keycloak.dto.AdminRaidsRequest;
import au.org.raid.api.service.keycloak.dto.RaidPermissionsResponse;
import au.org.raid.api.service.keycloak.dto.TokenRequest;
import au.org.raid.api.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakService {
    private final KeycloakApi keycloakApi;
    private final RaidPermissionsAuthProps authProps;

    public void addHandleToAdminRaids(final String handle) {
        log.debug("Requesting token for {}", authProps.getClientId());
        final var tokenResponse = keycloakApi.getToken(TokenRequest.builder()
                .clientId(authProps.getClientId())
                .clientSecret(authProps.getClientSecret())
                .grantType("client_credentials")
                .build());

        final var userId = TokenUtil.getUserId();

        log.debug("Adding handle {} to adminRaids for user {}", handle, userId);
        keycloakApi.addToAdminRaids(
                "Bearer %s".formatted(Objects.requireNonNull(tokenResponse.getBody()).getAccessToken()),
                AdminRaidsRequest.builder()
                        .handle(handle)
                        .userId(userId)
                        .build());
    }

    public RaidPermissionsResponse getRaidPermissions(final String userId) {
        final var tokenResponse = keycloakApi.getToken(TokenRequest.builder()
                .clientId(authProps.getClientId())
                .clientSecret(authProps.getClientSecret())
                .grantType("client_credentials")
                .build());

        final var response = keycloakApi.getRaidPermissions(
                "Bearer %s".formatted(Objects.requireNonNull(tokenResponse.getBody()).getAccessToken()),
                userId);

        return Objects.requireNonNull(response.getBody());
    }
}
