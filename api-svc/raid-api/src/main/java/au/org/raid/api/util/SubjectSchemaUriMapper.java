package au.org.raid.api.util;

import au.org.raid.idl.raidv2.model.SubjectSchemaURIEnum;

import java.util.Map;

/**
 * Maps between SubjectSchemaURIEnum constants (which have human-readable labels
 * as their values after the LinkML v2 upgrade) and the URI strings stored in the
 * subject_type_schema DB table.
 */
public final class SubjectSchemaUriMapper {

    // DB URI -> enum constant
    private static final Map<String, SubjectSchemaURIEnum> URI_TO_ENUM = Map.of(
            "https://vocabs.ardc.edu.au/viewById/316", SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316,
            "https://vocabs.ardc.edu.au/viewById/317", SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_317
    );

    // enum constant -> DB URI
    private static final Map<SubjectSchemaURIEnum, String> ENUM_TO_URI = Map.of(
            SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316, "https://vocabs.ardc.edu.au/viewById/316",
            SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_317, "https://vocabs.ardc.edu.au/viewById/317",
            SubjectSchemaURIEnum.HTTPS_LINKED_DATA_GOV_AU_DEF_ANZSRC_FOR_2020, "https://vocabs.ardc.edu.au/viewById/316",
            SubjectSchemaURIEnum.HTTPS_LINKED_DATA_GOV_AU_DEF_ANZSRC_SEO_2020, "https://vocabs.ardc.edu.au/viewById/317"
    );

    private SubjectSchemaUriMapper() {
    }

    /**
     * Returns the DB URI for the given enum constant, or null if not mapped.
     */
    public static String toDbUri(SubjectSchemaURIEnum schemaUri) {
        return ENUM_TO_URI.get(schemaUri);
    }

    /**
     * Returns the enum constant for the given DB URI, or null if not mapped.
     */
    public static SubjectSchemaURIEnum fromDbUri(String dbUri) {
        return URI_TO_ENUM.get(dbUri);
    }
}
