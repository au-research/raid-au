package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.DataciteContributor;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataciteContributorFactoryTest {

    private DataciteContributorFactory dataciteContributorFactory = new DataciteContributorFactory();

    @Test
    @DisplayName("Create with registration agency")
    void createWithRegistrationAgency() {
        final var id = "_id";
        final var schemaUri = RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_;

        final var registrationAgency = new RegistrationAgency()
                .id(id)
                .schemaUri(schemaUri);

        final var result = dataciteContributorFactory.create(registrationAgency);

        assertThat(result.getContributorType(), is("RegistrationAgency"));
        assertThat(result.getName(), is("RAiD AU"));
        assertThat(result.getNameType(), is("Organizational"));
        assertThat(result.getNameIdentifiers().get(0).getNameIdentifier(), is(id));
        assertThat(result.getNameIdentifiers().get(0).getNameIdentifierScheme(), is("ROR"));
        assertThat(result.getNameIdentifiers().get(0).getSchemeUri(), is(schemaUri.getValue()));
    }

    @Test
    @DisplayName("Create organisation contributor with 'Lead Research Organisation' role")
    public void leadResearchOrganisation() {
        final var id = "_id";

        Organisation organisation = new Organisation()
                .id(id)
                .role(List.of(
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186),
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)
                ));

        DataciteContributor dataciteContributor = dataciteContributorFactory.create(organisation);

        assertEquals(id, dataciteContributor.getName());
        assertEquals("HostingInstitution", dataciteContributor.getContributorType());
    }

    @Test
    @DisplayName("Create organisation contributor with 'Other Research Organisation' role")
    public void otherResearchOrganisation() {
        final var id = "_id";

        Organisation organisation = new Organisation()
                .id(id)
                .role(List.of(
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186),
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_183)
                ));

        DataciteContributor dataciteContributor = dataciteContributorFactory.create(organisation);

        assertEquals(id, dataciteContributor.getName());
        assertEquals("Other", dataciteContributor.getContributorType());
    }
    @Test
    @DisplayName("Create organisation contributor with 'Partner' role")
    public void partnerOrganisation() {
        final var id = "_id";

        Organisation organisation = new Organisation()
                .id(id)
                .role(List.of(
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186),
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_184)

                ));

        DataciteContributor dataciteContributor = dataciteContributorFactory.create(organisation);

        assertEquals(id, dataciteContributor.getName());
        assertEquals("Other", dataciteContributor.getContributorType());
    }
    @Test
    @DisplayName("Create organisation contributor with 'Contractor' role")
    public void contractorOrganisation() {
        final var id = "_id";

        Organisation organisation = new Organisation()
                .id(id)
                .role(List.of(
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186),
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_185)
                ));

        DataciteContributor dataciteContributor = dataciteContributorFactory.create(organisation);

        assertEquals(id, dataciteContributor.getName());
        assertEquals("Other", dataciteContributor.getContributorType());
    }

    @Test
    @DisplayName("Create organisation contributor with 'Facility' role")
    public void facilityOrganisation() {
        final var id = "_id";

        Organisation organisation = new Organisation()
                .id(id)
                .role(List.of(
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186),
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_187)
                ));

        DataciteContributor dataciteContributor = dataciteContributorFactory.create(organisation);

        assertEquals(id, dataciteContributor.getName());
        assertEquals("Sponsor", dataciteContributor.getContributorType());
    }

    @Test
    @DisplayName("Create organisation contributor with 'Other Organisation' role")
    public void otherOrganisation() {
        final var id = "_id";

        Organisation organisation = new Organisation()
                .id(id)
                .role(List.of(
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186),
                        new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_188)
                ));

        DataciteContributor dataciteContributor = dataciteContributorFactory.create(organisation);

        assertEquals(id, dataciteContributor.getName());
        assertEquals("Other", dataciteContributor.getContributorType());
    }
}
