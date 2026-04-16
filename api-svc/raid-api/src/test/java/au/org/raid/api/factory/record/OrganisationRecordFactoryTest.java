package au.org.raid.api.factory.record;

import au.org.raid.idl.raidv2.model.Organisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class OrganisationRecordFactoryTest {
    private final OrganisationRecordFactory organisationRecordFactory = new OrganisationRecordFactory();

    @Test
    @DisplayName("Sets all fields from organisation and schema id")
    void setsAllFieldsFromOrganisationAndSchemaId() {
        final var pid = "organisation-pid";
        final var schemaId = 123;
        final var schemaUri = "organisation-schema-uri";

        final var organisation = new Organisation()
                .id(pid)
                .schemaUri(schemaUri);

        final var result = organisationRecordFactory.create(organisation, schemaId);

        assertThat(result.getPid(), is(pid));
        assertThat(result.getSchemaId(), is(schemaId));
    }

    @Test
    @DisplayName("Sets all fields from organisation id and schema id")
    void setsAllFieldsFromOrganisationIdAndSchemaId() {
        final var pid = "organisation-pid";
        final var schemaId = 123;

        final var result = organisationRecordFactory.create(pid, schemaId);

        assertThat(result.getPid(), is(pid));
        assertThat(result.getSchemaId(), is(schemaId));
    }
}