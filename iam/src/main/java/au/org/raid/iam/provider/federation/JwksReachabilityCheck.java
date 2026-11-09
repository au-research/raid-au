package au.org.raid.iam.provider.federation;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.JsonSerialization;

import java.util.function.Consumer;

/**
 * Warns at boot when this Keycloak may be unable to fetch the Registration Authority's JWKS.
 *
 * <p>Keycloak only fetches the JWKS lazily, on the first token request. Without this check an agency
 * that blocks outbound HTTPS deploys cleanly and finds out only when ARDC first harvests.
 *
 * <p>The fetch goes through Keycloak's own {@code HttpClientProvider} (via {@link SimpleHttp}), the
 * same client Keycloak uses to load client JWKS, so it honours the same proxy and truststore
 * configuration and does not warn about a JWKS Keycloak can in fact reach.
 *
 * <p>It runs on a daemon thread and never blocks or fails boot: any problem is logged as a warning
 * naming the URL (without its query string, which may hold credentials), and nothing else changes.
 */
@Slf4j
final class JwksReachabilityCheck {

    static final int TIMEOUT_MILLIS = 10_000;
    static final long MAX_BODY_BYTES = 64 * 1024;

    /** The outcome of one fetch: an HTTP status and body. */
    record Fetched(int status, String body) {
    }

    @FunctionalInterface
    interface Fetcher {
        Fetched fetch(String url) throws Exception;
    }

    private JwksReachabilityCheck() {
    }

    /** Starts the check on a daemon thread and returns at once. */
    static void start(final KeycloakSessionFactory factory, final String jwksUrl, final boolean embeddedKeysAvailable) {
        final var thread = new Thread(
                () -> run(jwksUrl, url -> fetchWithKeycloak(factory, url), embeddedKeysAvailable,
                        JwksReachabilityCheck::warn),
                "raid-federation-harvest-jwks-check");
        thread.setDaemon(true);
        thread.start();
    }

    static void run(final String jwksUrl, final Fetcher fetcher, final boolean embeddedKeysAvailable,
                    final Consumer<String> warn) {
        String problem;
        try {
            final var fetched = fetcher.fetch(jwksUrl);
            problem = problemWith(fetched);
        } catch (Exception e) {
            // An HTTP client's exception message can quote the request URL.
            problem = "it could not be fetched (" + FederationHarvestKeySource.withoutSecrets(rootMessage(e), jwksUrl) + ")";
        }
        if (problem == null) {
            log.debug("Federation harvest: JWKS {} is reachable", FederationHarvestKeySource.loggable(jwksUrl));
            return;
        }
        try {
            warn.accept(message(jwksUrl, problem, embeddedKeysAvailable));
        } catch (RuntimeException e) {
            // Logging must not take the check's thread down noisily; nothing else to do.
        }
    }

    private static Fetched fetchWithKeycloak(final KeycloakSessionFactory factory, final String url) throws Exception {
        try (var session = factory.create()) {
            final var config = RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_MILLIS)
                    .setConnectionRequestTimeout(TIMEOUT_MILLIS)
                    .setSocketTimeout(TIMEOUT_MILLIS)
                    .build();
            try (var response = SimpleHttp.create(session)
                    .withRequestConfig(config)
                    .withMaxConsumedResponseSize(MAX_BODY_BYTES)
                    .doGet(url)
                    .acceptJson()
                    .asResponse()) {
                return new Fetched(response.getStatus(), response.asString());
            }
        }
    }

    static String problemWith(final Fetched fetched) {
        if (fetched.status() < 200 || fetched.status() >= 300) {
            return "it returned HTTP " + fetched.status();
        }
        try {
            final var keySet = JsonSerialization.readValue(fetched.body(), JSONWebKeySet.class);
            if (keySet == null || keySet.getKeys() == null || keySet.getKeys().length == 0) {
                return "it returned no keys";
            }
            return null;
        } catch (Exception e) {
            return "it did not return a valid JWKS";
        }
    }

    static String message(final String jwksUrl, final String problem, final boolean embeddedKeysAvailable) {
        final var remedy = embeddedKeysAvailable
                ? "Allow outbound HTTPS to it (check any egress proxy), set " +
                  FederationHarvestKeySource.JWKS_URL_ENV_VAR + ", or set " +
                  FederationHarvestKeySource.USE_JWKS_URL_ENV_VAR + "=false to use the key set embedded in raid-iam.jar."
                : "Allow outbound HTTPS to it (check any egress proxy), or set " +
                  FederationHarvestKeySource.JWKS_URL_ENV_VAR + " if this instance should trust a different key set.";
        return "Federation harvest: the JWKS " + FederationHarvestKeySource.loggable(jwksUrl) + " may be unreachable: " + problem +
                ". Keycloak has started and the harvest client exists, but the Registration Authority's harvest " +
                "will fail if this Keycloak cannot fetch that URL when harvesting. " + remedy;
    }

    static String rootMessage(final Throwable error) {
        var cause = error;
        var depth = 0;
        while (cause.getCause() != null && cause.getCause() != cause && depth++ < 20) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
    }

    private static void warn(final String message) {
        log.warn(message);
    }
}
