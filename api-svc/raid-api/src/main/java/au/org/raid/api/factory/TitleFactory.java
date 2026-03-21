package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.RaidTitleRecord;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
import org.springframework.stereotype.Component;

@Component
public class TitleFactory {
    public Title create(final RaidTitleRecord record, final String typeId, final String typeSchemaUri, final Language language) {
        return new Title()
                .text(record.getText())
                .startDate(record.getStartDate())
                .endDate(record.getEndDate())
                .type(new TitleType()
                        .id(typeId)
                        .schemaUri(typeSchemaUri)

                )
                .language(language);
    }
}