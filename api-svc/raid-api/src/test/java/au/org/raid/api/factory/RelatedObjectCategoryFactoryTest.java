package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectCategorySchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RelatedObjectCategoryFactoryTest {
    private final RelatedObjectCategoryFactory factory = new RelatedObjectCategoryFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191;
        final var schemaUri = RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_URI_386;

        final var result = factory.create(id.getValue(), schemaUri.getValue());

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}