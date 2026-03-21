package au.org.raid.api.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AccessTypeFactoryTest {
    private final AccessTypeFactory factory = new AccessTypeFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "_id";
        final var schemaUri = "schema-uri";

        final var result = factory.create(id, schemaUri);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}