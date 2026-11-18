package au.org.raid.api.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;

import static au.org.raid.db.jooq.tables.Raid.RAID;

/**
 * The queue of RAiDs to bring up to date with stored contributor statuses, held in
 * {@code raid.contributor_status_refresh_required}.
 */
@Repository
@RequiredArgsConstructor
public class ContributorStatusRefreshRepository {
    static final Field<Boolean> REFRESH_REQUIRED =
            DSL.field(DSL.name("contributor_status_refresh_required"), Boolean.class);

    private final DSLContext dslContext;

    public List<String> findRefreshRequired(final int limit) {
        return dslContext.select(RAID.HANDLE)
                .from(RAID)
                .where(REFRESH_REQUIRED.isTrue())
                .orderBy(RAID.HANDLE)
                .limit(limit)
                .fetch(RAID.HANDLE);
    }

    public void clearRefreshRequired(final String handle) {
        dslContext.update(RAID)
                .set(REFRESH_REQUIRED, false)
                .where(RAID.HANDLE.eq(handle))
                .execute();
    }
}
