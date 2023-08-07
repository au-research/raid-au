package raido.apisvc.factory;

import org.springframework.stereotype.Component;
import raido.idl.raidv2.model.RelatedObject;
import raido.idl.raidv2.model.RelatedObjectBlock;
import raido.idl.raidv2.model.RelatedObjectCategory;
import raido.idl.raidv2.model.RelatedObjectType;

import java.util.Map;

@Component
public class RelatedObjectFactory {
    private static final String CATEGORY_SCHEME_URI =
        "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1";
    private static final String TYPE_SCHEME_URI =
        "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1";

    private static final Map<String, String> CATEGORY_MAP = Map.of(
        "input", "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json",
        "output", "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json",
        "internal", "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/internal.json"
    );

    public RelatedObject create(final RelatedObjectBlock relatedObjectBlock) {
        if (relatedObjectBlock == null) {
            return null;
        }

        return new RelatedObject()
            .id(relatedObjectBlock.getRelatedObject())
            .identifierSchemeUri(relatedObjectBlock.getRelatedObjectSchemeUri())
            .category(new RelatedObjectCategory()
                .schemeUri(CATEGORY_SCHEME_URI)
                .id(relatedObjectBlock.getRelatedObjectCategory() != null ?
                    CATEGORY_MAP.get(relatedObjectBlock.getRelatedObjectCategory()) : null)
            )
            .type(new RelatedObjectType()
                .schemeUri(TYPE_SCHEME_URI)
                .id(relatedObjectBlock.getRelatedObjectType())
            );
    }
}