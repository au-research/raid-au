package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RelatedObjectFactory {
    public RelatedObject create(final String id, final String schemaUri, final RelatedObjectType type, final List<RelatedObjectCategory> categories) {
        return new RelatedObject()
                .id(id)
                .schemaUri(schemaUri)
                .type(type)
                .category(categories);
    }
}