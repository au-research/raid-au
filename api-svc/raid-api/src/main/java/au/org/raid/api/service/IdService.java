package au.org.raid.api.service;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
                .schemaUri(RaidIdentifierSchemaURIEnum.fromValue(record.getSchemaUri()))
                .registrationAgency(new RegistrationAgency()
                        .id(identifierProperties.getRegistrationAgencyIdentifier())
                        .schemaUri(RegistrationAgencySchemaURIEnum.fromValue(registrationAgencySchemaUri))
                )
                .owner(new Owner()
                        .id(ownerUri)
                        .schemaUri(RegistrationAgencySchemaURIEnum.fromValue(ownerSchemaUri))
                        .servicePoint(new BigDecimal(record.getServicePointId()))
                )
                .license(record.getLicense())
                .version(record.getVersion())
                .raidAgencyUrl(String.format("%s%s", identifierProperties.getLandingPrefix(), record.getHandle()));


    }
}
