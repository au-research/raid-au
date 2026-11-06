package au.org.raid.iam.provider.federation;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** RAID-982: where the harvest client's keys come from, resolved from the environment. */
class FederationHarvestKeySourceTest {

    private static final String TWO_KEYS = """
            {"keys":[
              {"kty":"RSA","kid":"current","use":"sig","alg":"RS256","n":"sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw","e":"AQAB"},
              {"kty":"RSA","kid":"next","use":"sig","alg":"RS256","n":"sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw","e":"AQAB"}
            ]}""";

    private static UnaryOperator<String> env(final Map<String, String> values) {
        return values::get;
    }

    @Test
    void defaultsToTheProductionJwksUrl() {
        var resolution = FederationHarvestKeySource.resolve(env(Map.of()), () -> TWO_KEYS);

        assertThat(resolution.error(), is(nullValue()));
        assertThat(resolution.keySource().usesUrl(), is(true));
        assertThat(resolution.keySource().jwksUrl(), is("https://keys.raid.org/federation/jwks.json"));
    }

    @Test
    void overrideReplacesTheDefaultUrl() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", " https://example.test/jwks.json ")), () -> TWO_KEYS);

        assertThat(resolution.keySource().jwksUrl(), is("https://example.test/jwks.json"));
    }

    @Test
    void blankOverrideUsesTheDefault() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "  ")), () -> TWO_KEYS);

        assertThat(resolution.keySource().jwksUrl(), is(FederationHarvestKeySource.DEFAULT_JWKS_URL));
    }

    @Test
    void invalidOverrideIsRefusedRatherThanFallingBackToProductionKeys() {
        for (var bad : new String[]{"keys.raid.org/jwks.json", "ftp://example.test/jwks.json", "https://", "not a url",
                "http://example.test/jwks.json"}) {
            var resolution = FederationHarvestKeySource.resolve(
                    env(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", bad)), () -> TWO_KEYS);

            assertThat(bad, resolution.keySource(), is(nullValue()));
            assertThat(bad, resolution.error(), containsString("RAID_FEDERATION_HARVEST_JWKS_URL is ignored"));
        }
    }

    @Test
    void useJwksUrlFalseSelectsTheEmbeddedKeySet() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_USE_JWKS_URL", " FALSE ",
                        "RAID_FEDERATION_HARVEST_JWKS_URL", "https://example.test/jwks.json")), () -> TWO_KEYS);

        assertThat(resolution.error(), is(nullValue()));
        assertThat(resolution.keySource().usesUrl(), is(false));
        assertThat(resolution.keySource().jwksString(), is(TWO_KEYS.trim()));
        assertThat(resolution.bundledKeyCount(), is(2));
    }

    @Test
    void anyOtherUseJwksUrlValueKeepsTheUrl() {
        for (var value : new String[]{"true", "no", "0", "flase", ""}) {
            var resolution = FederationHarvestKeySource.resolve(
                    env(Map.of("RAID_FEDERATION_HARVEST_USE_JWKS_URL", value)), () -> TWO_KEYS);

            assertThat(value, resolution.keySource().usesUrl(), is(true));
        }
    }

    @Test
    void embeddedKeySetThatIsNotAJwksIsRefused() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_USE_JWKS_URL", "false")), () -> "not json");

        assertThat(resolution.keySource(), is(nullValue()));
        assertThat(resolution.error(), containsString("is not a valid JWKS"));
    }

    @Test
    void embeddedKeySetThatCannotBeReadIsRefused() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_USE_JWKS_URL", "false")),
                () -> {
                    throw new IllegalStateException("resource not found");
                });

        assertThat(resolution.keySource(), is(nullValue()));
        assertThat(resolution.error(), containsString("could not be read: resource not found"));
    }

    @Test
    void bundledKeySetShipsEmptyUntilRaid983() {
        // RAID-1042 decision: the RAID-982 release ships the mechanism with no keys.
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_USE_JWKS_URL", "false")));

        assertThat(resolution.error(), is(nullValue()));
        assertThat(resolution.keySource().usesUrl(), is(false));
        assertThat(resolution.bundledKeyCount(), is(0));
    }

    @Test
    void exactlyOneKeySourceMustBeSet() {
        assertThrows(IllegalArgumentException.class, () -> new FederationHarvestKeySource(null, null));
        assertThrows(IllegalArgumentException.class, () -> new FederationHarvestKeySource("https://a/b", "{}"));
    }

    @Test
    void plainHttpIsRefusedWithoutTheOptIn() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "http://jwks:8080/jwks.json")), () -> TWO_KEYS);

        assertThat(resolution.keySource(), is(nullValue()));
        assertThat(resolution.error(), containsString("RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL=true"));
    }

    @Test
    void plainHttpIsAcceptedWithTheOptInAndFlaggedInsecure() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_JWKS_URL", "http://jwks:8080/jwks.json",
                        "RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL", "true")), () -> TWO_KEYS);

        assertThat(resolution.keySource().jwksUrl(), is("http://jwks:8080/jwks.json"));
        assertThat(resolution.insecureHttp(), is(true));
    }

    @Test
    void httpsIsNotFlaggedInsecureEvenWithTheOptIn() {
        var resolution = FederationHarvestKeySource.resolve(
                env(Map.of("RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL", "true")), () -> TWO_KEYS);

        assertThat(resolution.insecureHttp(), is(false));
    }

    @Test
    void bundledKeyCountIsReportedInUrlModeToo() {
        var resolution = FederationHarvestKeySource.resolve(env(Map.of()), () -> TWO_KEYS);

        assertThat(resolution.keySource().usesUrl(), is(true));
        assertThat(resolution.bundledKeyCount(), is(2));
    }
}
