package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.LanguageSchemaURIEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class LanguageFactoryTest {
    private final LanguageFactory factory = new LanguageFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "_id";
        final var schemaUri = LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML;

        final var result = factory.create(id, schemaUri.getValue());

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}