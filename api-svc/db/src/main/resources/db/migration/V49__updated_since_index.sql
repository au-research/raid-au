-- Supports the RAID-837 "updatedSince" incremental federation sync filter on
-- /raid/all-public and /raid/all-embargoed (query/filtering logic lands in RAID-899).
--
-- There is no raid.updated column: the RAiD's update timestamp only exists inside
-- the materialised metadata JSONB, as metadata -> 'metadata' ->> 'updated', stored
-- as Unix epoch seconds. Legacy/null-metadata rows fall back to date_created so
-- they are never silently dropped from incremental sync (worst case they are
-- returned once extra, never missed).
--
-- IMPORTANT: this expression MUST match the predicate used in RaidRepository
-- (RAID-899) exactly:
--   coalesce((metadata -> 'metadata' ->> 'updated')::numeric, extract(epoch from date_created))
-- If the query expression diverges from this index expression, Postgres will not
-- use the index and the query will silently fall back to a full table scan.

-- Flyway is currently broken for using "concurrently", it will hang if you try
-- https://github.com/flyway/flyway/issues/3508
-- https://github.com/flyway/flyway/issues/3684
create index idx_raid_updated_since
  on raid(coalesce((metadata -> 'metadata' ->> 'updated')::numeric, extract(epoch from date_created)));

comment on index idx_raid_updated_since is
'created for RAID-837 incremental federation sync (the updatedSince query'
' parameter on /raid/all-public and /raid/all-embargoed); the indexed expression'
' must exactly match the predicate used in RaidRepository (RAID-899) or the'
' index silently stops being used';

analyze raid;
