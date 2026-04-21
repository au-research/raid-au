package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteDescription;
import au.org.raid.idl.raidv2.model.Description;
import au.org.raid.idl.raidv2.model.DescriptionTypeIdEnum;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataciteDescriptionFactory {
    public DataciteDescription create(final Description description) {
        final var dataciteDescription =  new DataciteDescription()
                        .setDescription(description.getText())
                        .setDescriptionType(DESCRIPTION_TYP_MAP.get(description.getType().getId()));

        if (description.getLanguage() != null) {
            dataciteDescription.setLang(description.getLanguage().getId());
        }

        return dataciteDescription;
    }

    private static final Map<DescriptionTypeIdEnum, String> DESCRIPTION_TYP_MAP = Map.of(
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_319, "Other",
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318, "Abstract",
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3, "Other",
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_9, "Other",
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_8, "Methods",
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_7, "Other",
        DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_6, "Other"
    );
}
