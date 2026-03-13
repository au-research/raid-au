package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RaidSubjectRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static au.org.raid.db.jooq.tables.RaidSubject.RAID_SUBJECT;

@Repository
@RequiredArgsConstructor
public class RaidSubjectRepository {
    private final DSLContext dslContext;

    public RaidSubjectRecord create(final RaidSubjectRecord record) {
        return dslContext.insertInto(RAID_SUBJECT)
                .set(RAID_SUBJECT.HANDLE, record.getHandle())
                .set(RAID_SUBJECT.SUBJECT_TYPE_ID, record.getSubjectTypeId())
                .set(RAID_SUBJECT.SUBJECT_TYPE_SCHEMA_ID, record.getSubjectTypeSchemaId())
                .returning()
                .fetchOne();
    }

    public List<RaidSubjectRecord> findAllByHandle(final String handle) {
        return dslContext.selectFrom(RAID_SUBJECT)
                .where(RAID_SUBJECT.HANDLE.eq(handle))
                .fetch();
    }

    public void deleteAllByHandle(final String handle) {
        dslContext.deleteFrom(RAID_SUBJECT)
                .where(RAID_SUBJECT.HANDLE.eq(handle))
                .execute();
    }
}
