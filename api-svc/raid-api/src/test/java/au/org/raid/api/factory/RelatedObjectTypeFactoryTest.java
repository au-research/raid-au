package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RelatedObjectTypeFactoryTest {
    private final RelatedObjectTypeFactory factory = new RelatedObjectTypeFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247;
        final var schemaUri = RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329;

        final var result = factory.create(id.getValue(), schemaUri.getValue());

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}