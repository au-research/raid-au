package au.org.raid.api.factory.record;

import au.org.raid.db.jooq.tables.records.RaidSubjectRecord;
import org.springframework.stereotype.Component;

@Component
public class RaidSubjectRecordFactory {
    public RaidSubjectRecord create(final String handle, final String subjectTypeId, final int schemaId) {
        return new RaidSubjectRecord()
                .setHandle(handle)
                .setSubjectTypeId(subjectTypeId)
                .setSubjectTypeSchemaId(schemaId);
    }
}
