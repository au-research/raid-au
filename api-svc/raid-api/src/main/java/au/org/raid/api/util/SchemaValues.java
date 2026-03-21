package au.org.raid.api.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter
@RequiredArgsConstructor
public enum SchemaValues {
    ARKS_SCHEMA("https://arks.org/"),
    ISBN_SCHEMA("https://www.isbn-international.org/"),
    RRID_SCHEMA("https://scicrunch.org/resolver/"),
    ARCHIVE_ORG_SCHEMA("https://archive.org/"),
    DOI_SCHEMA("https://doi.org/"),
    ROR_SCHEMA_URI("https://ror.org/"),
    ORCID_SCHEMA_URI("https://orcid.org/"),
    GEONAMES_SCHEMA_URI("https://www.geonames.org/"),
    SUBJECT_SCHEMA_URI("https://vocabs.ardc.edu.au/viewById/316"),
    ACCESS_TYPE_OPEN("https://vocabularies.coar-repositories.org/access_rights/c_abf2/"),
    ACCESS_TYPE_EMBARGOED("https://vocabularies.coar-repositories.org/access_rights/c_f1cf/"),
    ACCESS_TYPE_SCHEMA("https://vocabularies.coar-repositories.org/access_rights/"),
    PRIMARY_DESCRIPTION_TYPE("https://vocabulary.raid.org/description.type.schema/318"),
    ALTERNATIVE_DESCRIPTION_TYPE("https://vocabulary.raid.org/description.type.schema/319"),
    DESCRIPTION_TYPE_SCHEMA("https://vocabulary.raid.org/description.type.schema/320"),
    PRIMARY_TITLE_TYPE("https://vocabulary.raid.org/title.type.schema/5"),
    ALTERNATIVE_TITLE_TYPE("https://vocabulary.raid.org/title.type.schema/4"),
    TITLE_TYPE_SCHEMA("https://vocabulary.raid.org/title.type.schema/376"),
    PRINCIPAL_INVESTIGATOR_CONTRIBUTOR_POSITION_ROLE("https://vocabulary.raid.org/contributor.position.schema/307"),
    CONTRIBUTOR_POSITION_SCHEMA_URI("https://vocabulary.raid.org/contributor.position.schema/305"),
    CONTRIBUTOR_ROLE_SCHEMA_URI("https://credit.niso.org"),
    LEAD_RESEARCH_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/182"),
    OTHER_RESEARCH_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/183"),
    PARTNER_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/184"),
    CONTRACTOR_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/185"),
    FUNDER_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/186"),
    FACILITY_RESEARCH_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/187"),
    OTHER_ORGANISATION_ROLE("https://vocabulary.raid.org/organisation.role.schema/188"),
    ORGANISATION_ROLE_SCHEMA_URI("https://vocabulary.raid.org/organisation.role.schema/359"),
    LANGUAGE_SCHEMA("https://www.iso.org/standard/74575.html"),
    RAID_SCHEMA_URI("https://raid.org/"),

    OUTPUT_MANAGEMENT_PLAN_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/247"),
    CONFERENCE_POSTER_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/248"),
    WORKFLOW_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/249"),
    JOURNAL_ARTICLE_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/250"),
    STANDARD_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/251"),
    REPORT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/252"),
    DISSERTATION_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/253"),
    PREPRINT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/254"),
    DATA_PAPER_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/255"),
    COMPUTATIONAL_NOTEBOOK_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/256"),
    IMAGE_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/257"),
    BOOK_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/258"),
    SOFTWARE_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/259"),
    EVENT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/260"),
    SOUND_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/261"),
    CONFERENCE_PROCEEDING_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/262"),
    MODEL_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/263"),
    CONFERENCE_PAPER_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/264"),
    TEXT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/265"),
    INSTRUMENT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/266"),
    LEARNING_OBJECT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/267"),
    PRIZE_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/268"),
    DATASET_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/269"),
    PHYSICAL_OBJECT_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/270"),
    BOOK_CHAPTER_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/271"),
    FUNDING_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/272"),
    AUDIO_VISUAL_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/273"),
    SERVICE_OBJECT_TYPE("https://vocabulary.raid.org/relatedObject.type.schema/274"),

    OUTPUT_RELATED_OBJECT_CATEGORY("https://vocabulary.raid.org/relatedObject.category.id/190"),
    INPUT_RELATED_OBJECT_CATEGORY("https://vocabulary.raid.org/relatedObject.category.id/191"),
    INTERNAL_PROCESS_DOCUMENT_RELATED_OBJECT_CATEGORY("https://vocabulary.raid.org/relatedObject.category.id/192"),

    SUBJECT_ID_PREFIX("https://linked.data.gov.au/def/anzsrc-for/2020/"),

    ;

    private final String uri;
}
