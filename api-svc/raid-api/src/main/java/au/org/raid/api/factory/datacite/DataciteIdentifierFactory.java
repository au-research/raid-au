package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteIdentifier;
import org.springframework.stereotype.Component;

@Component
public class DataciteIdentifierFactory {
    public DataciteIdentifier create(final String identifier, final String identifierType) {
        return new DataciteIdentifier()
                .setIdentifierType(identifierType)
                .setIdentifier(identifier);
    }
}
