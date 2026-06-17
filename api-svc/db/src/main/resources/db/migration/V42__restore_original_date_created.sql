-- RAID-690: restore raid.date_created from the original mint timestamp
--
-- A bug in RaidRepository.update() overwrote date_created with LocalDateTime.now()
-- on every update. The raid_history table's version 1 record preserves the original
-- creation timestamp.
--
-- This migration fixes:
--   1-3. Raids with revision 1 history: date_created, metadata JSONB, history diffs
--   4-6. V1-imported raids (no revision 1): same three stores, sourced from revision 2 diff

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

-- 4. Fix V1-imported raids that have no revision 1 history record.
-- These raids' history starts at revision 2, which contains the original metadata.created
-- as an "add /metadata" operation. We source the true epoch from that diff because
-- metadata.created in the raid table may also have been corrupted by subsequent updates.
WITH v1_true_epoch AS (
    SELECT
        rh.handle,
        (elem->'value'->>'created')::bigint AS created_epoch
    FROM raid_history rh,
        jsonb_array_elements(rh.diff::jsonb) AS elem
    WHERE rh.revision = 2
      AND rh.change_type = 'PATCH'
      AND elem->>'path' = '/metadata'
      AND NOT EXISTS (
        SELECT 1 FROM raid_history rh2
        WHERE rh2.handle = rh.handle AND rh2.revision = 1 AND rh2.change_type = 'PATCH'
      )
)
UPDATE raid r
SET
    date_created = to_timestamp(v.created_epoch) AT TIME ZONE 'UTC',
    metadata = CASE
        WHEN r.metadata IS NOT NULL
        THEN jsonb_set(
            r.metadata,
            '{metadata,created}',
            to_jsonb(v.created_epoch)
        )
        ELSE r.metadata
    END
FROM v1_true_epoch v
WHERE v.handle = r.handle;

-- 5. Fix patch diffs for V1-imported raids that contain wrong /metadata/created values
WITH v1_true_epoch AS (
    SELECT
        rh.handle,
        (elem->'value'->>'created')::bigint AS created_epoch
    FROM raid_history rh,
        jsonb_array_elements(rh.diff::jsonb) AS elem
    WHERE rh.revision = 2
      AND rh.change_type = 'PATCH'
      AND elem->>'path' = '/metadata'
      AND NOT EXISTS (
        SELECT 1 FROM raid_history rh2
        WHERE rh2.handle = rh.handle AND rh2.revision = 1 AND rh2.change_type = 'PATCH'
      )
)
UPDATE raid_history rh
SET diff = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'path' = '/metadata/created'
            THEN jsonb_set(elem, '{value}', to_jsonb(v.created_epoch))
            ELSE elem
        END
        ORDER BY ord
    )::text
    FROM jsonb_array_elements(rh.diff::jsonb) WITH ORDINALITY AS t(elem, ord)
)
FROM v1_true_epoch v
WHERE v.handle = rh.handle
  AND rh.revision > 2
  AND rh.diff::jsonb @> '[{"path": "/metadata/created"}]';

-- 6. Fix baseline diffs for V1-imported raids
WITH v1_true_epoch AS (
    SELECT
        rh.handle,
        (elem->'value'->>'created')::bigint AS created_epoch
    FROM raid_history rh,
        jsonb_array_elements(rh.diff::jsonb) AS elem
    WHERE rh.revision = 2
      AND rh.change_type = 'PATCH'
      AND elem->>'path' = '/metadata'
      AND NOT EXISTS (
        SELECT 1 FROM raid_history rh2
        WHERE rh2.handle = rh.handle AND rh2.revision = 1 AND rh2.change_type = 'PATCH'
      )
)
UPDATE raid_history rh
SET diff = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'path' = '/metadata'
            THEN jsonb_set(elem, '{value,created}', to_jsonb(v.created_epoch))
            ELSE elem
        END
        ORDER BY ord
    )::text
    FROM jsonb_array_elements(rh.diff::jsonb) WITH ORDINALITY AS t(elem, ord)
)
FROM v1_true_epoch v
WHERE v.handle = rh.handle
  AND rh.change_type = 'BASELINE';
