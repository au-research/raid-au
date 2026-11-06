package au.org.raid.iam.provider.federation;

import au.org.raid.iam.provider.federation.JwksReachabilityCheck.Fetched;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** RAID-982: a JWKS Keycloak may not reach is reported at boot as a warning naming the URL, never a failure. */
class JwksReachabilityCheckTest {

    private static final String URL = "https://keys.raid.org/federation/jwks.json";
    private static final String ONE_KEY = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"k\",\"n\":\"sXch\",\"e\":\"AQAB\"}]}";

    private final List<String> warnings = new ArrayList<>();

    private void run(final JwksReachabilityCheck.Fetcher fetcher, final boolean embeddedKeysAvailable) {
        JwksReachabilityCheck.run(URL, fetcher, embeddedKeysAvailable, warnings::add);
    }

    @Test
    void reachableJwksLogsNoWarning() {
        run(url -> new Fetched(200, ONE_KEY), false);

        assertThat(warnings, is(empty()));
    }

    @Test
    void fetchFailureWarnsNamingTheUrlAndProxy() {
        run(url -> {
            throw new RuntimeException("wrapped", new ConnectException("Connection refused"));
        }, false);

        assertThat(warnings, hasSize(1));
        assertThat(warnings.get(0), allOf(containsString(URL), containsString("may be unreachable"),
                containsString("ConnectException: Connection refused"), containsString("egress proxy")));
    }

    @Test
    void doesNotSuggestTheEmbeddedKeySetWhileItIsEmpty() {
        // Until RAID-983 ships keys, following that advice would switch harvesting off.
        run(url -> new Fetched(404, "not found"), false);

        assertThat(warnings.get(0), not(containsString("RAID_FEDERATION_HARVEST_USE_JWKS_URL")));
        assertThat(warnings.get(0), containsString("RAID_FEDERATION_HARVEST_JWKS_URL"));
    }

    @Test
    void suggestsTheEmbeddedKeySetOnceItHasKeys() {
        run(url -> new Fetched(404, "not found"), true);

        assertThat(warnings.get(0), containsString("RAID_FEDERATION_HARVEST_USE_JWKS_URL=false"));
    }

    @Test
    void errorStatusWarns() {
        run(url -> new Fetched(503, ""), false);

        assertThat(warnings.get(0), allOf(containsString(URL), containsString("HTTP 503")));
    }

    @Test
    void emptyKeySetWarns() {
        run(url -> new Fetched(200, "{\"keys\":[]}"), false);

        assertThat(warnings.get(0), containsString("returned no keys"));
    }

    @Test
    void nonJwksBodyWarns() {
        run(url -> new Fetched(200, "<html>captive portal</html>"), false);

        assertThat(warnings.get(0), containsString("did not return a valid JWKS"));
    }

    @Test
    void failingWarnSinkDoesNotThrow() {
        assertDoesNotThrow(() -> JwksReachabilityCheck.run(URL, url -> new Fetched(500, ""), false, message -> {
            throw new IllegalStateException("logger broken");
        }));
    }

    @Test
    void rootMessageStopsOnACauseCycle() {
        var a = new RuntimeException("a");
        var b = new RuntimeException("b", a);
        a.initCause(b);

        assertThat(JwksReachabilityCheck.rootMessage(b), startsWith("RuntimeException"));
    }
}
