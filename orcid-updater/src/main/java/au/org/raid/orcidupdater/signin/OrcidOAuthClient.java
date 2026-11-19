package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.orcid.OrcidWriteException;
import au.org.raid.orcidupdater.orcid.OrcidWriteException.Kind;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * ORCID's three-legged OAuth: the authorisation URL a researcher is sent to, and the server-side
 * exchange of the code ORCID sends back. Codes and tokens are never logged.
 */
@Slf4j
@Component
public class OrcidOAuthClient {
    /**
     * Writing a membership needs /activities/update; /read-limited lets the updater read the
     * entries it wrote (ORCID-Source tutorial affiliations.md, verified on RAID-975, 2026-10-26).
     */
    static final String SCOPE = "/read-limited /activities/update";
    static final String CALLBACK_PATH = "/orcid/callback";
    private static final Pattern BARE_ORCID_ID = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$");

    private final RestClient restClient;
    private final OrcidUpdaterProperties properties;

    public OrcidOAuthClient(final RestClient.Builder builder, final OrcidUpdaterProperties properties) {
        this.restClient = builder.baseUrl(properties.getOrcid().getBaseUrl()).build();
        this.properties = properties;
    }

    public URI authorisationUri(final String state) {
        return UriComponentsBuilder.fromUriString(properties.getOrcid().getBaseUrl())
                .path("/oauth/authorize")
                .queryParam("client_id", properties.getOrcid().getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("redirect_uri", redirectUri())
                .queryParam("state", state)
                .encode()
                .build()
                .toUri();
    }

    /** Must be registered as a redirect URI on the RA's ORCID client (see README). */
    String redirectUri() {
        return properties.appUrl(CALLBACK_PATH);
    }

    /**
     * @return the tokens and the iD the researcher signed in with, as a URL on the configured
     *         ORCID host
     * @throws OrcidSignInFailedException ORCID refused the code, failed, or couldn't be reached
     */
    public OrcidSignIn exchange(final String code) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.getOrcid().getClientId());
        form.add("client_secret", properties.getOrcid().getClientSecret());
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri());

        final TokenResponse response;
        try {
            response = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (final HttpStatusCodeException e) {
            // the body is not logged: it can echo the request
            throw new OrcidSignInFailedException("ORCID token exchange returned " + e.getStatusCode().value());
        } catch (final ResourceAccessException e) {
            throw new OrcidSignInFailedException("ORCID token exchange failed: " + e.getClass().getSimpleName());
        }

        if (response == null || isBlank(response.accessToken()) || response.orcid() == null
                || !BARE_ORCID_ID.matcher(response.orcid()).matches()) {
            throw new OrcidSignInFailedException("ORCID token response had no access token or valid iD");
        }
        return new OrcidSignIn(properties.getOrcid().getBaseUrl().replaceAll("/+$", "") + "/" + response.orcid(),
                response.accessToken(), response.refreshToken(), response.scope());
    }

    /**
     * Revokes a token at ORCID, with its refresh token (ORCID-Source tutorial revoke.md: POST
     * /oauth/revoke with the client credentials and either token).
     *
     * @throws OrcidWriteException RETRYABLE when ORCID fails or can't be reached, REJECTED when it
     *                             refuses the request
     */
    public void revoke(final String accessToken) {
        final var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.getOrcid().getClientId());
        form.add("client_secret", properties.getOrcid().getClientSecret());
        form.add("token", accessToken);
        try {
            restClient.post()
                    .uri("/oauth/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (final HttpStatusCodeException e) {
            // the body is not kept: it can echo the request
            final var status = e.getStatusCode();
            final var message = "ORCID token revocation returned " + status.value();
            throw new OrcidWriteException(status.is5xxServerError() || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                    ? Kind.RETRYABLE : Kind.REJECTED, message);
        } catch (final ResourceAccessException e) {
            throw new OrcidWriteException(Kind.RETRYABLE, "ORCID token revocation failed: " + e.getClass().getSimpleName());
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("refresh_token") String refreshToken,
                         @JsonProperty("scope") String scope,
                         @JsonProperty("orcid") String orcid) {
        @Override
        public String toString() {
            return "TokenResponse[orcid=%s, scope=%s, tokens=redacted]".formatted(orcid, scope);
        }
    }

    /** Never print the tokens. */
    public record OrcidSignIn(String orcidId, String accessToken, String refreshToken, String scope) {
        @Override
        public String toString() {
            return "OrcidSignIn[orcidId=%s, scope=%s, tokens=redacted]".formatted(orcidId, scope);
        }
    }
}
