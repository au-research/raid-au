-- RAID-690: restore raid.date_created from the original mint timestamp
--
-- A bug in RaidRepository.update() overwrote date_created with LocalDateTime.now()
-- on every update. The raid_history table's version 1 record preserves the original
-- creation timestamp.
--
-- This migration fixes three data stores:
--   1. raid.date_created column
--   2. raid.metadata materialised JSONB (metadata.created field)
--   3. raid_history diff chain (patch and baseline diffs with wrong metadata.created)

-- 1. Restore raid.date_created and fix materialised metadata
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

-- 2. Fix patch diffs that replaced /metadata/created with the wrong value
WITH correct_created AS (
    SELECT
        rh.handle,
        extract(epoch FROM rh.created)::bigint AS created_epoch
    FROM raid_history rh
    WHERE rh.revision = 1
      AND rh.change_type = 'PATCH'
)
UPDATE raid_history rh
SET diff = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'path' = '/metadata/created'
            THEN jsonb_set(elem, '{value}', to_jsonb(cc.created_epoch))
            ELSE elem
        END
        ORDER BY ord
    )::text
    FROM jsonb_array_elements(rh.diff::jsonb) WITH ORDINALITY AS t(elem, ord)
)
FROM correct_created cc
WHERE cc.handle = rh.handle
  AND rh.revision > 1
  AND rh.diff::jsonb @> '[{"path": "/metadata/created"}]';

-- 3. Fix baseline diffs that contain the wrong metadata.created inside /metadata value
WITH correct_created AS (
    SELECT
        rh.handle,
        extract(epoch FROM rh.created)::bigint AS created_epoch
    FROM raid_history rh
    WHERE rh.revision = 1
      AND rh.change_type = 'PATCH'
)
UPDATE raid_history rh
SET diff = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'path' = '/metadata'
            THEN jsonb_set(elem, '{value,created}', to_jsonb(cc.created_epoch))
            ELSE elem
        END
        ORDER BY ord
    )::text
    FROM jsonb_array_elements(rh.diff::jsonb) WITH ORDINALITY AS t(elem, ord)
)
FROM correct_created cc
WHERE cc.handle = rh.handle
  AND rh.change_type = 'BASELINE';
