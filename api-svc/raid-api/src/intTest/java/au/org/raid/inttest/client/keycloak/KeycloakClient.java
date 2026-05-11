package au.org.raid.inttest.client.keycloak;

import au.org.raid.idl.raidv2.api.RaidApi;
import au.org.raid.inttest.config.AuthConfig;
import au.org.raid.inttest.service.TokenService;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.jackson3.Jackson3Decoder;
import feign.jackson3.Jackson3Encoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@RequiredArgsConstructor
public class KeycloakClient {
    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private final Contract contract;
    @Value("${raid.iam.base-url}")
    private String apiUrl;
    private final TokenService tokenService;

    public KeycloakApi keycloakApi(
            final String token
    ) {
        return Feign.builder()
                .options(
                        new Request.Options(10, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, false)
                )

                .client(new OkHttpClient())
                .encoder(new Jackson3Encoder(objectMapper))
                .decoder(new ResponseEntityDecoder(new Jackson3Decoder(objectMapper)))
//                .errorDecoder(new RaidApiExceptionDecoder(objectMapper))
                .contract(contract)
                .requestInterceptor(request -> request.header(AUTHORIZATION, "Bearer " + token))
                .logger(new Slf4jLogger(RaidApi.class))
                .logLevel(Logger.Level.FULL)
                .target(KeycloakApi.class, apiUrl);
    }

    public KeycloakApi keycloakApi(AuthConfig.Client client) {
        final var token = tokenService.getClientToken(client.getClientId(), client.getClientSecret());
        return keycloakApi(token);
    }
}