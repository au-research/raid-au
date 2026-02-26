package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteRelatedIdentifier;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.vocabularies.datacite.RelatedIdentifierType;
import au.org.raid.api.vocabularies.datacite.RelationType;
import au.org.raid.api.vocabularies.datacite.ResourceTypeGeneral;
import au.org.raid.api.vocabularies.raid.RelatedObjectCategory;
import au.org.raid.api.vocabularies.raid.RelatedObjectType;
import au.org.raid.api.vocabularies.raid.RelatedRaidType;
import au.org.raid.idl.raidv2.model.AlternateUrl;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedRaid;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Map.entry;

@Component
public class DataciteRelatedIdentifierFactory {
    private static final Map<String, String> IDENTIFIER_TYPE_MAP = Map.of(
            SchemaValues.ARKS_SCHEMA.getUri(), RelatedIdentifierType.ARK.getName(),
            SchemaValues.DOI_SCHEMA.getUri(), RelatedIdentifierType.DOI.getName(),
            SchemaValues.ISBN_SCHEMA.getUri(), RelatedIdentifierType.ISBN.getName(),
            SchemaValues.RRID_SCHEMA.getUri(), RelatedIdentifierType.URL.getName(),
            SchemaValues.ARCHIVE_ORG_SCHEMA.getUri(), RelatedIdentifierType.URL.getName()
    );

    private static final Map<String, String> RESOURCE_TYPE_MAP = Map.ofEntries(
            entry(RelatedObjectType.OUTPUT_MANAGEMENT_PLAN.getUri(), ResourceTypeGeneral.OUTPUT_MANAGEMENT_PLAN.getName()),
            entry(RelatedObjectType.CONFERENCE_POSTER.getUri(), ResourceTypeGeneral.TEXT.getName()),
            entry(RelatedObjectType.WORKFLOW.getUri(), ResourceTypeGeneral.WORKFLOW.getName()),
            entry(RelatedObjectType.JOURNAL_ARTICLE.getUri(), ResourceTypeGeneral.JOURNAL_ARTICLE.getName()),
            entry(RelatedObjectType.STANDARD.getUri(), ResourceTypeGeneral.STANDARD.getName()),
            entry(RelatedObjectType.REPORT.getUri(), ResourceTypeGeneral.REPORT.getName()),
            entry(RelatedObjectType.DISSERTATION.getUri(), ResourceTypeGeneral.DISSERTATION.getName()),
            entry(RelatedObjectType.PREPRINT.getUri(), ResourceTypeGeneral.PREPRINT.getName()),
            entry(RelatedObjectType.DATA_PAPER.getUri(), ResourceTypeGeneral.DATA_PAPER.getName()),
            entry(RelatedObjectType.COMPUTATIONAL_NOTEBOOK.getUri(), ResourceTypeGeneral.COMPUTATIONAL_NOTEBOOK.getName()),
            entry(RelatedObjectType.IMAGE.getUri(), ResourceTypeGeneral.IMAGE.getName()),
            entry(RelatedObjectType.BOOK.getUri(), ResourceTypeGeneral.BOOK.getName()),
            entry(RelatedObjectType.SOFTWARE.getUri(), ResourceTypeGeneral.SOFTWARE.getName()),
            entry(RelatedObjectType.EVENT.getUri(), ResourceTypeGeneral.EVENT.getName()),
            entry(RelatedObjectType.SOUND.getUri(), ResourceTypeGeneral.SOUND.getName()),
            entry(RelatedObjectType.CONFERENCE_PROCEEDING.getUri(), ResourceTypeGeneral.CONFERENCE_PROCEEDING.getName()),
            entry(RelatedObjectType.MODEL.getUri(), ResourceTypeGeneral.MODEL.getName()),
            entry(RelatedObjectType.CONFERENCE_PAPER.getUri(), ResourceTypeGeneral.CONFERENCE_PAPER.getName()),
            entry(RelatedObjectType.TEXT.getUri(), ResourceTypeGeneral.TEXT.getName()),
            entry(RelatedObjectType.INSTRUMENT.getUri(), ResourceTypeGeneral.INSTRUMENT.getName()),
            entry(RelatedObjectType.LEARNING_OBJECT.getUri(), ResourceTypeGeneral.OTHER.getName()),
            entry(RelatedObjectType.PRIZE.getUri(), ResourceTypeGeneral.OTHER.getName()),
            entry(RelatedObjectType.DATASET.getUri(), ResourceTypeGeneral.DATASET.getName()),
            entry(RelatedObjectType.PHYSICAL_OBJECT.getUri(), ResourceTypeGeneral.PHYSICAL_OBJECT.getName()),
            entry(RelatedObjectType.BOOK_CHAPTER.getUri(), ResourceTypeGeneral.BOOK_CHAPTER.getName()),
            entry(RelatedObjectType.FUNDING.getUri(), ResourceTypeGeneral.OTHER.getName()),
            entry(RelatedObjectType.AUDIO_VISUAL.getUri(), ResourceTypeGeneral.AUDIOVISUAL.getName()),
            entry(RelatedObjectType.SERVICE.getUri(), ResourceTypeGeneral.SERVICE.getName())
    );

    private static final Map<String, String> RELATION_TYPE_MAP = Map.ofEntries(
            entry(RelatedObjectCategory.INPUT.getUri(), RelationType.REFERENCES.getName()),
            entry(RelatedObjectCategory.OUTPUT.getUri(), RelationType.IS_REFERENCED_BY.getName()),
            entry(RelatedObjectCategory.INTERNAL_PROCESS_DOCUMENT.getUri(), RelationType.IS_SUPPLEMENTED_BY.getName()),
            entry(RelatedRaidType.CONTINUES.getUri(), RelationType.CONTINUES.getName()),
            entry(RelatedRaidType.IS_CONTINUED_BY.getUri(), RelationType.IS_CONTINUED_BY.getName()),
            entry(RelatedRaidType.IS_PART_OF.getUri(), RelationType.IS_PART_OF.getName()),
            entry(RelatedRaidType.HAS_PART.getUri(), RelationType.HAS_PART.getName()),
            entry(RelatedRaidType.IS_DERIVED_FROM.getUri(), RelationType.IS_DERIVED_FROM.getName()),
            entry(RelatedRaidType.IS_SOURCE_OF.getUri(), RelationType.IS_SOURCE_OF.getName()),
            entry(RelatedRaidType.OBSOLETES.getUri(), RelationType.OBSOLETES.getName()),
            entry(RelatedRaidType.IS_OBSOLETED_BY.getUri(), RelationType.IS_OBSOLETED_BY.getName())
    );

    public DataciteRelatedIdentifier create(final RelatedObject relatedObject) {
        return new DataciteRelatedIdentifier()
                .setRelatedIdentifier(relatedObject.getId())
                .setRelatedIdentifierType(IDENTIFIER_TYPE_MAP.get(relatedObject.getSchemaUri()))
                .setResourceTypeGeneral(RESOURCE_TYPE_MAP.get(relatedObject.getType().getId()))
                .setRelationType(RELATION_TYPE_MAP.get(relatedObject.getCategory().get(0).getId()));
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
                .setRelationType(RELATION_TYPE_MAP.get(relatedRaid.getType().getId()))
                .setResourceTypeGeneral(ResourceTypeGeneral.PROJECT.getName());
    }

}
