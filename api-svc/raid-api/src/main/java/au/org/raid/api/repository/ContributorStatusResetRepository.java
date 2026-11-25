package au.org.raid.api.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The contributors recorded in {@code contributor_status_reset} as authenticated through the
 * central ORCID integration, and whether each has been reset yet.
 */
@Repository
@RequiredArgsConstructor
public class ContributorStatusResetRepository {
    static final Table<?> RESET = DSL.table(DSL.name("contributor_status_reset"));
    static final Field<Integer> CONTRIBUTOR_ID = DSL.field(DSL.name("contributor_id"), Integer.class);
    static final Field<String> PID = DSL.field(DSL.name("pid"), String.class);
    static final Field<OffsetDateTime> RESET_AT = DSL.field(DSL.name("reset_at"), OffsetDateTime.class);

    private final DSLContext dslContext;

    /** The iDs of recorded contributors not yet reset. */
    public List<String> findPending(final int limit) {
        return dslContext.select(PID)
                .from(RESET)
                .where(RESET_AT.isNull())
                .orderBy(CONTRIBUTOR_ID)
                .limit(limit)
                .fetch(PID);
    }

    public void markReset(final String pid) {
        dslContext.update(RESET)
                .set(RESET_AT, OffsetDateTime.now())
                .where(PID.eq(pid))
                .execute();
    }
}
