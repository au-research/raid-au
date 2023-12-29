package au.org.raid.api.service;

import au.org.raid.api.spring.config.IdentifierProperties;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.Owner;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IdService {

    private final OrganisationService organisationService;
    private final IdentifierProperties identifierProperties;

    public Id getId(final RaidRecord record) {
        final var registrationAgencySchemaUri = organisationService.findOrganisationSchemaUri(record.getRegistrationAgencyOrganisationId());
        final var ownerUri = organisationService.findOrganisationUri(record.getOwnerOrganisationId());
        final var ownerSchemaUri = organisationService.findOrganisationSchemaUri(record.getOwnerOrganisationId());

        return new Id()
                .id(String.format("%s%s",identifierProperties.getNamePrefix(), record.getHandle()))
                .schemaUri(record.getSchemaUri())
                .registrationAgency(new RegistrationAgency()
                        .id(identifierProperties.getRegistrationAgencyIdentifier())
                        .schemaUri(registrationAgencySchemaUri)
                )
                .owner(new Owner()
                        .id(ownerUri)
                        .schemaUri(ownerSchemaUri)
                        .servicePoint(record.getServicePointId())
                )
                .license(record.getLicense())
                .version(record.getVersion())
                .raidAgencyUrl(String.format("%s%s", identifierProperties.getHandleUrlPrefix(), record.getHandle()));


    }
}
