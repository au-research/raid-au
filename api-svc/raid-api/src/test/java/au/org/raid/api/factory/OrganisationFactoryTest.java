package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.OrganisationRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class OrganisationFactoryTest {
    private final OrganisationFactory factory = new OrganisationFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {

        final var id = "_id";
        final var schemaUri = "schema-uri";
        final var roles = List.of(new OrganisationRole());

        final var result = factory.create(id, schemaUri, roles);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
        assertThat(result.getRole(), is(roles));
    }
}