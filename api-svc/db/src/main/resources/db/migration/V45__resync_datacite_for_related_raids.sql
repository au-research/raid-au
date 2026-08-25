-- RAID-797: back-fill already-minted DataCite records so their related RAiDs are
-- re-emitted with DataCite's native "RAiD" relatedIdentifierType instead of the
-- generic "DOI".
--
-- This is the targeting migration for the reusable DataCite re-sync mechanism added
-- in RAID-832 (V44). It flags every DataCite (DOI) RAiD that references another RAiD.
-- The re-sync worker then re-pushes each flagged record through the idempotent
-- full-document PUT and clears the flag, so no manual step and no bespoke backfill is
-- needed, in any environment.
--
-- Only DOI handles are flagged (handle like '10.%', matching DataciteService.isDoi,
-- which checks handle.startsWith("10.")). Legacy/non-DOI handles are not registered in
-- DataCite, so re-pushing them would be wasted work; they are deliberately left alone.
update raid
set datacite_resync_required = true
where handle like '10.%'
  and exists (
      select 1 from related_raid rr where rr.handle = raid.handle
  );
