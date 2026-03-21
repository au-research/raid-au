package au.org.raid.api.factory;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.Owner;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdFactory {
    private final IdentifierProperties identifierProperties;

    public Id create(final String handle,
                     final ServicePointRecord servicePointRecord
    ) {
        return new Id().
                id(String.format("%s%s", identifierProperties.getNamePrefix(), handle))
                .schemaUri(identifierProperties.getSchemaUri())
                .registrationAgency(new RegistrationAgency()
                        .id(identifierProperties.getRegistrationAgencyIdentifier())
                        .schemaUri(SchemaValues.ROR_SCHEMA_URI.getUri())
                )
                .owner(new Owner()
                        .id(servicePointRecord.getIdentifierOwner())
                        .schemaUri(SchemaValues.ROR_SCHEMA_URI.getUri())
                        .servicePoint(servicePointRecord.getId())
                )
                .raidAgencyUrl(String.format("%s%s", identifierProperties.getLandingPrefix(), handle))
                .license(identifierProperties.getLicense())
                .version(1);
    }
}