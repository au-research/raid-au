-- RAID-690: restore raid.date_created from the original mint timestamp
--
-- A bug in RaidRepository.update() overwrote date_created with LocalDateTime.now()
-- on every update. The raid_history table's version 1 record preserves the original
-- creation timestamp. This migration restores date_created and fixes the materialised
-- metadata JSON for all raids that have history.

UPDATE raid r
SET
    date_created = rh.created,
    metadata = CASE
        WHEN r.metadata IS NOT NULL
        THEN jsonb_set(
            r.metadata,
            '{metadata,created}',
            to_jsonb(extract(epoch FROM rh.created)::bigint)
        )
        ELSE r.metadata
    END
FROM raid_history rh
WHERE rh.handle = r.handle
  AND rh.revision = 1
  AND rh.change_type = 'PATCH';
