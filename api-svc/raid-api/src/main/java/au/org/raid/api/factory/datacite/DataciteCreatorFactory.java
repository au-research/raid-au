package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.contributor.isni.IsniClient;
import au.org.raid.api.client.contributor.orcid.OrcidClient;
import au.org.raid.api.model.datacite.doi.DataciteCreator;
import au.org.raid.api.model.datacite.doi.NameIdentifier;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorSchemaUriEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataciteCreatorFactory {
    private final OrcidClient orcidClient;
    private final IsniClient isniClient;

    private static final Map<ContributorSchemaUriEnum, String> NAME_IDENTIFIER_SCHEMA_MAP = Map.of(
            ContributorSchemaUriEnum.HTTPS_ORCID_ORG_, "ORCID",
            ContributorSchemaUriEnum.HTTPS_ISNI_ORG_, "ISNI"
    );

    public DataciteCreator create(final Contributor contributor) {
        final var creator = new DataciteCreator();
        String name;

        final var schemaUri = contributor.getSchemaUri();
        if (schemaUri == ContributorSchemaUriEnum.HTTPS_ORCID_ORG_) {
            name = orcidClient.getName(contributor.getId());
        } else if (schemaUri == ContributorSchemaUriEnum.HTTPS_ISNI_ORG_) {
            name = isniClient.getName(contributor.getId());
        } else {
            throw new RuntimeException("Unsupported contributor schema %s".formatted(schemaUri.getValue()));
        }

        creator.setName(name);

        creator.setNameType("Personal");
        creator.setNameIdentifiers(List.of(
                new NameIdentifier()
                        .setNameIdentifier(contributor.getId())
                        .setSchemeUri(schemaUri.getValue())
                        .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEMA_MAP.get(schemaUri))
        ));

        return creator;
    }
}
