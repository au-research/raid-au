package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.OrganizationRoleIdEnum;
import au.org.raid.idl.raidv2.model.OrganizationRoleSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OrganisationRoleFactoryTest {
    private final OrganisationRoleFactory factory = new OrganisationRoleFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182;
        final var schemaUri = OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359;
        final var startDate = "2021";
        final var endDate = "2022";

        final var result = factory.create(id.getValue(), schemaUri.getValue(), startDate, endDate);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
        assertThat(result.getStartDate(), is(startDate));
        assertThat(result.getEndDate(), is(endDate));
    }
}