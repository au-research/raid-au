package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectType;
import org.springframework.stereotype.Component;

@Component
public class RelatedObjectTypeFactory {
    public RelatedObjectType create(final String id, final String schemaUri) {
        return new RelatedObjectType()
                .id(id)
                .schemaUri(schemaUri);
    }
}
