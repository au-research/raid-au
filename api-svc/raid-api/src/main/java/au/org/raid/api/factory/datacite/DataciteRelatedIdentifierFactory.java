package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteRelatedIdentifier;
import au.org.raid.api.vocabularies.datacite.RelatedIdentifierType;
import au.org.raid.api.vocabularies.datacite.RelationType;
import au.org.raid.api.vocabularies.datacite.ResourceTypeGeneral;
import au.org.raid.idl.raidv2.model.AlternateUrl;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategoryIdEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RelatedObjectTypeIdEnum;
import au.org.raid.idl.raidv2.model.RelatedRaid;
import au.org.raid.idl.raidv2.model.RelatedRaidTypeIdEnum;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;

@Component
public class DataciteRelatedIdentifierFactory {
    private static final Map<RelatedObjectSchemaUriEnum, String> IDENTIFIER_TYPE_MAP = Map.of(
            RelatedObjectSchemaUriEnum.HTTPS_ARKS_ORG_, RelatedIdentifierType.ARK.getName(),
            RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_, RelatedIdentifierType.DOI.getName(),
            RelatedObjectSchemaUriEnum.HTTPS_WWW_ISBN_INTERNATIONAL_ORG_, RelatedIdentifierType.ISBN.getName(),
            RelatedObjectSchemaUriEnum.HTTPS_SCICRUNCH_ORG_RESOLVER_, RelatedIdentifierType.URL.getName(),
            RelatedObjectSchemaUriEnum.HTTPS_ARCHIVE_ORG_, RelatedIdentifierType.URL.getName()
    );

    private static final Map<RelatedObjectTypeIdEnum, String> RESOURCE_TYPE_MAP = Map.ofEntries(
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247, ResourceTypeGeneral.OUTPUT_MANAGEMENT_PLAN.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_248, ResourceTypeGeneral.TEXT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_249, ResourceTypeGeneral.WORKFLOW.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_250, ResourceTypeGeneral.JOURNAL_ARTICLE.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_251, ResourceTypeGeneral.STANDARD.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_252, ResourceTypeGeneral.REPORT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_253, ResourceTypeGeneral.DISSERTATION.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_254, ResourceTypeGeneral.PREPRINT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_255, ResourceTypeGeneral.DATA_PAPER.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_256, ResourceTypeGeneral.COMPUTATIONAL_NOTEBOOK.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_257, ResourceTypeGeneral.IMAGE.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_258, ResourceTypeGeneral.BOOK.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_259, ResourceTypeGeneral.SOFTWARE.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_260, ResourceTypeGeneral.EVENT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_261, ResourceTypeGeneral.SOUND.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_262, ResourceTypeGeneral.CONFERENCE_PROCEEDING.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_263, ResourceTypeGeneral.MODEL.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_264, ResourceTypeGeneral.CONFERENCE_PAPER.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_265, ResourceTypeGeneral.TEXT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_266, ResourceTypeGeneral.INSTRUMENT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_267, ResourceTypeGeneral.OTHER.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_268, ResourceTypeGeneral.OTHER.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_269, ResourceTypeGeneral.DATASET.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_270, ResourceTypeGeneral.PHYSICAL_OBJECT.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_271, ResourceTypeGeneral.BOOK_CHAPTER.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_272, ResourceTypeGeneral.OTHER.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_273, ResourceTypeGeneral.AUDIOVISUAL.getName()),
            entry(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_274, ResourceTypeGeneral.SERVICE.getName())
    );

    private static final Map<RelatedObjectCategoryIdEnum, String> OBJECT_RELATION_TYPE_MAP = Map.of(
            RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191, RelationType.REFERENCES.getName(),
            RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190, RelationType.IS_REFERENCED_BY.getName(),
            RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_192, RelationType.IS_SUPPLEMENTED_BY.getName()
    );

    private static final Map<RelatedRaidTypeIdEnum, String> RAID_RELATION_TYPE_MAP = Map.of(
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_204, RelationType.CONTINUES.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_203, RelationType.IS_CONTINUED_BY.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_202, RelationType.IS_PART_OF.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_201, RelationType.HAS_PART.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_200, RelationType.IS_DERIVED_FROM.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_199, RelationType.IS_SOURCE_OF.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198, RelationType.OBSOLETES.getName(),
            RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_205, RelationType.IS_OBSOLETED_BY.getName()
    );

    public DataciteRelatedIdentifier create(final RelatedObject relatedObject) {
        return new DataciteRelatedIdentifier()
                .setRelatedIdentifier(relatedObject.getId())
                .setRelatedIdentifierType(IDENTIFIER_TYPE_MAP.get(relatedObject.getSchemaUri()))
                .setResourceTypeGeneral(RESOURCE_TYPE_MAP.get(relatedObject.getType().getId()))
                .setRelationType(OBJECT_RELATION_TYPE_MAP.get(relatedObject.getCategory().get(0).getId()));
    }

    public DataciteRelatedIdentifier create(final AlternateUrl alternateUrl) {
        return new DataciteRelatedIdentifier()
                .setRelatedIdentifier(alternateUrl.getUrl())
                .setRelatedIdentifierType(RelatedIdentifierType.URL.getName())
                .setRelationType(RelationType.IS_DOCUMENTED_BY.getName())
                .setResourceTypeGeneral(ResourceTypeGeneral.OTHER.getName());
    }

    public DataciteRelatedIdentifier create(final RelatedRaid relatedRaid) {
        return new DataciteRelatedIdentifier()
                .setRelatedIdentifier(relatedRaid.getId())
                .setRelatedIdentifierType(RelatedIdentifierType.DOI.getName())
                .setRelationType(RAID_RELATION_TYPE_MAP.get(relatedRaid.getType().getId()))
                .setResourceTypeGeneral(ResourceTypeGeneral.PROJECT.getName());
    }

}
