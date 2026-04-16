package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.RaidDescriptionRecord;
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
        final var typeId = "type-id";
        final var typeSchemaUri = "type-schema-uri";
        final var language = new Language();

        final var record = new RaidDescriptionRecord()
                .setText(text);

        final var result = factory.create(record, typeId, typeSchemaUri, language);

        assertThat(result.getText(), is(text));
        assertThat(result.getType().getId(), is(typeId));
        assertThat(result.getType().getSchemaUri(), is(typeSchemaUri));
        assertThat(result.getLanguage(), is(language));
    }
}