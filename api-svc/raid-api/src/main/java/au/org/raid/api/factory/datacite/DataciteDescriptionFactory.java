package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteDescription;
import au.org.raid.idl.raidv2.model.Description;
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

    private static final Map<String, String> DESCRIPTION_TYP_MAP = Map.of(
        "https://vocabulary.raid.org/description.type.schema/319", "Other",
        "https://vocabulary.raid.org/description.type.schema/318", "Abstract",
            "https://vocabulary.raid.org/description.type.schema/3", "Other",
            "https://vocabulary.raid.org/description.type.schema/9", "Other",
            "https://vocabulary.raid.org/description.type.schema/8", "Methods",
            "https://vocabulary.raid.org/description.type.schema/7", "Other",
            "https://vocabulary.raid.org/description.type.schema/6", "Other"
    );
}
