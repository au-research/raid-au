package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.RaidContributorRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static au.org.raid.db.jooq.tables.Contributor.CONTRIBUTOR;
import static au.org.raid.db.jooq.tables.RaidContributor.RAID_CONTRIBUTOR;

@Repository
@RequiredArgsConstructor
public class RaidContributorRepository {
    private final DSLContext dslContext;
    public RaidContributorRecord create(final RaidContributorRecord raidContributor) {
        return dslContext.insertInto(RAID_CONTRIBUTOR)
                .set(RAID_CONTRIBUTOR.HANDLE, raidContributor.getHandle())
                .set(RAID_CONTRIBUTOR.CONTRIBUTOR_ID, raidContributor.getContributorId())
                .set(RAID_CONTRIBUTOR.LEADER, raidContributor.getLeader())
                .set(RAID_CONTRIBUTOR.CONTACT, raidContributor.getContact())
                .returning()
                .fetchOne();
    }

    public List<RaidContributorRecord> findAllByHandle(final String handle) {
        return dslContext.selectFrom(RAID_CONTRIBUTOR)
                .where(RAID_CONTRIBUTOR.HANDLE.eq(handle))
                .fetch();
    }

    /** The handles of every RAiD whose current version names the contributor with this PID. */
    public List<String> findHandlesByContributorPid(final String pid) {
        return dslContext.selectDistinct(RAID_CONTRIBUTOR.HANDLE)
                .from(RAID_CONTRIBUTOR)
                .join(CONTRIBUTOR).on(CONTRIBUTOR.ID.eq(RAID_CONTRIBUTOR.CONTRIBUTOR_ID))
                .where(CONTRIBUTOR.PID.eq(pid))
                .orderBy(RAID_CONTRIBUTOR.HANDLE)
                .fetch(RAID_CONTRIBUTOR.HANDLE);
    }

    public void deleteAllByHandle(final String handle) {
        dslContext.deleteFrom(RAID_CONTRIBUTOR)
                .where(RAID_CONTRIBUTOR.HANDLE.eq(handle))
                .execute();
    }
}
