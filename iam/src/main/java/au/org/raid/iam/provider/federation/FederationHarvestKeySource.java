package au.org.raid.iam.provider.federation;

import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Where the federation harvest client gets the Registration Authority's public keys from (RAID-982).
 *
 * <p>Exactly one of {@link #jwksUrl()} and {@link #jwksString()} is set:
 * <ul>
 *   <li>URL mode (the default): Keycloak fetches the JWKS from {@link #DEFAULT_JWKS_URL}, or from
 *   {@value #JWKS_URL_ENV_VAR} when set. Non-production instances must set the override, because
 *   the default is the production key set. The URL must be https: whoever can tamper with the JWKS
 *   fetch can mint harvest tokens. Plain http is accepted only with
 *   {@value #ALLOW_HTTP_JWKS_URL_ENV_VAR}{@code =true}, for local and test rigs.</li>
 *   <li>Embedded mode ({@value #USE_JWKS_URL_ENV_VAR}{@code =false}): the client holds the key set
 *   bundled in this jar ({@value #EMBEDDED_JWKS_RESOURCE}, next to this class), for agencies whose
 *   Keycloak cannot make outbound HTTPS calls. The bundled set is empty until RAID-983 creates the
 *   KMS keys.</li>
 * </ul>
 *
 * <p>Decisions recorded in {@code doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md}.
 */
public record FederationHarvestKeySource(String jwksUrl, String jwksString) {

    public static final String DEFAULT_JWKS_URL = "https://keys.raid.org/federation/jwks.json";
    public static final String JWKS_URL_ENV_VAR = "RAID_FEDERATION_HARVEST_JWKS_URL";
    public static final String USE_JWKS_URL_ENV_VAR = "RAID_FEDERATION_HARVEST_USE_JWKS_URL";
    public static final String ALLOW_HTTP_JWKS_URL_ENV_VAR = "RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL";
    /** Resolved relative to this class's package, so no other provider jar can shadow it. */
    public static final String EMBEDDED_JWKS_RESOURCE = "harvest-jwks.json";

    public FederationHarvestKeySource {
        if ((jwksUrl == null) == (jwksString == null)) {
            throw new IllegalArgumentException("Exactly one of jwksUrl and jwksString must be set");
        }
    }

    public static FederationHarvestKeySource url(final String jwksUrl) {
        return new FederationHarvestKeySource(jwksUrl, null);
    }

    public static FederationHarvestKeySource embedded(final String jwksString) {
        return new FederationHarvestKeySource(null, jwksString);
    }

    public boolean usesUrl() {
        return jwksUrl != null;
    }

    /**
     * The resolved key source, or why the configuration was refused. {@code bundledKeyCount} is the
     * number of keys in the bundled set (or 0 when it cannot be read), whichever mode is chosen.
     * {@code insecureHttp} is true when a plain-http URL was accepted through the opt-in.
     */
    public record Resolution(FederationHarvestKeySource keySource, String error, int bundledKeyCount,
                             boolean insecureHttp) {
        static Resolution refused(final String error, final int bundledKeyCount) {
            return new Resolution(null, error, bundledKeyCount, false);
        }
    }

    /** Resolves the key source from the environment and the bundled key set. */
    public static Resolution resolve(final UnaryOperator<String> environment) {
        return resolve(environment, FederationHarvestKeySource::readBundledJwks);
    }

    static Resolution resolve(final UnaryOperator<String> environment, final Supplier<String> bundledJwks) {
        final var bundled = readBundled(bundledJwks);

        if (isFalse(environment.apply(USE_JWKS_URL_ENV_VAR))) {
            if (bundled.error() != null) {
                return Resolution.refused(bundled.error(), 0);
            }
            return new Resolution(embedded(bundled.json()), null, bundled.keyCount(), false);
        }

        final var override = environment.apply(JWKS_URL_ENV_VAR);
        final var jwksUrl = override == null || override.isBlank() ? DEFAULT_JWKS_URL : override.trim();
        final var allowHttp = isTrue(environment.apply(ALLOW_HTTP_JWKS_URL_ENV_VAR));
        final var invalid = invalidJwksUrlReason(jwksUrl, allowHttp);
        if (invalid != null) {
            // Never fall back to the default on a bad override: on a non-production instance that
            // would silently trust the production Registration Authority's keys.
            return Resolution.refused(JWKS_URL_ENV_VAR + " is ignored because " + invalid, bundled.keyCount());
        }
        final var insecure = URI.create(jwksUrl).getScheme().equalsIgnoreCase("http");
        return new Resolution(url(jwksUrl), null, bundled.keyCount(), insecure);
    }

    private record Bundled(String json, int keyCount, String error) {
    }

    private static Bundled readBundled(final Supplier<String> bundledJwks) {
        final String json;
        try {
            json = bundledJwks.get();
        } catch (RuntimeException e) {
            return new Bundled(null, 0, "the bundled key set " + EMBEDDED_JWKS_RESOURCE +
                    " could not be read: " + e.getMessage());
        }
        try {
            final var keySet = JsonSerialization.readValue(json, JSONWebKeySet.class);
            if (keySet == null) {
                return new Bundled(null, 0, "the bundled key set " + EMBEDDED_JWKS_RESOURCE + " is empty");
            }
            final var keys = keySet.getKeys();
            return new Bundled(json.trim(), keys == null ? 0 : keys.length, null);
        } catch (IOException | RuntimeException e) {
            return new Bundled(null, 0, "the bundled key set " + EMBEDDED_JWKS_RESOURCE + " is not a valid JWKS");
        }
    }

    private static boolean isFalse(final String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).equals("false");
    }

    private static boolean isTrue(final String value) {
        return value != null && value.trim().toLowerCase(Locale.ROOT).equals("true");
    }

    /** Returns null when the URL is acceptable, otherwise why not. */
    static String invalidJwksUrlReason(final String jwksUrl, final boolean allowHttp) {
        try {
            final var uri = new URI(jwksUrl);
            final var scheme = uri.getScheme();
            if (scheme == null) {
                return "it must be an https URL";
            }
            if (scheme.equalsIgnoreCase("http")) {
                if (!allowHttp) {
                    return "it must be an https URL (plain http needs " + ALLOW_HTTP_JWKS_URL_ENV_VAR + "=true)";
                }
            } else if (!scheme.equalsIgnoreCase("https")) {
                return "it must be an https URL";
            }
            if (uri.getHost() == null) {
                return "it has no host";
            }
            return null;
        } catch (Exception e) {
            return "it is not a valid URL";
        }
    }

    private static String readBundledJwks() {
        try (InputStream in = FederationHarvestKeySource.class.getResourceAsStream(EMBEDDED_JWKS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("resource not found");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
