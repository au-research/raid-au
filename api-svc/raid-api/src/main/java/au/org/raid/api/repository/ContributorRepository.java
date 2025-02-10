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
                .set(CONTRIBUTOR.UUID, contributor.getUuid())
                .returning()
                .fetchOne();
    }

    public ContributorRecord findOrCreate(final ContributorRecord contributor) {
        final var result = dslContext.selectFrom(CONTRIBUTOR)
                .where(CONTRIBUTOR.PID.eq(contributor.getPid())
                        .and(CONTRIBUTOR.SCHEMA_ID.eq(contributor.getSchemaId())))
                .fetchOptional();

        return result.orElseGet(() -> create(contributor));
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

    public void update(final ContributorRecord contributor) {
        dslContext.update(CONTRIBUTOR)
                .set(CONTRIBUTOR.UUID, contributor.getUuid())
                .set(CONTRIBUTOR.STATUS, contributor.getStatus())
                .where(CONTRIBUTOR.PID.eq(contributor.getPid()))
                .execute();
    }
}
