package au.org.raid.orcidupdater.work;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class WorkRepository {
    private final JdbcClient jdbcClient;

    public enum Kind { STATUS, RAID_SYNC }

    public record WorkItem(Kind kind, String key, int generation, int attempts) {
    }

    /**
     * Asks for the work to be done. A request for an item already waiting, failed or in progress
     * is merged into it and makes it due now; the generation tells a worker finishing the earlier
     * request that there is more to do.
     */
    public void request(final Kind kind, final String key, final Instant now) {
        jdbcClient.sql("""
                        INSERT INTO work_item (kind, item_key, requested_at, next_attempt_at)
                        VALUES (:kind, :key, :now, :now)
                        ON CONFLICT (kind, item_key) DO UPDATE
                            SET requested_at = excluded.requested_at,
                                next_attempt_at = excluded.next_attempt_at,
                                generation = work_item.generation + 1,
                                attempts = 0,
                                failed_at = NULL,
                                failure = NULL
                        """)
                .param("kind", kind.name())
                .param("key", key)
                .param("now", utc(now))
                .update();
    }

    /**
     * Takes up to {@code limit} due items for this instance until {@code claimedUntil}. Rows
     * another instance is claiming at the same moment are skipped, not waited for.
     */
    public List<WorkItem> claimDue(final Instant now, final Instant claimedUntil, final int limit) {
        return jdbcClient.sql("""
                        UPDATE work_item SET claimed_until = :claimedUntil
                        WHERE (kind, item_key) IN (
                            SELECT kind, item_key FROM work_item
                            WHERE failed_at IS NULL AND next_attempt_at <= :now
                              AND (claimed_until IS NULL OR claimed_until < :now)
                            ORDER BY next_attempt_at
                            LIMIT :limit
                            FOR UPDATE SKIP LOCKED)
                        RETURNING kind, item_key, generation, attempts
                        """)
                .param("now", utc(now))
                .param("claimedUntil", utc(claimedUntil))
                .param("limit", limit)
                .query((rs, row) -> new WorkItem(Kind.valueOf(rs.getString("kind")), rs.getString("item_key"),
                        rs.getInt("generation"), rs.getInt("attempts")))
                .list();
    }

    /** Done. Kept, and released for another pass, if it was requested again meanwhile. */
    public void complete(final WorkItem item) {
        final var deleted = jdbcClient.sql("""
                        DELETE FROM work_item WHERE kind = :kind AND item_key = :key AND generation = :generation
                        """)
                .param("kind", item.kind().name())
                .param("key", item.key())
                .param("generation", item.generation())
                .update();
        if (deleted == 0) {
            release(item);
        }
    }

    /**
     * A failed attempt: tried again at {@code nextAttemptAt}, or, when that is null, recorded as
     * failed and left for operators. A request made meanwhile wins and is due straight away.
     */
    public void recordFailure(final WorkItem item, final String failure, final Instant now, final Instant nextAttemptAt) {
        final var updated = jdbcClient.sql("""
                        UPDATE work_item
                        SET attempts = attempts + 1, failure = :failure, claimed_until = NULL,
                            next_attempt_at = COALESCE(CAST(:nextAttemptAt AS timestamptz), next_attempt_at),
                            failed_at = CASE WHEN CAST(:nextAttemptAt AS timestamptz) IS NULL THEN CAST(:now AS timestamptz) END
                        WHERE kind = :kind AND item_key = :key AND generation = :generation
                        """)
                .param("kind", item.kind().name())
                .param("key", item.key())
                .param("generation", item.generation())
                .param("failure", failure)
                .param("now", utc(now))
                .param("nextAttemptAt", nextAttemptAt == null ? null : utc(nextAttemptAt))
                .update();
        if (updated == 0) {
            release(item);
        }
    }

    /**
     * Queues a status change for each unused invitation that has expired since the last call, so
     * the researcher's status drops back from AWAITING_AUTHENTICATION. Each expiry is queued once.
     */
    public int requestStatusForExpiredInvitations(final Instant now) {
        return jdbcClient.sql("""
                        WITH expired AS (
                            UPDATE invitation SET expiry_queued_at = :now
                            WHERE used_at IS NULL AND expiry_queued_at IS NULL AND expires_at <= :now
                            RETURNING orcid_id)
                        INSERT INTO work_item (kind, item_key, requested_at, next_attempt_at)
                        SELECT DISTINCT 'STATUS', orcid_id, :now, :now FROM expired
                        ON CONFLICT (kind, item_key) DO UPDATE
                            SET requested_at = excluded.requested_at,
                                next_attempt_at = excluded.next_attempt_at,
                                generation = work_item.generation + 1,
                                attempts = 0,
                                failed_at = NULL,
                                failure = NULL
                        """)
                .param("now", utc(now))
                .update();
    }

    private void release(final WorkItem item) {
        jdbcClient.sql("UPDATE work_item SET claimed_until = NULL WHERE kind = :kind AND item_key = :key")
                .param("kind", item.kind().name())
                .param("key", item.key())
                .update();
    }

    private static OffsetDateTime utc(final Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
