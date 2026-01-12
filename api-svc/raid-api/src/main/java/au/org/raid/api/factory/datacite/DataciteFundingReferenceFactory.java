package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.model.datacite.doi.DataciteFundingReference;
import au.org.raid.idl.raidv2.model.Organisation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataciteFundingReferenceFactory {
    private final RorClient rorClient;

    public DataciteFundingReference create(final Organisation organisation) {
        final var organisationName = rorClient.getOrganisationName(organisation.getId());

        return new DataciteFundingReference()
                .setFunderName(organisationName)
                .setFunderIdentifier(organisation.getId())
                .setFunderIdentifierType("ROR")
                .setSchemeUri(organisation.getSchemaUri());
    }
}
