package au.org.raid.api.util;

import java.time.LocalDate;

public class TestConstants {

    public static final String ACCESS_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1";

    public static final String OPEN_ACCESS_TYPE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json";

    public static final String EMBARGOED_ACCESS_TYPE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json";

    public static final String CLOSED_ACCESS_TYPE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json";

    public static final String TITLE_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1";

    public static final String TITLE = "Test Title";

    public static final String PRIMARY_TITLE_TYPE_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json";

    public static final String ALTERNATIVE_TITLE_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/alternative.json";

    public static final String UNKNOWN_TITLE_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/unknown.json";

    public static final String ORCID_SCHEMA_URI = "https://orcid.org/";

    public static final String ISNI_SCHEMA_URI = "https://isni.org/";

    public static final String CONTRIBUTOR_ROLE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/role/v1";

    public static final String SUPERVISION_CONTRIBUTOR_ROLE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/role/v1/supervision.json";

    public static final String CONTRIBUTOR_POSITION_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1";

    public static final String LEADER_CONTRIBUTOR_POSITION =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json";

    public static final String VALID_ORCID = "https://orcid.org/0000-0000-0000-0001";

    public static final String VALID_ISNI = "https://isni.org/0000000078519858";

    public static final String ORGANISATION_ROLE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1";

    public static final String LEAD_RESEARCH_ORGANISATION_ROLE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json";

    public static final String VALID_ROR = "https://ror.org/038sjwq14";

    public static final String HTTPS_ROR_ORG = "https://ror.org/";

    public static final String BOOK_CHAPTER_RELATED_OBJECT_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/related-object-type/book-chapter.json";

    public static final String INPUT_RELATED_OBJECT_CATEGORY =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json";

    public static final String RELATED_OBJECT_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1";

    public static final String RELATED_OBJECT_CATEGORY_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1";

    public static final String VALID_DOI = "http://doi.org/10.000/00000";

    public static final String HTTPS_DOI_ORG = "https://doi.org/";

    public static final String PRIMARY_DESCRIPTION_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json";

    public static final String DESCRIPTION_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1";

    public static final String RELATED_RAID_TYPE_SCHEMA_URI =
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1";

    public static final String CONTINUES_RELATED_RAID_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/continues.json";

    public static final LocalDate START_DATE = LocalDate.now().minusMonths(1);
    public static final LocalDate END_DATE = LocalDate.now();

    public static final String LANGUAGE_ID = "eng";

    public static final String LANGUAGE_SCHEMA_URI = "https://www.iso.org/standard/39534.html";
    public static final String ALTERNATIVE_DESCRIPTION_TYPE = "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/alternative.json";
}