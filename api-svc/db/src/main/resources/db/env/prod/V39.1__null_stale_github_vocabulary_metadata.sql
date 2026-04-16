-- RAID-575: Clear stale metadata column for raids still containing
-- github.com/au-research/raid-metadata vocabulary URIs.
-- The ApplicationRunner backfill will re-materialise these from the
-- already-correct normalised tables on startup.
UPDATE api_svc.raid
SET metadata = NULL
WHERE metadata::text LIKE '%github.com/au-research/raid-metadata%';
