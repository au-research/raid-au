package au.org.raid.api.service;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.Owner;
import au.org.raid.idl.raidv2.model.RaidIdentifierSchemaURIEnum;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import au.org.raid.idl.raidv2.model.RegistrationAgencySchemaURIEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdServiceTest {
    @Mock
    private OrganisationService organisationService;
    @Mock
    private IdentifierProperties identifierProperties;
    @InjectMocks
    private IdService idService;

    @Test
    @DisplayName("getId returns Id")
    void getIdReturnsId() {
        final var registrationAgencyOrganisationId = 123;
        final var ownerOrganisationId = 234;
        final var schemaUri = RaidIdentifierSchemaURIEnum.HTTPS_RAID_ORG_.getValue();
        final var handle = "_handle";
        final var servicePointId = 345L;
        final var license = "_license";
        final var version = 7;

        final var namePrefix = "name-prefix";
        final var registrationAgencyIdentifier = "registration-agency-identifier";
        final var landingPrefix = "landing-prefix";

        final var registrationAgencySchemaUri = RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_.getValue();
        final var ownerUri = "owner-uri";
        final var ownerSchemaUri = RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_.getValue();;

        when(organisationService.findOrganisationSchemaUri(registrationAgencyOrganisationId))
                .thenReturn(registrationAgencySchemaUri);
        when(organisationService.findOrganisationUri(ownerOrganisationId)).thenReturn(ownerUri);
        when(organisationService.findOrganisationSchemaUri(ownerOrganisationId)).thenReturn(ownerSchemaUri);

        when(identifierProperties.getNamePrefix()).thenReturn(namePrefix);
        when(identifierProperties.getRegistrationAgencyIdentifier()).thenReturn(registrationAgencyIdentifier);
        when(identifierProperties.getLandingPrefix()).thenReturn(landingPrefix);

        final var raidRecord = new RaidRecord()
                .setRegistrationAgencyOrganisationId(registrationAgencyOrganisationId)
                .setOwnerOrganisationId(ownerOrganisationId)
                .setSchemaUri(schemaUri)
                .setHandle(handle)
                .setServicePointId(servicePointId)
                .setLicense(license)
                .setVersion(version);

        final var expectedRegistrationAgency = new RegistrationAgency()
                .id(registrationAgencyIdentifier)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var expectedOwner = new Owner()
                .id(ownerUri)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_)
                .servicePoint(new BigDecimal(servicePointId));

        final var result = idService.getId(raidRecord);

        assertThat(result.getId(), is(namePrefix + handle));
        assertThat(result.getSchemaUri().getValue(), is(schemaUri));
        assertThat(result.getRegistrationAgency(), is(expectedRegistrationAgency));
        assertThat(result.getOwner(), is(expectedOwner));
        assertThat(result.getLicense(), is(license));
        assertThat(result.getVersion(), is(version));
        assertThat(result.getRaidAgencyUrl(), is(landingPrefix + handle));
    }
}