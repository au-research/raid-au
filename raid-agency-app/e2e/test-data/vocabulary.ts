// RAID-536: Vocabulary URI constants sourced from general-mapping.json and the API spec

// Access types
export const ACCESS_TYPE = {
  OPEN: "https://vocabularies.coar-repositories.org/access_rights/c_abf2/",
  EMBARGOED:
    "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/",
} as const;

// Title types (field: title.type.schema)
export const TITLE_TYPE = {
  PRIMARY: "https://vocabulary.raid.org/title.type.schema/5",
  ALTERNATIVE: "https://vocabulary.raid.org/title.type.schema/4",
  ACRONYM: "https://vocabulary.raid.org/title.type.schema/156",
  SHORT: "https://vocabulary.raid.org/title.type.schema/157",
} as const;

// Description types (field: description.type.id)
export const DESCRIPTION_TYPE = {
  PRIMARY: "https://vocabulary.raid.org/description.type.schema/318",
  ALTERNATIVE: "https://vocabulary.raid.org/description.type.schema/319",
  BRIEF: "https://vocabulary.raid.org/description.type.schema/3",
  METHODS: "https://vocabulary.raid.org/description.type.schema/8",
  OBJECTIVES: "https://vocabulary.raid.org/description.type.schema/7",
  OTHER: "https://vocabulary.raid.org/description.type.schema/6",
  SIGNIFICANCE: "https://vocabulary.raid.org/description.type.schema/9",
  ACKNOWLEDGEMENTS: "https://vocabulary.raid.org/description.type.schema/392",
} as const;

// Contributor positions (field: contributor.position.id)
export const CONTRIBUTOR_POSITION = {
  PRINCIPAL_INVESTIGATOR:
    "https://vocabulary.raid.org/contributor.position.schema/307",
  CO_INVESTIGATOR:
    "https://vocabulary.raid.org/contributor.position.schema/308",
  PARTNER_INVESTIGATOR:
    "https://vocabulary.raid.org/contributor.position.schema/309",
  CONSULTANT: "https://vocabulary.raid.org/contributor.position.schema/310",
  OTHER_PARTICIPANT:
    "https://vocabulary.raid.org/contributor.position.schema/311",
} as const;

// Contributor roles — sourced from the RAiD vocabulary (CRediT taxonomy)
export const CONTRIBUTOR_ROLE = {
  CONCEPTUALIZATION:
    "https://credit.niso.org/contributor-roles/conceptualization/",
  DATA_CURATION: "https://credit.niso.org/contributor-roles/data-curation/",
  FORMAL_ANALYSIS: "https://credit.niso.org/contributor-roles/formal-analysis/",
  FUNDING_ACQUISITION:
    "https://credit.niso.org/contributor-roles/funding-acquisition/",
  INVESTIGATION: "https://credit.niso.org/contributor-roles/investigation/",
  METHODOLOGY: "https://credit.niso.org/contributor-roles/methodology/",
  PROJECT_ADMINISTRATION:
    "https://credit.niso.org/contributor-roles/project-administration/",
  RESOURCES: "https://credit.niso.org/contributor-roles/resources/",
  SOFTWARE: "https://credit.niso.org/contributor-roles/software/",
  SUPERVISION: "https://credit.niso.org/contributor-roles/supervision/",
  VALIDATION: "https://credit.niso.org/contributor-roles/validation/",
  VISUALIZATION: "https://credit.niso.org/contributor-roles/visualization/",
  WRITING_ORIGINAL_DRAFT:
    "https://credit.niso.org/contributor-roles/writing-original-draft/",
  WRITING_REVIEW_EDITING:
    "https://credit.niso.org/contributor-roles/writing-review-editing/",
} as const;

// Organisation roles (field: organisation.role.id)
export const ORGANISATION_ROLE = {
  LEAD_RESEARCH_ORGANISATION:
    "https://vocabulary.raid.org/organisation.role.schema/182",
  OTHER_RESEARCH_ORGANISATION:
    "https://vocabulary.raid.org/organisation.role.schema/183",
  PARTNER_ORGANISATION:
    "https://vocabulary.raid.org/organisation.role.schema/184",
  CONTRACTOR: "https://vocabulary.raid.org/organisation.role.schema/185",
  FUNDER: "https://vocabulary.raid.org/organisation.role.schema/186",
  FACILITY: "https://vocabulary.raid.org/organisation.role.schema/187",
  OTHER_ORGANISATION:
    "https://vocabulary.raid.org/organisation.role.schema/188",
} as const;

// Related object types (field: relatedObject.type.id)
export const RELATED_OBJECT_TYPE = {
  JOURNAL_ARTICLE:
    "https://vocabulary.raid.org/relatedObject.type.schema/250",
  DATASET: "https://vocabulary.raid.org/relatedObject.type.schema/269",
  SOFTWARE: "https://vocabulary.raid.org/relatedObject.type.schema/259",
  REPORT: "https://vocabulary.raid.org/relatedObject.type.schema/252",
  CONFERENCE_PAPER:
    "https://vocabulary.raid.org/relatedObject.type.schema/264",
} as const;

// Related object categories (field: relatedObject.category.id)
export const RELATED_OBJECT_CATEGORY = {
  INPUT: "https://vocabulary.raid.org/relatedObject.category.id/191",
  OUTPUT: "https://vocabulary.raid.org/relatedObject.category.id/190",
  INTERNAL_PROCESS:
    "https://vocabulary.raid.org/relatedObject.category.id/192",
} as const;

// Related RAiD types (field: relatedRaid.type.schema)
export const RELATED_RAID_TYPE = {
  CONTINUES: "https://vocabulary.raid.org/relatedRaid.type.schema/204",
  HAS_PART: "https://vocabulary.raid.org/relatedRaid.type.schema/201",
  IS_CONTINUED_BY:
    "https://vocabulary.raid.org/relatedRaid.type.schema/203",
  IS_DERIVED_FROM:
    "https://vocabulary.raid.org/relatedRaid.type.schema/200",
  IS_OBSOLETED_BY:
    "https://vocabulary.raid.org/relatedRaid.type.schema/205",
  IS_PART_OF: "https://vocabulary.raid.org/relatedRaid.type.schema/202",
  IS_SOURCE_OF: "https://vocabulary.raid.org/relatedRaid.type.schema/199",
  OBSOLETES: "https://vocabulary.raid.org/relatedRaid.type.schema/198",
} as const;

// Language schema URI
export const LANGUAGE_SCHEMA_URI =
  "https://www.iso.org/standard/74575.html";

// Common language IDs
export const LANGUAGE = {
  ENGLISH: "eng",
} as const;
