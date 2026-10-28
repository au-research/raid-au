package au.org.raid.orcidupdater.orcid;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/** The membership entries the updater has written, with their put codes and last outcome. */
@Repository
@RequiredArgsConstructor
public class OrcidEntryRepository {
    private final JdbcClient jdbcClient;

    /** @param putCode null when ORCID hasn't accepted the entry yet */
    public record OrcidEntry(String orcidId, String raidHandle, Long putCode) {
    }

    public Optional<OrcidEntry> find(final String orcidId, final String raidHandle) {
        return jdbcClient.sql("SELECT orcid_id, raid_handle, put_code FROM orcid_entry WHERE orcid_id = :orcidId AND raid_handle = :raidHandle")
                .param("orcidId", orcidId)
                .param("raidHandle", raidHandle)
                .query(OrcidEntry.class)
                .optional();
    }

    public List<OrcidEntry> findByRaidHandle(final String raidHandle) {
        return jdbcClient.sql("SELECT orcid_id, raid_handle, put_code FROM orcid_entry WHERE raid_handle = :raidHandle ORDER BY orcid_id")
                .param("raidHandle", raidHandle)
                .query(OrcidEntry.class)
                .list();
    }

    public List<OrcidEntry> findByOrcidId(final String orcidId) {
        return jdbcClient.sql("SELECT orcid_id, raid_handle, put_code FROM orcid_entry WHERE orcid_id = :orcidId ORDER BY raid_handle")
                .param("orcidId", orcidId)
                .query(OrcidEntry.class)
                .list();
    }

    public void recordSuccess(final String orcidId, final String raidHandle, final long putCode, final Instant at) {
        jdbcClient.sql("""
                        INSERT INTO orcid_entry (orcid_id, raid_handle, put_code, last_attempt_at, last_success_at, failure)
                        VALUES (:orcidId, :raidHandle, :putCode, :at, :at, NULL)
                        ON CONFLICT (orcid_id, raid_handle) DO UPDATE SET put_code = excluded.put_code,
                                                                          last_attempt_at = excluded.last_attempt_at,
                                                                          last_success_at = excluded.last_success_at,
                                                                          failure = NULL
                        """)
                .param("orcidId", orcidId)
                .param("raidHandle", raidHandle)
                .param("putCode", putCode)
                .param("at", at.atOffset(ZoneOffset.UTC))
                .update();
    }

    /** Keeps any put code already held, so a failed update can still be retried as an update. */
    public void recordFailure(final String orcidId, final String raidHandle, final String failure, final Instant at) {
        jdbcClient.sql("""
                        INSERT INTO orcid_entry (orcid_id, raid_handle, last_attempt_at, failure)
                        VALUES (:orcidId, :raidHandle, :at, :failure)
                        ON CONFLICT (orcid_id, raid_handle) DO UPDATE SET last_attempt_at = excluded.last_attempt_at,
                                                                          failure = excluded.failure
                        """)
                .param("orcidId", orcidId)
                .param("raidHandle", raidHandle)
                .param("failure", failure)
                .param("at", at.atOffset(ZoneOffset.UTC))
                .update();
    }

    public void delete(final String orcidId, final String raidHandle) {
        jdbcClient.sql("DELETE FROM orcid_entry WHERE orcid_id = :orcidId AND raid_handle = :raidHandle")
                .param("orcidId", orcidId)
                .param("raidHandle", raidHandle)
                .update();
    }

    /** Forgets every entry and put code for this iD (RAID-1006). */
    public void deleteByOrcidId(final String orcidId) {
        jdbcClient.sql("DELETE FROM orcid_entry WHERE orcid_id = :orcidId")
                .param("orcidId", orcidId)
                .update();
    }
}
