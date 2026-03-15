package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.TraditionalKnowledgeLabel;
import au.org.raid.idl.raidv2.model.TraditionalKnowledgeLabelSchemaUriEnum;
import org.springframework.stereotype.Component;

@Component
public class TraditionalKnowledgeLabelFactory {
    public TraditionalKnowledgeLabel create(final String id, final String schemaUri) {
        return new TraditionalKnowledgeLabel()
                .id(id)
                .schemaUri(TraditionalKnowledgeLabelSchemaUriEnum.fromValue(schemaUri));
    }
}