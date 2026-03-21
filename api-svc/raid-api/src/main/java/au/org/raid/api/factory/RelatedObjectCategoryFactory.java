package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import org.springframework.stereotype.Component;

@Component
public class RelatedObjectCategoryFactory {
    public RelatedObjectCategory create(final String id, final String schemaUri) {
        return new RelatedObjectCategory()
                .id(id)
                .schemaUri(schemaUri);
    }
}
