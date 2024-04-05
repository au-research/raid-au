/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq;


import au.org.raid.db.jooq.tables.*;
import org.jooq.Catalog;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ApiSvc extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc</code>
     */
    public static final ApiSvc API_SVC = new ApiSvc();

    /**
     * No further instances allowed
     */
    private ApiSvc() {
        super("api_svc", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Sequence<?>> getSequences() {
        return Arrays.asList(
            Sequences.ACCESS_TYPE_NEW_ID_SEQ,
            Sequences.ACCESS_TYPE_SCHEME_ID_SEQ,
            Sequences.CONTRIBUTOR_POSITION_NEW_ID_SEQ,
            Sequences.CONTRIBUTOR_POSITION_SCHEME_ID_SEQ,
            Sequences.CONTRIBUTOR_ROLE_NEW_ID_SEQ,
            Sequences.CONTRIBUTOR_ROLE_SCHEME_ID_SEQ,
            Sequences.DESCRIPTION_TYPE_NEW_ID_SEQ,
            Sequences.DESCRIPTION_TYPE_SCHEME_ID_SEQ,
            Sequences.LANGUAGE_NEW_ID_SEQ,
            Sequences.LANGUAGE_SCHEME_ID_SEQ,
            Sequences.ORGANISATION_ROLE_NEW_ID_SEQ,
            Sequences.ORGANISATION_ROLE_SCHEME_ID_SEQ,
            Sequences.RELATED_OBJECT_CATEGORY_NEW_ID_SEQ,
            Sequences.RELATED_OBJECT_CATEGORY_SCHEME_ID_SEQ,
            Sequences.RELATED_OBJECT_TYPE_NEW_ID_SEQ,
            Sequences.RELATED_OBJECT_TYPE_SCHEME_ID_SEQ,
            Sequences.RELATED_RAID_TYPE_NEW_ID_SEQ,
            Sequences.RELATED_RAID_TYPE_SCHEME_ID_SEQ,
            Sequences.SUBJECT_TYPE_SCHEME_ID_SEQ,
            Sequences.TITLE_TYPE_NEW_ID_SEQ,
            Sequences.TITLE_TYPE_SCHEME_ID_SEQ,
            Sequences.TRADITIONAL_KNOWLEDGE_LABEL_NEW_ID_SEQ
        );
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            AccessType.ACCESS_TYPE,
            AccessTypeSchema.ACCESS_TYPE_SCHEMA,
            AppUser.APP_USER,
            Contributor.CONTRIBUTOR,
            ContributorPosition.CONTRIBUTOR_POSITION,
            ContributorPositionSchema.CONTRIBUTOR_POSITION_SCHEMA,
            ContributorRole.CONTRIBUTOR_ROLE,
            ContributorRoleSchema.CONTRIBUTOR_ROLE_SCHEMA,
            ContributorSchema.CONTRIBUTOR_SCHEMA,
            DescriptionType.DESCRIPTION_TYPE,
            DescriptionTypeSchema.DESCRIPTION_TYPE_SCHEMA,
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            Language.LANGUAGE,
            LanguageSchema.LANGUAGE_SCHEMA,
            Organisation.ORGANISATION,
            OrganisationRole.ORGANISATION_ROLE,
            OrganisationRoleSchema.ORGANISATION_ROLE_SCHEMA,
            OrganisationSchema.ORGANISATION_SCHEMA,
            Raid.RAID,
            RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER,
            RaidAlternateUrl.RAID_ALTERNATE_URL,
            RaidContributor.RAID_CONTRIBUTOR,
            RaidContributorPosition.RAID_CONTRIBUTOR_POSITION,
            RaidContributorRole.RAID_CONTRIBUTOR_ROLE,
            RaidDescription.RAID_DESCRIPTION,
            RaidHistory.RAID_HISTORY,
            RaidOrganisation.RAID_ORGANISATION,
            RaidOrganisationRole.RAID_ORGANISATION_ROLE,
            RaidRelatedObject.RAID_RELATED_OBJECT,
            RaidRelatedObjectCategory.RAID_RELATED_OBJECT_CATEGORY,
            RaidSpatialCoverage.RAID_SPATIAL_COVERAGE,
            RaidSpatialCoveragePlace.RAID_SPATIAL_COVERAGE_PLACE,
            RaidSubject.RAID_SUBJECT,
            RaidSubjectKeyword.RAID_SUBJECT_KEYWORD,
            RaidTitle.RAID_TITLE,
            RaidTraditionalKnowledgeLabel.RAID_TRADITIONAL_KNOWLEDGE_LABEL,
            RaidoOperator.RAIDO_OPERATOR,
            RelatedObject.RELATED_OBJECT,
            RelatedObjectCategory.RELATED_OBJECT_CATEGORY,
            RelatedObjectCategorySchema.RELATED_OBJECT_CATEGORY_SCHEMA,
            RelatedObjectSchema.RELATED_OBJECT_SCHEMA,
            RelatedObjectType.RELATED_OBJECT_TYPE,
            RelatedObjectTypeSchema.RELATED_OBJECT_TYPE_SCHEMA,
            RelatedRaid.RELATED_RAID,
            RelatedRaidType.RELATED_RAID_TYPE,
            RelatedRaidTypeSchema.RELATED_RAID_TYPE_SCHEMA,
            ServicePoint.SERVICE_POINT,
            SpatialCoverageSchema.SPATIAL_COVERAGE_SCHEMA,
            SubjectType.SUBJECT_TYPE,
            SubjectTypeSchema.SUBJECT_TYPE_SCHEMA,
            TitleType.TITLE_TYPE,
            TitleTypeSchema.TITLE_TYPE_SCHEMA,
            Token.TOKEN,
            TraditionalKnowledgeLabel.TRADITIONAL_KNOWLEDGE_LABEL,
            TraditionalKnowledgeLabelSchema.TRADITIONAL_KNOWLEDGE_LABEL_SCHEMA,
            TraditionalKnowledgeNotice.TRADITIONAL_KNOWLEDGE_NOTICE,
            TraditionalKnowledgeNoticeSchema.TRADITIONAL_KNOWLEDGE_NOTICE_SCHEMA,
            UserAuthzRequest.USER_AUTHZ_REQUEST
        );
    }
}
