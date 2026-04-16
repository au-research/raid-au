package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.RaidDescriptionRecord;
import au.org.raid.idl.raidv2.model.Description;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.Language;
import org.springframework.stereotype.Component;

@Component
public class DescriptionFactory {
    public Description create(
            final RaidDescriptionRecord record,
            final String typeId,
            final String typeSchemaUri,
            final Language language) {
        return new Description()
                .text(record.getText())
                .type(new DescriptionType()
                        .id(typeId)
                        .schemaUri(typeSchemaUri)
                )
                .language(language);
    }
}