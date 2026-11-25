package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.ContributorRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static au.org.raid.db.jooq.tables.Contributor.CONTRIBUTOR;

@Repository
@RequiredArgsConstructor
public class ContributorRepository {
    private final DSLContext dslContext;
    public ContributorRecord create(final ContributorRecord contributor) {
        return dslContext.insertInto(CONTRIBUTOR)
                .set(CONTRIBUTOR.PID, contributor.getPid())
                .set(CONTRIBUTOR.SCHEMA_ID, contributor.getSchemaId())
                .set(CONTRIBUTOR.STATUS, contributor.getStatus())
                .returning()
                .fetchOne();
    }

    public ContributorRecord updateOrCreate(final ContributorRecord contributor) {
        final var result = dslContext.selectFrom(CONTRIBUTOR)
                .where(CONTRIBUTOR.PID.eq(contributor.getPid()))
                .fetchOptional();

        if (result.isPresent()) {
            // Status is left alone: only updateStatus changes it, so saving a RAiD can never overwrite
            // a status the ORCID updater set in the meantime.
            return dslContext.update(CONTRIBUTOR)
                    .set(CONTRIBUTOR.SCHEMA_ID, contributor.getSchemaId())
                    .where(CONTRIBUTOR.PID.eq(contributor.getPid()))
                    .returning()
                    .fetchOne();
        } else {
            return create(contributor);
        }
    }

    public Optional<ContributorRecord> findById(final Integer id) {
        return dslContext.selectFrom(CONTRIBUTOR)
                .where(CONTRIBUTOR.ID.eq(id))
                .fetchOptional();
    }

    public Optional<ContributorRecord> findByPid(final String pid) {
        return dslContext.selectFrom(CONTRIBUTOR)
                .where(CONTRIBUTOR.PID.eq(pid))
                .fetchOptional();
    }

    /** @return the number of rows updated: 0 when no contributor has this PID */
    public int updateStatus(final String pid, final String status) {
        return dslContext.update(CONTRIBUTOR)
                .set(CONTRIBUTOR.STATUS, status)
                .where(CONTRIBUTOR.PID.eq(pid))
                .execute();
    }
}
