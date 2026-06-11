package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.LanguageSchemaURIEnum;
import org.springframework.stereotype.Component;

@Component
public class LanguageFactory {
    public Language create(final String id, final String schemaUri) {
        return new Language()
                .id(id)
                .schemaUri(LanguageSchemaURIEnum.fromValue(schemaUri));
    }
}
