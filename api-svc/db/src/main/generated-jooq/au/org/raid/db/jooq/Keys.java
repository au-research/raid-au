/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq;


import au.org.raid.db.jooq.tables.AccessType;
import au.org.raid.db.jooq.tables.AccessTypeSchema;
import au.org.raid.db.jooq.tables.AppUser;
import au.org.raid.db.jooq.tables.Contributor;
import au.org.raid.db.jooq.tables.ContributorPosition;
import au.org.raid.db.jooq.tables.ContributorPositionSchema;
import au.org.raid.db.jooq.tables.ContributorRole;
import au.org.raid.db.jooq.tables.ContributorRoleSchema;
import au.org.raid.db.jooq.tables.ContributorSchema;
import au.org.raid.db.jooq.tables.Description;
import au.org.raid.db.jooq.tables.DescriptionType;
import au.org.raid.db.jooq.tables.DescriptionTypeSchema;
import au.org.raid.db.jooq.tables.FlywaySchemaHistory;
import au.org.raid.db.jooq.tables.Language;
import au.org.raid.db.jooq.tables.LanguageSchema;
import au.org.raid.db.jooq.tables.Organisation;
import au.org.raid.db.jooq.tables.OrganisationRole;
import au.org.raid.db.jooq.tables.OrganisationRoleSchema;
import au.org.raid.db.jooq.tables.OrganisationSchema;
import au.org.raid.db.jooq.tables.Raid;
import au.org.raid.db.jooq.tables.RaidAlternateIdentifier;
import au.org.raid.db.jooq.tables.RaidAlternateUrl;
import au.org.raid.db.jooq.tables.RaidContributorPosition;
import au.org.raid.db.jooq.tables.RaidContributorRole;
import au.org.raid.db.jooq.tables.RaidHistory;
import au.org.raid.db.jooq.tables.RaidOrganisationRole;
import au.org.raid.db.jooq.tables.RaidRelatedObject;
import au.org.raid.db.jooq.tables.RaidSpatialCoverage;
import au.org.raid.db.jooq.tables.RaidSubjectType;
import au.org.raid.db.jooq.tables.RaidTraditionalKnowledgeLabel;
import au.org.raid.db.jooq.tables.RaidoOperator;
import au.org.raid.db.jooq.tables.RelatedObject;
import au.org.raid.db.jooq.tables.RelatedObjectCategory;
import au.org.raid.db.jooq.tables.RelatedObjectCategorySchema;
import au.org.raid.db.jooq.tables.RelatedObjectType;
import au.org.raid.db.jooq.tables.RelatedObjectTypeSchema;
import au.org.raid.db.jooq.tables.RelatedRaid;
import au.org.raid.db.jooq.tables.RelatedRaidType;
import au.org.raid.db.jooq.tables.RelatedRaidTypeSchema;
import au.org.raid.db.jooq.tables.ServicePoint;
import au.org.raid.db.jooq.tables.SpatialCoverageSchema;
import au.org.raid.db.jooq.tables.SubjectType;
import au.org.raid.db.jooq.tables.SubjectTypeSchema;
import au.org.raid.db.jooq.tables.Title;
import au.org.raid.db.jooq.tables.TitleType;
import au.org.raid.db.jooq.tables.TitleTypeSchema;
import au.org.raid.db.jooq.tables.Token;
import au.org.raid.db.jooq.tables.TraditionalKnowledgeLabel;
import au.org.raid.db.jooq.tables.TraditionalKnowledgeLabelSchema;
import au.org.raid.db.jooq.tables.UserAuthzRequest;
import au.org.raid.db.jooq.tables.records.AccessTypeRecord;
import au.org.raid.db.jooq.tables.records.AccessTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.AppUserRecord;
import au.org.raid.db.jooq.tables.records.ContributorPositionRecord;
import au.org.raid.db.jooq.tables.records.ContributorPositionSchemaRecord;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.db.jooq.tables.records.ContributorRoleRecord;
import au.org.raid.db.jooq.tables.records.ContributorRoleSchemaRecord;
import au.org.raid.db.jooq.tables.records.ContributorSchemaRecord;
import au.org.raid.db.jooq.tables.records.DescriptionRecord;
import au.org.raid.db.jooq.tables.records.DescriptionTypeRecord;
import au.org.raid.db.jooq.tables.records.DescriptionTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.FlywaySchemaHistoryRecord;
import au.org.raid.db.jooq.tables.records.LanguageRecord;
import au.org.raid.db.jooq.tables.records.LanguageSchemaRecord;
import au.org.raid.db.jooq.tables.records.OrganisationRecord;
import au.org.raid.db.jooq.tables.records.OrganisationRoleRecord;
import au.org.raid.db.jooq.tables.records.OrganisationRoleSchemaRecord;
import au.org.raid.db.jooq.tables.records.OrganisationSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidAlternateIdentifierRecord;
import au.org.raid.db.jooq.tables.records.RaidAlternateUrlRecord;
import au.org.raid.db.jooq.tables.records.RaidContributorPositionRecord;
import au.org.raid.db.jooq.tables.records.RaidContributorRoleRecord;
import au.org.raid.db.jooq.tables.records.RaidHistoryRecord;
import au.org.raid.db.jooq.tables.records.RaidOrganisationRoleRecord;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.db.jooq.tables.records.RaidRelatedObjectRecord;
import au.org.raid.db.jooq.tables.records.RaidSpatialCoverageRecord;
import au.org.raid.db.jooq.tables.records.RaidSubjectTypeRecord;
import au.org.raid.db.jooq.tables.records.RaidTraditionalKnowledgeLabelRecord;
import au.org.raid.db.jooq.tables.records.RaidoOperatorRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategoryRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategorySchemaRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.RelatedRaidRecord;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.db.jooq.tables.records.SpatialCoverageSchemaRecord;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import au.org.raid.db.jooq.tables.records.SubjectTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.TitleRecord;
import au.org.raid.db.jooq.tables.records.TitleTypeRecord;
import au.org.raid.db.jooq.tables.records.TitleTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.TokenRecord;
import au.org.raid.db.jooq.tables.records.TraditionalKnowledgeLabelRecord;
import au.org.raid.db.jooq.tables.records.TraditionalKnowledgeLabelSchemaRecord;
import au.org.raid.db.jooq.tables.records.UserAuthzRequestRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * api_svc.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<AccessTypeRecord> ACCESS_TYPE_PKEY = Internal.createUniqueKey(AccessType.ACCESS_TYPE, DSL.name("access_type_pkey"), new TableField[] { AccessType.ACCESS_TYPE.SCHEMA_ID, AccessType.ACCESS_TYPE.URI }, true);
    public static final UniqueKey<AccessTypeSchemaRecord> ACCESS_TYPE_SCHEMA_PKEY = Internal.createUniqueKey(AccessTypeSchema.ACCESS_TYPE_SCHEMA, DSL.name("access_type_schema_pkey"), new TableField[] { AccessTypeSchema.ACCESS_TYPE_SCHEMA.ID }, true);
    public static final UniqueKey<AppUserRecord> APP_USER_PKEY = Internal.createUniqueKey(AppUser.APP_USER, DSL.name("app_user_pkey"), new TableField[] { AppUser.APP_USER.ID }, true);
    public static final UniqueKey<ContributorRecord> CONTRIBUTOR_PKEY = Internal.createUniqueKey(Contributor.CONTRIBUTOR, DSL.name("contributor_pkey"), new TableField[] { Contributor.CONTRIBUTOR.ID }, true);
    public static final UniqueKey<ContributorPositionRecord> CONTRIBUTOR_POSITION_NEW_PKEY = Internal.createUniqueKey(ContributorPosition.CONTRIBUTOR_POSITION, DSL.name("contributor_position_new_pkey"), new TableField[] { ContributorPosition.CONTRIBUTOR_POSITION.ID }, true);
    public static final UniqueKey<ContributorPositionSchemaRecord> CONTRIBUTOR_POSITION_SCHEMA_PKEY = Internal.createUniqueKey(ContributorPositionSchema.CONTRIBUTOR_POSITION_SCHEMA, DSL.name("contributor_position_schema_pkey"), new TableField[] { ContributorPositionSchema.CONTRIBUTOR_POSITION_SCHEMA.ID }, true);
    public static final UniqueKey<ContributorRoleRecord> CONTRIBUTOR_ROLE_NEW_PKEY = Internal.createUniqueKey(ContributorRole.CONTRIBUTOR_ROLE, DSL.name("contributor_role_new_pkey"), new TableField[] { ContributorRole.CONTRIBUTOR_ROLE.ID }, true);
    public static final UniqueKey<ContributorRoleSchemaRecord> CONTRIBUTOR_ROLE_SCHEMA_PKEY = Internal.createUniqueKey(ContributorRoleSchema.CONTRIBUTOR_ROLE_SCHEMA, DSL.name("contributor_role_schema_pkey"), new TableField[] { ContributorRoleSchema.CONTRIBUTOR_ROLE_SCHEMA.ID }, true);
    public static final UniqueKey<ContributorSchemaRecord> CONTRIBUTOR_SCHEMA_PKEY = Internal.createUniqueKey(ContributorSchema.CONTRIBUTOR_SCHEMA, DSL.name("contributor_schema_pkey"), new TableField[] { ContributorSchema.CONTRIBUTOR_SCHEMA.ID }, true);
    public static final UniqueKey<DescriptionRecord> DESCRIPTION_PKEY = Internal.createUniqueKey(Description.DESCRIPTION, DSL.name("description_pkey"), new TableField[] { Description.DESCRIPTION.ID }, true);
    public static final UniqueKey<DescriptionTypeRecord> DESCRIPTION_TYPE_NEW_PKEY = Internal.createUniqueKey(DescriptionType.DESCRIPTION_TYPE, DSL.name("description_type_new_pkey"), new TableField[] { DescriptionType.DESCRIPTION_TYPE.ID }, true);
    public static final UniqueKey<DescriptionTypeSchemaRecord> DWSCRIPTION_TYPE_SCHEMA_PKEY = Internal.createUniqueKey(DescriptionTypeSchema.DESCRIPTION_TYPE_SCHEMA, DSL.name("dwscription_type_schema_pkey"), new TableField[] { DescriptionTypeSchema.DESCRIPTION_TYPE_SCHEMA.ID }, true);
    public static final UniqueKey<FlywaySchemaHistoryRecord> FLYWAY_SCHEMA_HISTORY_PK = Internal.createUniqueKey(FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, DSL.name("flyway_schema_history_pk"), new TableField[] { FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK }, true);
    public static final UniqueKey<LanguageSchemaRecord> LANGUAGE_SCHEMA_PKEY = Internal.createUniqueKey(LanguageSchema.LANGUAGE_SCHEMA, DSL.name("language_schema_pkey"), new TableField[] { LanguageSchema.LANGUAGE_SCHEMA.ID }, true);
    public static final UniqueKey<OrganisationRecord> ORGANISATION_PKEY = Internal.createUniqueKey(Organisation.ORGANISATION, DSL.name("organisation_pkey"), new TableField[] { Organisation.ORGANISATION.ID }, true);
    public static final UniqueKey<OrganisationRoleRecord> ORGANISATION_ROLE_NEW_PKEY = Internal.createUniqueKey(OrganisationRole.ORGANISATION_ROLE, DSL.name("organisation_role_new_pkey"), new TableField[] { OrganisationRole.ORGANISATION_ROLE.ID }, true);
    public static final UniqueKey<OrganisationRoleSchemaRecord> ORGANISATION_ROLE_SCHEMA_PKEY = Internal.createUniqueKey(OrganisationRoleSchema.ORGANISATION_ROLE_SCHEMA, DSL.name("organisation_role_schema_pkey"), new TableField[] { OrganisationRoleSchema.ORGANISATION_ROLE_SCHEMA.ID }, true);
    public static final UniqueKey<OrganisationSchemaRecord> ORGANISATION_SCHEMA_PKEY = Internal.createUniqueKey(OrganisationSchema.ORGANISATION_SCHEMA, DSL.name("organisation_schema_pkey"), new TableField[] { OrganisationSchema.ORGANISATION_SCHEMA.ID }, true);
    public static final UniqueKey<RaidRecord> RAID_PKEY = Internal.createUniqueKey(Raid.RAID, DSL.name("raid_pkey"), new TableField[] { Raid.RAID.HANDLE }, true);
    public static final UniqueKey<RaidAlternateIdentifierRecord> RAID_ALTERNATE_IDENTIFIER_PKEY = Internal.createUniqueKey(RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER, DSL.name("raid_alternate_identifier_pkey"), new TableField[] { RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER.RAID_NAME, RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER.ID, RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER.TYPE }, true);
    public static final UniqueKey<RaidAlternateUrlRecord> RAID_ALTERNATE_URL_PKEY = Internal.createUniqueKey(RaidAlternateUrl.RAID_ALTERNATE_URL, DSL.name("raid_alternate_url_pkey"), new TableField[] { RaidAlternateUrl.RAID_ALTERNATE_URL.RAID_NAME, RaidAlternateUrl.RAID_ALTERNATE_URL.URL }, true);
    public static final UniqueKey<RaidContributorPositionRecord> RAID_CONTRIBUTOR_POSITION_PKEY = Internal.createUniqueKey(RaidContributorPosition.RAID_CONTRIBUTOR_POSITION, DSL.name("raid_contributor_position_pkey"), new TableField[] { RaidContributorPosition.RAID_CONTRIBUTOR_POSITION.RAID_NAME, RaidContributorPosition.RAID_CONTRIBUTOR_POSITION.CONTRIBUTOR_ID, RaidContributorPosition.RAID_CONTRIBUTOR_POSITION.CONTRIBUTOR_POSITION_ID }, true);
    public static final UniqueKey<RaidContributorRoleRecord> RAID_CONTRIBUTOR_ROLE_PKEY = Internal.createUniqueKey(RaidContributorRole.RAID_CONTRIBUTOR_ROLE, DSL.name("raid_contributor_role_pkey"), new TableField[] { RaidContributorRole.RAID_CONTRIBUTOR_ROLE.RAID_NAME, RaidContributorRole.RAID_CONTRIBUTOR_ROLE.CONTRIBUTOR_ID, RaidContributorRole.RAID_CONTRIBUTOR_ROLE.CONTRIBUTOR_ROLE_ID }, true);
    public static final UniqueKey<RaidHistoryRecord> RAID_HISTORY_PKEY = Internal.createUniqueKey(RaidHistory.RAID_HISTORY, DSL.name("raid_history_pkey"), new TableField[] { RaidHistory.RAID_HISTORY.HANDLE, RaidHistory.RAID_HISTORY.REVISION, RaidHistory.RAID_HISTORY.CHANGE_TYPE }, true);
    public static final UniqueKey<RaidOrganisationRoleRecord> RAID_ORGANISATION_ROLE_PKEY = Internal.createUniqueKey(RaidOrganisationRole.RAID_ORGANISATION_ROLE, DSL.name("raid_organisation_role_pkey"), new TableField[] { RaidOrganisationRole.RAID_ORGANISATION_ROLE.RAID_NAME, RaidOrganisationRole.RAID_ORGANISATION_ROLE.ORGANISATION_ID, RaidOrganisationRole.RAID_ORGANISATION_ROLE.ORGANISATION_ROLE_ID }, true);
    public static final UniqueKey<RaidSpatialCoverageRecord> RAID_SPATIAL_COVERAGE_PKEY = Internal.createUniqueKey(RaidSpatialCoverage.RAID_SPATIAL_COVERAGE, DSL.name("raid_spatial_coverage_pkey"), new TableField[] { RaidSpatialCoverage.RAID_SPATIAL_COVERAGE.RAID_NAME, RaidSpatialCoverage.RAID_SPATIAL_COVERAGE.ID, RaidSpatialCoverage.RAID_SPATIAL_COVERAGE.SCHEMA_ID }, true);
    public static final UniqueKey<RaidSubjectTypeRecord> RAID_SUBJECT_TYPE_PKEY = Internal.createUniqueKey(RaidSubjectType.RAID_SUBJECT_TYPE, DSL.name("raid_subject_type_pkey"), new TableField[] { RaidSubjectType.RAID_SUBJECT_TYPE.RAID_NAME, RaidSubjectType.RAID_SUBJECT_TYPE.SUBJECT_TYPE_ID }, true);
    public static final UniqueKey<RaidTraditionalKnowledgeLabelRecord> RAID_TRADITIONAL_KNOWLEDGE_LABEL_PKEY = Internal.createUniqueKey(RaidTraditionalKnowledgeLabel.RAID_TRADITIONAL_KNOWLEDGE_LABEL, DSL.name("raid_traditional_knowledge_label_pkey"), new TableField[] { RaidTraditionalKnowledgeLabel.RAID_TRADITIONAL_KNOWLEDGE_LABEL.RAID_NAME, RaidTraditionalKnowledgeLabel.RAID_TRADITIONAL_KNOWLEDGE_LABEL.TRADITIONAL_KNOWLEDGE_LABEL_ID }, true);
    public static final UniqueKey<RaidoOperatorRecord> RAIDO_OPERATOR_PKEY = Internal.createUniqueKey(RaidoOperator.RAIDO_OPERATOR, DSL.name("raido_operator_pkey"), new TableField[] { RaidoOperator.RAIDO_OPERATOR.EMAIL }, true);
    public static final UniqueKey<RelatedObjectRecord> RELATED_OBJECT_PKEY = Internal.createUniqueKey(RelatedObject.RELATED_OBJECT, DSL.name("related_object_pkey"), new TableField[] { RelatedObject.RELATED_OBJECT.ID }, true);
    public static final UniqueKey<RelatedObjectCategoryRecord> RELATED_OBJECT_CATEGORY_NEW_PKEY = Internal.createUniqueKey(RelatedObjectCategory.RELATED_OBJECT_CATEGORY, DSL.name("related_object_category_new_pkey"), new TableField[] { RelatedObjectCategory.RELATED_OBJECT_CATEGORY.ID }, true);
    public static final UniqueKey<RelatedObjectCategorySchemaRecord> RELATED_OBJECT_CATEGORY_SCHEMA_PKEY = Internal.createUniqueKey(RelatedObjectCategorySchema.RELATED_OBJECT_CATEGORY_SCHEMA, DSL.name("related_object_category_schema_pkey"), new TableField[] { RelatedObjectCategorySchema.RELATED_OBJECT_CATEGORY_SCHEMA.ID }, true);
    public static final UniqueKey<RelatedObjectTypeRecord> RELATED_OBJECT_TYPE_NEW_PKEY1 = Internal.createUniqueKey(RelatedObjectType.RELATED_OBJECT_TYPE, DSL.name("related_object_type_new_pkey1"), new TableField[] { RelatedObjectType.RELATED_OBJECT_TYPE.ID }, true);
    public static final UniqueKey<RelatedObjectTypeSchemaRecord> RELATED_OBJECT_TYPE_SCHEMA_PKEY = Internal.createUniqueKey(RelatedObjectTypeSchema.RELATED_OBJECT_TYPE_SCHEMA, DSL.name("related_object_type_schema_pkey"), new TableField[] { RelatedObjectTypeSchema.RELATED_OBJECT_TYPE_SCHEMA.ID }, true);
    public static final UniqueKey<RelatedRaidRecord> RELATED_RAID_PKEY = Internal.createUniqueKey(RelatedRaid.RELATED_RAID, DSL.name("related_raid_pkey"), new TableField[] { RelatedRaid.RELATED_RAID.RAID_NAME, RelatedRaid.RELATED_RAID.RELATED_RAID_NAME }, true);
    public static final UniqueKey<RelatedRaidTypeRecord> RELATED_RAID_TYPE_NEW_PKEY1 = Internal.createUniqueKey(RelatedRaidType.RELATED_RAID_TYPE, DSL.name("related_raid_type_new_pkey1"), new TableField[] { RelatedRaidType.RELATED_RAID_TYPE.ID }, true);
    public static final UniqueKey<RelatedRaidTypeSchemaRecord> RELATED_RAID_TYPE_SCHEMA_PKEY = Internal.createUniqueKey(RelatedRaidTypeSchema.RELATED_RAID_TYPE_SCHEMA, DSL.name("related_raid_type_schema_pkey"), new TableField[] { RelatedRaidTypeSchema.RELATED_RAID_TYPE_SCHEMA.ID }, true);
    public static final UniqueKey<ServicePointRecord> SERVICE_POINT_PKEY = Internal.createUniqueKey(ServicePoint.SERVICE_POINT, DSL.name("service_point_pkey"), new TableField[] { ServicePoint.SERVICE_POINT.ID }, true);
    public static final UniqueKey<ServicePointRecord> UNIQUE_NAME = Internal.createUniqueKey(ServicePoint.SERVICE_POINT, DSL.name("unique_name"), new TableField[] { ServicePoint.SERVICE_POINT.LOWER_NAME }, true);
    public static final UniqueKey<SpatialCoverageSchemaRecord> SPATIAL_COVERAGE_SCHEMA_PKEY = Internal.createUniqueKey(SpatialCoverageSchema.SPATIAL_COVERAGE_SCHEMA, DSL.name("spatial_coverage_schema_pkey"), new TableField[] { SpatialCoverageSchema.SPATIAL_COVERAGE_SCHEMA.ID }, true);
    public static final UniqueKey<SubjectTypeRecord> SUBJECT_PKEY = Internal.createUniqueKey(SubjectType.SUBJECT_TYPE, DSL.name("subject_pkey"), new TableField[] { SubjectType.SUBJECT_TYPE.ID }, true);
    public static final UniqueKey<SubjectTypeSchemaRecord> SUBJECT_TYPE_SCHEMA_PKEY = Internal.createUniqueKey(SubjectTypeSchema.SUBJECT_TYPE_SCHEMA, DSL.name("subject_type_schema_pkey"), new TableField[] { SubjectTypeSchema.SUBJECT_TYPE_SCHEMA.ID }, true);
    public static final UniqueKey<TitleRecord> TITLE_PKEY = Internal.createUniqueKey(Title.TITLE, DSL.name("title_pkey"), new TableField[] { Title.TITLE.ID }, true);
    public static final UniqueKey<TitleTypeRecord> TITLE_TYPE_NEW_PKEY = Internal.createUniqueKey(TitleType.TITLE_TYPE, DSL.name("title_type_new_pkey"), new TableField[] { TitleType.TITLE_TYPE.ID }, true);
    public static final UniqueKey<TitleTypeSchemaRecord> TITLE_TYPE_SCHEMA_PKEY = Internal.createUniqueKey(TitleTypeSchema.TITLE_TYPE_SCHEMA, DSL.name("title_type_schema_pkey"), new TableField[] { TitleTypeSchema.TITLE_TYPE_SCHEMA.ID }, true);
    public static final UniqueKey<TokenRecord> TOKEN_PKEY = Internal.createUniqueKey(Token.TOKEN, DSL.name("token_pkey"), new TableField[] { Token.TOKEN.NAME, Token.TOKEN.ENVIRONMENT, Token.TOKEN.DATE_CREATED }, true);
    public static final UniqueKey<TraditionalKnowledgeLabelRecord> TRADITIONAL_KNOWLEDGE_LABEL_NEW_PKEY = Internal.createUniqueKey(TraditionalKnowledgeLabel.TRADITIONAL_KNOWLEDGE_LABEL, DSL.name("traditional_knowledge_label_new_pkey"), new TableField[] { TraditionalKnowledgeLabel.TRADITIONAL_KNOWLEDGE_LABEL.ID }, true);
    public static final UniqueKey<TraditionalKnowledgeLabelSchemaRecord> TRADITIONAL_KNOWLEDGE_LABEL_SCHEMA_PKEY = Internal.createUniqueKey(TraditionalKnowledgeLabelSchema.TRADITIONAL_KNOWLEDGE_LABEL_SCHEMA, DSL.name("traditional_knowledge_label_schema_pkey"), new TableField[] { TraditionalKnowledgeLabelSchema.TRADITIONAL_KNOWLEDGE_LABEL_SCHEMA.ID }, true);
    public static final UniqueKey<UserAuthzRequestRecord> USER_AUTHZ_REQUEST_PKEY = Internal.createUniqueKey(UserAuthzRequest.USER_AUTHZ_REQUEST, DSL.name("user_authz_request_pkey"), new TableField[] { UserAuthzRequest.USER_AUTHZ_REQUEST.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<AccessTypeRecord, AccessTypeSchemaRecord> ACCESS_TYPE__FK_ACCESS_TYPE_SCHEMA_ID = Internal.createForeignKey(AccessType.ACCESS_TYPE, DSL.name("fk_access_type_schema_id"), new TableField[] { AccessType.ACCESS_TYPE.SCHEMA_ID }, Keys.ACCESS_TYPE_SCHEMA_PKEY, new TableField[] { AccessTypeSchema.ACCESS_TYPE_SCHEMA.ID }, true);
    public static final ForeignKey<AppUserRecord, ServicePointRecord> APP_USER__APP_USER_SERVICE_POINT_ID_FKEY = Internal.createForeignKey(AppUser.APP_USER, DSL.name("app_user_service_point_id_fkey"), new TableField[] { AppUser.APP_USER.SERVICE_POINT_ID }, Keys.SERVICE_POINT_PKEY, new TableField[] { ServicePoint.SERVICE_POINT.ID }, true);
    public static final ForeignKey<ContributorRecord, ContributorSchemaRecord> CONTRIBUTOR__FK_CONTRIBUTOR_SCHEMA_ID = Internal.createForeignKey(Contributor.CONTRIBUTOR, DSL.name("fk_contributor_schema_id"), new TableField[] { Contributor.CONTRIBUTOR.SCHEMA_ID }, Keys.CONTRIBUTOR_SCHEMA_PKEY, new TableField[] { ContributorSchema.CONTRIBUTOR_SCHEMA.ID }, true);
    public static final ForeignKey<DescriptionRecord, RaidRecord> DESCRIPTION__FK_DESCRIPTION_RAID_NAME = Internal.createForeignKey(Description.DESCRIPTION, DSL.name("fk_description_raid_name"), new TableField[] { Description.DESCRIPTION.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<DescriptionRecord, DescriptionTypeRecord> DESCRIPTION__FK_DESCRIPTION_TYPE = Internal.createForeignKey(Description.DESCRIPTION, DSL.name("fk_description_type"), new TableField[] { Description.DESCRIPTION.DESCRIPTION_TYPE_ID }, Keys.DESCRIPTION_TYPE_NEW_PKEY, new TableField[] { DescriptionType.DESCRIPTION_TYPE.ID }, true);
    public static final ForeignKey<LanguageRecord, LanguageSchemaRecord> LANGUAGE__FK_LANGUAGE_SCHEMA_ID = Internal.createForeignKey(Language.LANGUAGE, DSL.name("fk_language_schema_id"), new TableField[] { Language.LANGUAGE.SCHEMA_ID }, Keys.LANGUAGE_SCHEMA_PKEY, new TableField[] { LanguageSchema.LANGUAGE_SCHEMA.ID }, true);
    public static final ForeignKey<OrganisationRecord, OrganisationSchemaRecord> ORGANISATION__FK_ORGANISATION_SCHEMA_ID = Internal.createForeignKey(Organisation.ORGANISATION, DSL.name("fk_organisation_schema_id"), new TableField[] { Organisation.ORGANISATION.SCHEMA_ID }, Keys.ORGANISATION_SCHEMA_PKEY, new TableField[] { OrganisationSchema.ORGANISATION_SCHEMA.ID }, true);
    public static final ForeignKey<RaidRecord, ServicePointRecord> RAID__RAID_SERVICE_POINT_ID_FKEY = Internal.createForeignKey(Raid.RAID, DSL.name("raid_service_point_id_fkey"), new TableField[] { Raid.RAID.SERVICE_POINT_ID }, Keys.SERVICE_POINT_PKEY, new TableField[] { ServicePoint.SERVICE_POINT.ID }, true);
    public static final ForeignKey<RaidAlternateIdentifierRecord, RaidRecord> RAID_ALTERNATE_IDENTIFIER__FK_RAID_ALTERNATE_IDENTIFIER_RAID_NAME = Internal.createForeignKey(RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER, DSL.name("fk_raid_alternate_identifier_raid_name"), new TableField[] { RaidAlternateIdentifier.RAID_ALTERNATE_IDENTIFIER.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidAlternateUrlRecord, RaidRecord> RAID_ALTERNATE_URL__FK_RAID_ALTERNATE_URL_RAID_NAME = Internal.createForeignKey(RaidAlternateUrl.RAID_ALTERNATE_URL, DSL.name("fk_raid_alternate_url_raid_name"), new TableField[] { RaidAlternateUrl.RAID_ALTERNATE_URL.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidContributorPositionRecord, ContributorRecord> RAID_CONTRIBUTOR_POSITION__FK_RAID_CONTRIBUTOR_CONTRIBUTOR_ID = Internal.createForeignKey(RaidContributorPosition.RAID_CONTRIBUTOR_POSITION, DSL.name("fk_raid_contributor_contributor_id"), new TableField[] { RaidContributorPosition.RAID_CONTRIBUTOR_POSITION.CONTRIBUTOR_ID }, Keys.CONTRIBUTOR_PKEY, new TableField[] { Contributor.CONTRIBUTOR.ID }, true);
    public static final ForeignKey<RaidContributorPositionRecord, ContributorPositionRecord> RAID_CONTRIBUTOR_POSITION__FK_RAID_CONTRIBUTOR_POSITION_CONTRIBUTOR_POSITION_ID = Internal.createForeignKey(RaidContributorPosition.RAID_CONTRIBUTOR_POSITION, DSL.name("fk_raid_contributor_position_contributor_position_id"), new TableField[] { RaidContributorPosition.RAID_CONTRIBUTOR_POSITION.CONTRIBUTOR_POSITION_ID }, Keys.CONTRIBUTOR_POSITION_NEW_PKEY, new TableField[] { ContributorPosition.CONTRIBUTOR_POSITION.ID }, true);
    public static final ForeignKey<RaidContributorPositionRecord, RaidRecord> RAID_CONTRIBUTOR_POSITION__FK_RAID_CONTRIBUTOR_POSITION_RAID_NAME = Internal.createForeignKey(RaidContributorPosition.RAID_CONTRIBUTOR_POSITION, DSL.name("fk_raid_contributor_position_raid_name"), new TableField[] { RaidContributorPosition.RAID_CONTRIBUTOR_POSITION.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidContributorRoleRecord, ContributorRecord> RAID_CONTRIBUTOR_ROLE__FK_RAID_CONTRIBUTOR_CONTRIBUTOR_ID = Internal.createForeignKey(RaidContributorRole.RAID_CONTRIBUTOR_ROLE, DSL.name("fk_raid_contributor_contributor_id"), new TableField[] { RaidContributorRole.RAID_CONTRIBUTOR_ROLE.CONTRIBUTOR_ID }, Keys.CONTRIBUTOR_PKEY, new TableField[] { Contributor.CONTRIBUTOR.ID }, true);
    public static final ForeignKey<RaidContributorRoleRecord, ContributorRoleRecord> RAID_CONTRIBUTOR_ROLE__FK_RAID_CONTRIBUTOR_ROLE_CONTRIBUTOR_ROLE_ID = Internal.createForeignKey(RaidContributorRole.RAID_CONTRIBUTOR_ROLE, DSL.name("fk_raid_contributor_role_contributor_role_id"), new TableField[] { RaidContributorRole.RAID_CONTRIBUTOR_ROLE.CONTRIBUTOR_ROLE_ID }, Keys.CONTRIBUTOR_ROLE_NEW_PKEY, new TableField[] { ContributorRole.CONTRIBUTOR_ROLE.ID }, true);
    public static final ForeignKey<RaidContributorRoleRecord, RaidRecord> RAID_CONTRIBUTOR_ROLE__FK_RAID_CONTRIBUTOR_ROLE_RAID_NAME = Internal.createForeignKey(RaidContributorRole.RAID_CONTRIBUTOR_ROLE, DSL.name("fk_raid_contributor_role_raid_name"), new TableField[] { RaidContributorRole.RAID_CONTRIBUTOR_ROLE.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidOrganisationRoleRecord, OrganisationRecord> RAID_ORGANISATION_ROLE__FK_RAID_ORGANISATION_ROLE_ORGANISATION_ID = Internal.createForeignKey(RaidOrganisationRole.RAID_ORGANISATION_ROLE, DSL.name("fk_raid_organisation_role_organisation_id"), new TableField[] { RaidOrganisationRole.RAID_ORGANISATION_ROLE.ORGANISATION_ID }, Keys.ORGANISATION_PKEY, new TableField[] { Organisation.ORGANISATION.ID }, true);
    public static final ForeignKey<RaidOrganisationRoleRecord, OrganisationRoleRecord> RAID_ORGANISATION_ROLE__FK_RAID_ORGANISATION_ROLE_ORGANISATION_ROLE_ID = Internal.createForeignKey(RaidOrganisationRole.RAID_ORGANISATION_ROLE, DSL.name("fk_raid_organisation_role_organisation_role_id"), new TableField[] { RaidOrganisationRole.RAID_ORGANISATION_ROLE.ORGANISATION_ROLE_ID }, Keys.ORGANISATION_ROLE_NEW_PKEY, new TableField[] { OrganisationRole.ORGANISATION_ROLE.ID }, true);
    public static final ForeignKey<RaidOrganisationRoleRecord, RaidRecord> RAID_ORGANISATION_ROLE__FK_RAID_ORGANISATION_ROLE_RAID_NAME = Internal.createForeignKey(RaidOrganisationRole.RAID_ORGANISATION_ROLE, DSL.name("fk_raid_organisation_role_raid_name"), new TableField[] { RaidOrganisationRole.RAID_ORGANISATION_ROLE.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidRelatedObjectRecord, RelatedObjectCategoryRecord> RAID_RELATED_OBJECT__FK_RAID_RELATED_OBJECT_RELATED_OBJECT_CATEGORY_ID = Internal.createForeignKey(RaidRelatedObject.RAID_RELATED_OBJECT, DSL.name("fk_raid_related_object_related_object_category_id"), new TableField[] { RaidRelatedObject.RAID_RELATED_OBJECT.RELATED_OBJECT_CATEGORY_ID }, Keys.RELATED_OBJECT_CATEGORY_NEW_PKEY, new TableField[] { RelatedObjectCategory.RELATED_OBJECT_CATEGORY.ID }, true);
    public static final ForeignKey<RaidRelatedObjectRecord, RelatedObjectRecord> RAID_RELATED_OBJECT__FK_RAID_RELATED_OBJECT_RELATED_OBJECT_ID = Internal.createForeignKey(RaidRelatedObject.RAID_RELATED_OBJECT, DSL.name("fk_raid_related_object_related_object_id"), new TableField[] { RaidRelatedObject.RAID_RELATED_OBJECT.RELATED_OBJECT_ID }, Keys.RELATED_OBJECT_PKEY, new TableField[] { RelatedObject.RELATED_OBJECT.ID }, true);
    public static final ForeignKey<RaidRelatedObjectRecord, RelatedObjectTypeRecord> RAID_RELATED_OBJECT__FK_RAID_RELATED_OBJECT_RELATED_OBJECT_TYPE_ID = Internal.createForeignKey(RaidRelatedObject.RAID_RELATED_OBJECT, DSL.name("fk_raid_related_object_related_object_type_id"), new TableField[] { RaidRelatedObject.RAID_RELATED_OBJECT.RELATED_OBJECT_TYPE_ID }, Keys.RELATED_OBJECT_TYPE_NEW_PKEY1, new TableField[] { RelatedObjectType.RELATED_OBJECT_TYPE.ID }, true);
    public static final ForeignKey<RaidSpatialCoverageRecord, RaidRecord> RAID_SPATIAL_COVERAGE__FK_RAID_SPATIAL_COVERAGE_RAID_NAME = Internal.createForeignKey(RaidSpatialCoverage.RAID_SPATIAL_COVERAGE, DSL.name("fk_raid_spatial_coverage_raid_name"), new TableField[] { RaidSpatialCoverage.RAID_SPATIAL_COVERAGE.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidSpatialCoverageRecord, SpatialCoverageSchemaRecord> RAID_SPATIAL_COVERAGE__FK_RAID_SPATIAL_COVERAGE_SCHEMA_ID = Internal.createForeignKey(RaidSpatialCoverage.RAID_SPATIAL_COVERAGE, DSL.name("fk_raid_spatial_coverage_schema_id"), new TableField[] { RaidSpatialCoverage.RAID_SPATIAL_COVERAGE.SCHEMA_ID }, Keys.SPATIAL_COVERAGE_SCHEMA_PKEY, new TableField[] { SpatialCoverageSchema.SPATIAL_COVERAGE_SCHEMA.ID }, true);
    public static final ForeignKey<RaidSubjectTypeRecord, RaidRecord> RAID_SUBJECT_TYPE__FK_RAID_SUBJECT_TYPE_RAID_NAME = Internal.createForeignKey(RaidSubjectType.RAID_SUBJECT_TYPE, DSL.name("fk_raid_subject_type_raid_name"), new TableField[] { RaidSubjectType.RAID_SUBJECT_TYPE.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RaidSubjectTypeRecord, SubjectTypeRecord> RAID_SUBJECT_TYPE__FK_RAID_SUBJECT_TYPE_SUBJECT_ID = Internal.createForeignKey(RaidSubjectType.RAID_SUBJECT_TYPE, DSL.name("fk_raid_subject_type_subject_id"), new TableField[] { RaidSubjectType.RAID_SUBJECT_TYPE.SUBJECT_TYPE_ID }, Keys.SUBJECT_PKEY, new TableField[] { SubjectType.SUBJECT_TYPE.ID }, true);
    public static final ForeignKey<RaidTraditionalKnowledgeLabelRecord, RaidRecord> RAID_TRADITIONAL_KNOWLEDGE_LABEL__FK_RAID_TRADITIONAL_KNOWLEDGE_LABEL_RAID_NAME = Internal.createForeignKey(RaidTraditionalKnowledgeLabel.RAID_TRADITIONAL_KNOWLEDGE_LABEL, DSL.name("fk_raid_traditional_knowledge_label_raid_name"), new TableField[] { RaidTraditionalKnowledgeLabel.RAID_TRADITIONAL_KNOWLEDGE_LABEL.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<RelatedRaidRecord, RaidRecord> RELATED_RAID__FK_RELATED_RAID_RAID_NAME = Internal.createForeignKey(RelatedRaid.RELATED_RAID, DSL.name("fk_related_raid_raid_name"), new TableField[] { RelatedRaid.RELATED_RAID.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<SubjectTypeRecord, SubjectTypeSchemaRecord> SUBJECT_TYPE__FK_SUBJECT_TYPE_SCHEMA_ID = Internal.createForeignKey(SubjectType.SUBJECT_TYPE, DSL.name("fk_subject_type_schema_id"), new TableField[] { SubjectType.SUBJECT_TYPE.SCHEMA_ID }, Keys.SUBJECT_TYPE_SCHEMA_PKEY, new TableField[] { SubjectTypeSchema.SUBJECT_TYPE_SCHEMA.ID }, true);
    public static final ForeignKey<TitleRecord, RaidRecord> TITLE__FK_TITLE_RAID_NAME = Internal.createForeignKey(Title.TITLE, DSL.name("fk_title_raid_name"), new TableField[] { Title.TITLE.RAID_NAME }, Keys.RAID_PKEY, new TableField[] { Raid.RAID.HANDLE }, true);
    public static final ForeignKey<TitleRecord, TitleTypeRecord> TITLE__FK_TITLE_TYPE = Internal.createForeignKey(Title.TITLE, DSL.name("fk_title_type"), new TableField[] { Title.TITLE.TITLE_TYPE_ID }, Keys.TITLE_TYPE_NEW_PKEY, new TableField[] { TitleType.TITLE_TYPE.ID }, true);
    public static final ForeignKey<UserAuthzRequestRecord, AppUserRecord> USER_AUTHZ_REQUEST__USER_AUTHZ_REQUEST_APPROVED_USER_FKEY = Internal.createForeignKey(UserAuthzRequest.USER_AUTHZ_REQUEST, DSL.name("user_authz_request_approved_user_fkey"), new TableField[] { UserAuthzRequest.USER_AUTHZ_REQUEST.APPROVED_USER }, Keys.APP_USER_PKEY, new TableField[] { AppUser.APP_USER.ID }, true);
    public static final ForeignKey<UserAuthzRequestRecord, AppUserRecord> USER_AUTHZ_REQUEST__USER_AUTHZ_REQUEST_RESPONDING_USER_FKEY = Internal.createForeignKey(UserAuthzRequest.USER_AUTHZ_REQUEST, DSL.name("user_authz_request_responding_user_fkey"), new TableField[] { UserAuthzRequest.USER_AUTHZ_REQUEST.RESPONDING_USER }, Keys.APP_USER_PKEY, new TableField[] { AppUser.APP_USER.ID }, true);
    public static final ForeignKey<UserAuthzRequestRecord, ServicePointRecord> USER_AUTHZ_REQUEST__USER_AUTHZ_REQUEST_SERVICE_POINT_ID_FKEY = Internal.createForeignKey(UserAuthzRequest.USER_AUTHZ_REQUEST, DSL.name("user_authz_request_service_point_id_fkey"), new TableField[] { UserAuthzRequest.USER_AUTHZ_REQUEST.SERVICE_POINT_ID }, Keys.SERVICE_POINT_PKEY, new TableField[] { ServicePoint.SERVICE_POINT.ID }, true);
}
