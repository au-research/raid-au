package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeSchemaUriEnum;
import org.springframework.stereotype.Component;

@Component
public class RelatedObjectTypeFactory {
    public RelatedObjectType create(final String id, final String schemaUri) {
        return new RelatedObjectType()
                .id(RelatedObjectTypeIdEnum.fromValue(id))
                .schemaUri(RelatedObjectTypeSchemaUriEnum.fromValue(schemaUri));
    }
}
