package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.TraditionalKnowledgeLabelSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TraditionalKnowledgeLabelFactoryTest {
    private final TraditionalKnowledgeLabelFactory factory = new TraditionalKnowledgeLabelFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "_id";
        final var schemaUri = TraditionalKnowledgeLabelSchemaUriEnum.HTTPS_LOCALCONTEXTS_ORG_LABELS_TRADITIONAL_KNOWLEDGE_LABELS_;

        final var result = factory.create(id, schemaUri.getValue());

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}