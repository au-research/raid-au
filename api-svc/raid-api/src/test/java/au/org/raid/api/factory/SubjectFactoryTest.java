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
        final var schemaUri = SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316.getValue();
        final var keywords = List.of(new SubjectKeyword());

        final var result = factory.create(id, schemaUri, keywords);

        assertThat(result.getId(), is(SchemaValues.SUBJECT_ID_PREFIX.getUri() + id));
        assertThat(result.getSchemaUri().getValue(), is(schemaUri));
        assertThat(result.getKeyword(), is(keywords));
    }
}