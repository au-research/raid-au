package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.model.datacite.doi.DatacitePublisher;
import au.org.raid.idl.raidv2.model.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatacitePublisherFactory {
    private static final String PUBLISHER_IDENTIFIER_SCHEME = "ROR";
    private final RorClient rorClient;

    public DatacitePublisher create(final Owner owner) {
        final var name = rorClient.getOrganisationName(owner.getId());

        return new DatacitePublisher()
                .setName(name)
                .setPublisherIdentifier(owner.getId())
                .setPublisherIdentifierScheme(PUBLISHER_IDENTIFIER_SCHEME)
                .setSchemeUri(owner.getSchemaUri());
    }
}
