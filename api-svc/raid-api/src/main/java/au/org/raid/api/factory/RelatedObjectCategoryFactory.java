package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectCategorySchemaUriEnum;
import org.springframework.stereotype.Component;

@Component
public class RelatedObjectCategoryFactory {
    public RelatedObjectCategory create(final String id, final String schemaUri) {
        return new RelatedObjectCategory()
                .id(RelatedObjectCategoryIdEnum.fromValue(id))
                .schemaUri(RelatedObjectCategorySchemaUriEnum.fromValue(schemaUri));
    }
}
