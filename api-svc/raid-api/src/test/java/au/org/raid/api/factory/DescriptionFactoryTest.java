package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.RaidDescriptionRecord;
import au.org.raid.idl.raidv2.model.DescriptionTypeIdEnum;
import au.org.raid.idl.raidv2.model.DescriptionTypeSchemaURIEnum;
import au.org.raid.idl.raidv2.model.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class DescriptionFactoryTest {
    private final DescriptionFactory factory = new DescriptionFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var text = "_text";
        final var typeId = DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3;
        final var typeSchemaUri = DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320;
        final var language = new Language();

        final var record = new RaidDescriptionRecord()
                .setText(text);


        final var result = factory.create(record, typeId.getValue(), typeSchemaUri.getValue(), language);

        assertThat(result.getText(), is(text));
        assertThat(result.getType().getId(), is(typeId));
        assertThat(result.getType().getSchemaUri(), is(typeSchemaUri));
        assertThat(result.getLanguage(), is(language));
    }
}