package au.org.raid.api.util;

import au.org.raid.idl.raidv2.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TestRaid {
    public static final String HANDLE = "10.25.1.1/abcde";
    public static final String RAID_NAME = "https://raid.org/" + HANDLE;
    public static final String RAID_SCHEMA_URI = "https://raid.org/";
    public static final String RAID_AGENCY_URL = "https://static.raid.org.au/10.25.1.1/abcde";
    public static final int VERSION = 3;
    public static final String OWNER_ID = "https://ror.org/01ej9dk98";
    public static final String ROR_SCHEMA_URI = "https://ror.org/";
    public static final BigDecimal SERVICE_POINT_ID = new BigDecimal(20_000_000);
    public static final String REGISTRATION_AGENCY_ID = "https://ror.org/038sjwq14";
    public static final String LICENSE = "Creative Commons CC-0";
    public static final String START_DATE = "2021";
    public static final String END_DATE = "2022";
    public static final String TITLE_TEXT = "Raid Title Text";
    public static final String TITLE_START_DATE = "2021-01";
    public static final String TITLE_END_DATE = "2021-06";
    public static final String DESCRIPTION_TEXT = "Raid Title Text";
    public static final String LANGUAGE_CODE = "eng";
    public static final String LANGUAGE_SCHEMA_URI = "https://www.iso.org/standard/39534.html";
    public static final String CONTRIBUTOR_ID = "https://orcid.org/0009-0006-4129-5257";
    public static final String ORCID_SCHEMA_URI = "https://orcid.org/";
    public static final boolean IS_LEADER = true;
    public static final boolean IS_CONTACT = true;
    public static final String PRINCIPAL_INVESTIGATOR_POSITION_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json";
    public static final String CONTRIBUTOR_POSITION_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1";
    public static final String CONTRIBUTOR_POSITION_START_DATE = "2021-06-01";
    public static final String CONTRIBUTOR_POSITION_END_DATE = "2022-06-01";
    public static final String CONCEPTUALISATION_CONTRIBUTOR_ROLE_ID =
            "https://credit.niso.org/contributor-role/conceptualization/";
    public static final String CONTRIBUTOR_ROLE_SCHEMA_URI = "https://credit.niso.org/";
    public static final String ORGANISATION_ID = "https://ror.org/038sjwq14";
    public static final String ORGANISATION_SCHEMA_URI = "https://ror.org/";
    public static final String ORGANISATION_ROLE_START_DATE = "2021-07-12";
    public static final String ORGANISATION_ROLE_END_DATE = "2022";
    public static final String LEAD_RESEARCH_ORGANISATION_ROLE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json";
    public static final String ORGANISATION_ROLE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1";
    public static final String RELATED_OBJECT_ID = "https://doi.org/10.1000/182";
    public static final String RELATED_OBJECT_SCHEMA_URI = "https://doi.org/";
    public static final String RELATED_OBJECT_TYPE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/audiovisual.json";
    public static final String RELATED_OBJECT_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1";
    public static final String RELATED_OBJECT_CATEGORY_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json";
    public static final String RELATED_OBJECT_CATEGORY_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1";
    public static final String ALTERNATE_IDENTIFIER_ID = "1ec0c785-7608-417b-8d75-9fa5b75d74ce";
    public static final String ALTERNATE_IDENTIFIER_TYPE = "UUID";
    public static final String ALTERNATE_URL = "https://static.raid.org.au/10.25.1.1/abcde";
    public static final String RELATED_RAID_ID = "https://raid.org/10.25.1.1/bcdef";
    public static final String RELATED_RAID_TYPE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/continues.json";
    public static final String RELATED_RAID_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1/";
    public static final LocalDate EMBARGO_EXPIRY = LocalDate.now();
    public static final String ACCESS_STATEMENT_TEXT = "Access statement";
    public static final String ACCESS_TYPE_ID = AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_.getValue();
    public static final String ACCESS_TYPE_SCHEMA_URI = AccessTypeSchemaUriEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_.getValue();
    public static final String SUBJECT_ID = "https://linked.data.gov.au/def/anzsrc-for/2020/401099";
    public static final String SUBJECT_SCHEMA_URI = "https://linked.data.gov.au/def/anzsrc-for/2020/";
    public static final String SUBJECT_KEYWORD_TEXT =
            "unconstrained keyword or key phrase describing the project or activity";
    public static final String TRADITIONAL_KNOWLEDGE_LABEL_ID = "https://localcontexts.org/label/tk-attribution/";
    public static final String TRADITIONAL_KNOWLEDGE_LABEL_SCHEMA_URI =
            "https://localcontexts.org/labels/traditional-knowledge-labels/";
    public static final String SPATIAL_COVERAGE_ID = "https://www.openstreetmap.org/relation/62422";
    public static final String SPATIAL_COVERAGE_SCHEMA_URI = "https://www.openstreetmap.org/";
    public static final String SPATIAL_COVERAGE_PLACE_TEXT = "Berliner Urstromtal";
    public static final String TITLE_TYPE_ID = "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json";
    public static final String TITLE_TYPE_SCHEMA_URI = "https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1";

    public static final Language LANGUAGE = new Language()
            .id(LANGUAGE_CODE)
            .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);
    public static final Owner OWNER = new Owner()
            .id(OWNER_ID)
            .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_)
            .servicePoint(SERVICE_POINT_ID);
    public static final RegistrationAgency REGISTRATION_AGENCY = new RegistrationAgency()
            .id(REGISTRATION_AGENCY_ID)
            .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);
    public static final Id IDENTIFIER = new Id()
            .id(RAID_NAME)
            .schemaUri(RaidIdentifierSchemaURIEnum.HTTPS_RAID_ORG_)
            .raidAgencyUrl(RAID_AGENCY_URL)
            .registrationAgency(REGISTRATION_AGENCY)
            .owner(OWNER)
            .license(LICENSE)
            .version(VERSION);
    public static final Date DATES = new Date()
            .startDate(START_DATE)
            .endDate(END_DATE);
    public static final List<Title> TITLES = List.of(new Title()
            .type(new TitleType()
                    .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4)
                    .schemaUri(TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376))
            .text(TITLE_TEXT)
            .startDate(TITLE_START_DATE)
            .endDate(TITLE_END_DATE)
            .language(LANGUAGE));
    public static final List<Description> DESCRIPTIONS = List.of(new Description()
            .text(DESCRIPTION_TEXT)
            .language(LANGUAGE));
    public static final List<Contributor> CONTRIBUTORS = List.of(new Contributor()
            .id(CONTRIBUTOR_ID)
            .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
            .leader(IS_LEADER)
            .contact(IS_CONTACT)
            .position(List.of(new ContributorPosition()
                    .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307)
                    .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                    .startDate(CONTRIBUTOR_POSITION_START_DATE)
                    .endDate(CONTRIBUTOR_POSITION_END_DATE)
            ))
            .role(List.of(new ContributorRole()
                    .id(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_CONCEPTUALIZATION_)
                    .schemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_))));
    public static final List<Organisation> ORGANISATIONS = List.of(new Organisation()
            .id(ORGANISATION_ID)
            .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
            .role(List.of(new OrganisationRole()
                    .id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)
                    .schemaUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359)
                    .startDate(ORGANISATION_ROLE_START_DATE)
                    .endDate(ORGANISATION_ROLE_END_DATE))));
    public static final List<RelatedObject> RELATED_OBJECTS = List.of(new RelatedObject()
            .id(RELATED_OBJECT_ID)
            .schemaUri(RelatedObjectSchemaUriEnum.HTTP_DOI_ORG_)
            .type(new RelatedObjectType()
                    .id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247)
                    .schemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329))
            .category(List.of(new RelatedObjectCategory()
                    .id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)
                    .schemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_385))));
    public static final List<AlternateIdentifier> ALTERNATE_IDENTIFIERS = List.of(new AlternateIdentifier()
            .id(ALTERNATE_IDENTIFIER_ID)
            .type(ALTERNATE_IDENTIFIER_TYPE));
    public static final List<AlternateUrl> ALTERNATE_URLS = List.of(new AlternateUrl()
            .url(ALTERNATE_URL));
    public static final List<RelatedRaid> RELATED_RAIDS = List.of(new RelatedRaid()
            .id(RELATED_RAID_ID)
            .type(new RelatedRaidType()
                    .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198)
                    .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367)));
    public static final Access ACCESS = new Access()
            .embargoExpiry(EMBARGO_EXPIRY)
            .type(new AccessType()
                    .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_)
                    .schemaUri(AccessTypeSchemaUriEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_))
            .statement(new AccessStatement()
                    .text(ACCESS_STATEMENT_TEXT)
                    .language(LANGUAGE));
    public static final List<Subject> SUBJECTS = List.of(new Subject()
            .id(SUBJECT_ID)
            .schemaUri(SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316)
            .keyword(List.of(new SubjectKeyword()
                    .text(SUBJECT_KEYWORD_TEXT)
                    .language(LANGUAGE))));
    public static final List<TraditionalKnowledgeLabel> TRADITIONAL_KNOWLEDGE_LABELS = List.of(new TraditionalKnowledgeLabel()
            .id(TRADITIONAL_KNOWLEDGE_LABEL_ID)
            .schemaUri(TRADITIONAL_KNOWLEDGE_LABEL_SCHEMA_URI));
    public static final List<SpatialCoverage> SPATIAL_COVERAGES = List.of(new SpatialCoverage()
            .id(SPATIAL_COVERAGE_ID)
            .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)
            .place(List.of(new SpatialCoveragePlace()
                    .text(SPATIAL_COVERAGE_PLACE_TEXT)
                    .language(LANGUAGE))));
    public static final RaidDto RAID_DTO = new RaidDto()
            .identifier(IDENTIFIER)
            .date(DATES)
            .title(TITLES)
            .description(DESCRIPTIONS)
            .contributor(CONTRIBUTORS)
            .organisation(ORGANISATIONS)
            .relatedObject(RELATED_OBJECTS)
            .alternateIdentifier(ALTERNATE_IDENTIFIERS)
            .alternateUrl(ALTERNATE_URLS)
            .relatedRaid(RELATED_RAIDS)
            .access(ACCESS)
            .subject(SUBJECTS)
            .traditionalKnowledgeLabel(TRADITIONAL_KNOWLEDGE_LABELS)
            .spatialCoverage(SPATIAL_COVERAGES);
}
