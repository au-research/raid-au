package au.org.raid.api.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OrganisationRoleFactoryTest {
    private final OrganisationRoleFactory factory = new OrganisationRoleFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "_id";
        final var schemaUri = "schema-uri";
        final var startDate = "2021";
        final var endDate = "2022";

        final var result = factory.create(id, schemaUri, startDate, endDate);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
        assertThat(result.getStartDate(), is(startDate));
        assertThat(result.getEndDate(), is(endDate));
    }
}