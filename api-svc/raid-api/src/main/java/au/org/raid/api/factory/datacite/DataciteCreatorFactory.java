package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.contributor.isni.IsniClient;
import au.org.raid.api.client.contributor.orcid.OrcidClient;
import au.org.raid.api.model.datacite.doi.DataciteCreator;
import au.org.raid.api.model.datacite.doi.NameIdentifier;
import au.org.raid.idl.raidv2.model.Contributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataciteCreatorFactory {
    private static final String ORCID_SCHEMA_URI = "https://orcid.org/";
    private static final String ISNI_SCHEMA_URI = "https://isni.org/";
    private final OrcidClient orcidClient;
    private final IsniClient isniClient;

    private static final Map<String, String> NAME_IDENTIFIER_SCHEMA_MAP = Map.of(
            ORCID_SCHEMA_URI, "ORCID",
            ISNI_SCHEMA_URI, "ISNI"
    );

    public DataciteCreator create(final Contributor contributor) {
        final var creator = new DataciteCreator();
        String name;

        if (contributor.getSchemaUri().equals(ORCID_SCHEMA_URI)) {
            name = orcidClient.getName(contributor.getId());
        } else if (contributor.getSchemaUri().equals(ISNI_SCHEMA_URI)) {
            name = isniClient.getName(contributor.getId());
        } else {
            throw new RuntimeException("Unsupported contributor schema %s".formatted(contributor.getSchemaUri()));
        }

        creator.setName(name);

        creator.setNameType("Personal");
        creator.setNameIdentifiers(List.of(
                new NameIdentifier()
                        .setNameIdentifier(contributor.getId())
                        .setSchemeUri(contributor.getSchemaUri())
                        .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEMA_MAP.get(contributor.getSchemaUri()))
        ));

        return creator;
    }
}
