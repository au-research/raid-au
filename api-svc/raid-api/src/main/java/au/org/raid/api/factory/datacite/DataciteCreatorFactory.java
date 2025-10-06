package au.org.raid.api.factory.datacite;

import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.model.datacite.DataciteCreator;
import au.org.raid.api.model.datacite.NameIdentifier;
import au.org.raid.api.service.OrcidIntegrationClient;
import au.org.raid.idl.raidv2.model.Contributor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataciteCreatorFactory {
    private final OrcidIntegrationClient orcidIntegrationClient;

    private static final Map<String, String> NAME_IDENTIFIER_SCHEMA_MAP = Map.of(
            "https://orcid.org/", "ORCID",
            "https://isni.org/", "ISNI"
    );

    public DataciteCreator create(final Contributor contributor) {
        final var creator = new DataciteCreator();

        if (contributor.getStatus() != null && contributor.getStatus().equals("AUTHENTICATED")) {
            final var response = orcidIntegrationClient.findByOrcid(contributor.getId())
                    .orElseThrow(() -> new IllegalStateException("No contributor found with id %s".formatted(contributor.getId())));

            if (response.getOrcid() != null && response.getName() != null) {
                creator.setName(response.getName());
            }

            creator.setNameType("Personal");
            creator.setNameIdentifiers(List.of(
                    new NameIdentifier()
                            .setNameIdentifier(contributor.getId())
                            .setSchemeUri(contributor.getSchemaUri().getValue())
                            .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEMA_MAP.get(contributor.getSchemaUri().getValue()))
            ));
        }

        return creator;
    }
}
