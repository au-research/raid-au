package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteAlternateIdentifier;
import au.org.raid.idl.raidv2.model.AlternateIdentifier;
import au.org.raid.idl.raidv2.model.Id;
import org.springframework.stereotype.Component;

@Component
public class DataciteAlternateIdentifierFactory {
    public DataciteAlternateIdentifier create(final AlternateIdentifier alternateIdentifier) {
        return new DataciteAlternateIdentifier()
                .setAlternateIdentifier(alternateIdentifier.getId())
                .setAlternateIdentifierType("URL");
    }

    public DataciteAlternateIdentifier create(final Id id) {
        return new DataciteAlternateIdentifier()
                .setAlternateIdentifier(id.getRaidAgencyUrl())
                .setAlternateIdentifierType("RaidAgencyUrl");
    }

}
