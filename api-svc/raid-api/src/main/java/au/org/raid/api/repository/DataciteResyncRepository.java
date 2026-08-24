package au.org.raid.api.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static au.org.raid.db.jooq.tables.Raid.RAID;

@Repository
@RequiredArgsConstructor
public class DataciteResyncRepository {
    private final DSLContext dslContext;

    public List<String> findResyncRequired(final int limit) {
        return dslContext.select(RAID.HANDLE)
                .from(RAID)
                .where(RAID.DATACITE_RESYNC_REQUIRED.isTrue())
                .orderBy(RAID.HANDLE)
                .limit(limit)
                .fetch(RAID.HANDLE);
    }

    public void clearResyncRequired(final String handle) {
        dslContext.update(RAID)
                .set(RAID.DATACITE_RESYNC_REQUIRED, false)
                .where(RAID.HANDLE.eq(handle))
                .execute();
    }
}
