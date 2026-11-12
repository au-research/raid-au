package au.org.raid.orcidupdater.serviceauth;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Gets the updater's own access token from Keycloak with the client credentials grant,
 * authenticating with a JWT signed by the service key (private_key_jwt, RFC 7523). Tokens are
 * reused until shortly before they expire.
 */
@Slf4j
@Component
public class ServiceTokenClient {
    static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final Duration ASSERTION_LIFETIME = Duration.ofSeconds(60);
    /** Renew this long before expiry, so a token never expires between fetching and use. */
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final ServiceKeyStore serviceKeyStore;
    private final String issuer;
    private final String clientId;
    private final Clock clock;

    private String token;
    private Instant renewAt = Instant.MIN;

    public ServiceTokenClient(final RestClient.Builder builder, final ServiceKeyStore serviceKeyStore,
                              final OrcidUpdaterProperties properties, final Clock clock) {
        this.issuer = properties.getKeycloak().getIssuerUri().replaceAll("/+$", "");
        this.restClient = builder.build();
        this.serviceKeyStore = serviceKeyStore;
        this.clientId = properties.getRaidApi().getClientId();
        this.clock = clock;
    }

    /** @throws ServiceTokenException Keycloak refused the request or couldn't be reached */
    public synchronized String accessToken() {
        final var now = clock.instant();
        if (token != null && now.isBefore(renewAt)) {
            return token;
        }
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_assertion_type", ASSERTION_TYPE);
        form.add("client_assertion", assertion(now));

        final JsonNode response;
        try {
            response = restClient.post()
                    .uri(issuer + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (final HttpStatusCodeException e) {
            // Keycloak's error code only: the body never holds a token on failure, but keep it short
            throw new ServiceTokenException("Keycloak returned %d for the updater's token request (%s)"
                    .formatted(e.getStatusCode().value(), errorCode(e)));
        } catch (final ResourceAccessException e) {
            throw new ServiceTokenException("Keycloak unreachable for the updater's token request");
        }
        if (response == null || !response.hasNonNull("access_token")) {
            throw new ServiceTokenException("Keycloak's token response has no access token");
        }
        token = response.get("access_token").asText();
        renewAt = now.plusSeconds(response.path("expires_in").asLong(60)).minus(EXPIRY_MARGIN);
        return token;
    }

    /** Forgets the cached token, e.g. after the RAiD API rejected it. */
    public synchronized void reset() {
        token = null;
    }

    String assertion(final Instant now) {
        final var key = serviceKeyStore.signingKey();
        final var claims = new JWTClaimsSet.Builder()
                .issuer(clientId)
                .subject(clientId)
                // Keycloak accepts its issuer URL as the audience
                .audience(issuer)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ASSERTION_LIFETIME)))
                .build();
        try {
            final var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(), claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (final JOSEException e) {
            throw new IllegalStateException("Couldn't sign the token request", e);
        }
    }

    private static String errorCode(final HttpStatusCodeException e) {
        try {
            final var error = e.getResponseBodyAs(JsonNode.class);
            final var code = error == null ? null : error.path("error").asText(null);
            return code == null ? "no error code" : code.replaceAll("[^a-z_]", "");
        } catch (final RuntimeException parseFailure) {
            return "no error code";
        }
    }
}
