package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteTitle;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleTypeIdEnum;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataciteTitleFactory {

    public DataciteTitle create(final Title title) {
        final var dataciteTitle =  new DataciteTitle()
                .setTitle(title.getText());

        // Don't set title type of raid title type is primary
        if (TITLE_TYPE_MAP.containsKey(title.getType().getId())) {
            dataciteTitle.setTitleType(TITLE_TYPE_MAP.get(title.getType().getId()));
        }

        if (title.getLanguage() != null) {
            dataciteTitle.setLang(title.getLanguage().getId());
        }

        return dataciteTitle;
    }

    private static final Map<TitleTypeIdEnum, String> TITLE_TYPE_MAP = Map.of(
            TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4, "AlternativeTitle",
            TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_156, "Other",
            TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_157, "Other"
            );
}
