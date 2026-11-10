package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteRight;
import au.org.raid.idl.raidv2.model.Id;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Maps a RAiD's {@code identifier.license} to a DataCite {@code rightsList}.
 * <p>
 * The licence is free text (configured as "Creative Commons CC-0"), so CC0 is recognised and emitted
 * as a canonical SPDX entry. Any other licence is passed through as rights text only, because a URI
 * or identifier can't be derived from it safely.
 */
@Component
public class DataciteRightFactory {
    static final String CC0_RIGHTS = "Creative Commons Zero v1.0 Universal";
    static final String CC0_RIGHTS_URI = "https://creativecommons.org/publicdomain/zero/1.0/legalcode";
    static final String CC0_SPDX_IDENTIFIER = "cc0-1.0";
    static final String SPDX_SCHEME = "SPDX";
    static final String SPDX_SCHEME_URI = "https://spdx.org/licenses/";

    public List<DataciteRight> create(final Id identifier) {
        if (identifier == null || isBlank(identifier.getLicense())) {
            return List.of();
        }

        final var license = identifier.getLicense().trim();

        if (isCc0(license)) {
            return List.of(new DataciteRight()
                    .setRights(CC0_RIGHTS)
                    .setRightsURI(CC0_RIGHTS_URI)
                    .setRightsIdentifier(CC0_SPDX_IDENTIFIER)
                    .setRightsIdentifierScheme(SPDX_SCHEME)
                    .setSchemeUri(SPDX_SCHEME_URI));
        }

        return List.of(new DataciteRight().setRights(license));
    }

    private boolean isCc0(final String license) {
        final var normalised = license.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-_]", "");
        return normalised.contains("cc0") || normalised.contains("creativecommons.org/publicdomain/zero/1.0");
    }
}
