package au.org.raid.inttest.service;

import au.org.raid.idl.raidv2.model.*;

public class TestConstants {
    public static final String REAL_TEST_ORCID = "https://orcid.org/0009-0000-9306-3120";
    public static final String REAL_TEST_ROR = "https://ror.org/038sjwq14";

    public static final String INPUT_RELATED_OBJECT_CATEGORY =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json";
    public static final String OUTPUT_RELATED_OBJECT_CATEGORY =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json";
    public static final String RELATED_OBJECT_CATEGORY_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1/";
    public static final OrganizationRoleIdEnum LEAD_RESEARCH_ORGANISATION =
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182;

    public static final OrganizationRoleSchemaUriEnum ORGANISATION_ROLE_SCHEMA_URI =
            OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359;

    public static final OrganizationSchemaUriEnum ORGANISATION_IDENTIFIER_SCHEMA_URI = OrganizationSchemaUriEnum.HTTPS_ROR_ORG_;

    public static final AccessTypeIdEnum OPEN_ACCESS_TYPE =
            AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_;

    public static final String CLOSED_ACCESS_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json";

    public static final AccessTypeIdEnum EMBARGOED_ACCESS_TYPE =
            AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_;

    public static final AccessTypeSchemaUriEnum ACCESS_TYPE_SCHEMA_URI =
            AccessTypeSchemaUriEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_;

    public static final TitleTypeIdEnum PRIMARY_TITLE_TYPE =
            TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5;

    public static final TitleTypeIdEnum ALTERNATIVE_TITLE_TYPE =
            TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4;

    public static final TitleTypeSchemaURIEnum TITLE_TYPE_SCHEMA_URI =
            TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376;

    public static final String PRIMARY_DESCRIPTION_TYPE =
            "https://vocabulary.raid.org/description.type.schema/318";

    public static final DescriptionTypeIdEnum ALTERNATIVE_DESCRIPTION_TYPE =
            DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_319;

    public static final DescriptionTypeSchemaURIEnum DESCRIPTION_TYPE_SCHEMA_URI =
            DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320;

    public static final ContributorSchemaUriEnum CONTRIBUTOR_IDENTIFIER_SCHEMA_URI = ContributorSchemaUriEnum.HTTPS_ORCID_ORG_;

    public static final ContributorPositionSchemaUriEnum CONTRIBUTOR_POSITION_SCHEMA_URI =
            ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305;

    public static final ContributorPositionIdEnum PRINCIPAL_INVESTIGATOR_POSITION =
            ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307;

    public static final ContributorPositionIdEnum OTHER_PARTICIPANT_POSITION =
            ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_311;

    public static final ContributorRoleSchemaUriEnum CONTRIBUTOR_ROLE_SCHEMA_URI = ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_;

    public static final ContributorRoleIdEnum SOFTWARE_CONTRIBUTOR_ROLE =
            ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_SOFTWARE_;
    public static final String SUPERVISION_CONTRIBUTOR_ROLE =
            "https://credit.niso.org/contributor-roles/supervision/";
    public static final String WRITING_REVIEW_EDITING_CONTRIBUTOR_ROLE =
            "https://credit.niso.org/contributor-roles/writing-review-editing/";
    public static final String DATA_CURATION_CONTRIBUTOR_ROLE =
            "https://credit.niso.org/contributor-roles/data-curation/";
    public static final String CONCEPTUALIZATION_CONTRIBUTOR_ROLE =
            "https://credit.niso.org/contributor-roles/conceptualization/";
    public static final OrganizationRoleIdEnum LEAD_RESEARCH_ORGANISATION_ROLE =
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182;

    public static final String PARTNER_ORGANISATION_ROLE =
            "https://vocabulary.raid.org/organisation.role.schema/184";

    public static final String OTHER_ORGANISATION_ROLE =
            "https://vocabulary.raid.org/organisation.role.schema/188";

    public static final String CONTRACTOR_ORGANISATION_ROLE =
            "https://vocabulary.raid.org/organisation.role.schema/185";

    public static final String VALID_ROR = "https://ror.org/038sjwq14";

    public static final LanguageSchemaURIEnum LANGUAGE_SCHEMA_URI = LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML;
    public static final String LANGUAGE_ID = "eng";

    public static final String GEONAMES_MELBOURNE = "https://www.geonames.org/2158177/melbourne.html";

    public static final String GEONAMES_SCHEMA_URI = "https://www.geonames.org/";

    public static final String CONTRIBUTOR_EMAIL = "authenticated@test.raid.org.au";
}