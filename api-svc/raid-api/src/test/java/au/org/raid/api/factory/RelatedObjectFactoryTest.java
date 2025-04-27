package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RelatedObjectFactoryTest {
    private final RelatedObjectFactory factory = new RelatedObjectFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "_id";
        final var schemaUri = RelatedObjectSchemaUriEnum.HTTPS_WWW_ISBN_INTERNATIONAL_ORG_;
        final var type = new RelatedObjectType();
        final var categories = List.of(new RelatedObjectCategory());

        final var result = factory.create(id, schemaUri.getValue(), type, categories);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
        assertThat(result.getType(), is(type));
        assertThat(result.getCategory(), is(categories));
    }
}