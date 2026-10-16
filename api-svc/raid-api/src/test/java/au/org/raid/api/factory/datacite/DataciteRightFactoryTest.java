package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteRight;
import au.org.raid.idl.raidv2.model.Id;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DataciteRightFactoryTest {

    private final DataciteRightFactory dataciteRightFactory = new DataciteRightFactory();

    private static DataciteRight cc0() {
        return new DataciteRight()
                .setRights("Creative Commons Zero v1.0 Universal")
                .setRightsURI("https://creativecommons.org/publicdomain/zero/1.0/legalcode")
                .setRightsIdentifier("cc0-1.0")
                .setRightsIdentifierScheme("SPDX")
                .setSchemeUri("https://spdx.org/licenses/");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Creative Commons CC-0",
            "  Creative Commons CC-0  ",
            "CC0",
            "cc-0",
            "CC0 1.0",
            "https://creativecommons.org/publicdomain/zero/1.0/",
            "https://creativecommons.org/publicdomain/zero/1.0/legalcode"
    })
    @DisplayName("CC0 licence variants map to the canonical SPDX rights entry")
    void mapsCc0ToSpdxEntry(final String license) {
        final var result = dataciteRightFactory.create(new Id().license(license));

        assertThat(result).containsExactly(cc0());
    }

    @Test
    @DisplayName("An unrecognised licence is emitted as rights text only")
    void unrecognisedLicenceIsRightsTextOnly() {
        final var result = dataciteRightFactory.create(new Id().license("Some Bespoke Licence"));

        assertThat(result).containsExactly(new DataciteRight().setRights("Some Bespoke Licence"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("A blank licence produces an empty rightsList")
    void blankLicenceProducesEmptyList(final String license) {
        assertThat(dataciteRightFactory.create(new Id().license(license))).isEmpty();
    }

    @Test
    @DisplayName("A null identifier produces an empty rightsList")
    void nullIdentifierProducesEmptyList() {
        assertThat(dataciteRightFactory.create(null)).isEmpty();
    }

    @Test
    @DisplayName("CC0 serialises with DataCite REST API keys")
    void cc0SerialisesWithDataciteKeys() throws Exception {
        final var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(
                dataciteRightFactory.create(new Id().license("Creative Commons CC-0")).get(0)));

        assertThat(json.path("rights").asText()).isEqualTo("Creative Commons Zero v1.0 Universal");
        assertThat(json.path("rightsUri").asText()).isEqualTo("https://creativecommons.org/publicdomain/zero/1.0/legalcode");
        assertThat(json.path("rightsIdentifier").asText()).isEqualTo("cc0-1.0");
        assertThat(json.path("rightsIdentifierScheme").asText()).isEqualTo("SPDX");
        assertThat(json.path("schemeUri").asText()).isEqualTo("https://spdx.org/licenses/");
        assertThat(json.has("rightsURI")).isFalse();
    }

    @Test
    @DisplayName("Rights text only omits null URI and identifier keys")
    void rightsTextOnlyOmitsNullKeys() throws Exception {
        final var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(
                dataciteRightFactory.create(new Id().license("Some Bespoke Licence")).get(0)));

        assertThat(json.size()).isEqualTo(1);
        assertThat(json.path("rights").asText()).isEqualTo("Some Bespoke Licence");
    }
}
