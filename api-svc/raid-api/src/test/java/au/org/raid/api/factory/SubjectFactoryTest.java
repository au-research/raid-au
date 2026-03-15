package au.org.raid.api.factory;

import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.SubjectKeyword;
import au.org.raid.idl.raidv2.model.SubjectSchemaURIEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SubjectFactoryTest {
    private final SubjectFactory factory = new SubjectFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "_id";
        // factory.create() expects a DB URI (not the enum label)
        final var dbSchemaUri = SchemaValues.SUBJECT_SCHEMA_URI.getUri();
        final var keywords = List.of(new SubjectKeyword());

        final var result = factory.create(id, dbSchemaUri, keywords);

        assertThat(result.getId(), is(SchemaValues.SUBJECT_ID_PREFIX.getUri() + id));
        assertThat(result.getSchemaUri(), is(SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316));
        assertThat(result.getKeyword(), is(keywords));
    }
}