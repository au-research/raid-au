-- Prod-only fix for ANZSRC-FOR subject schemaUri data so it matches the canonical
-- (no-trailing-slash) form used by the generated SubjectSchemaURIEnum.
--
-- Background
--   The 2.10.0 typed-enum change requires subject.schemaUri to be one of the enum's values, e.g.
--   'https://linked.data.gov.au/def/anzsrc-for/2020' (NO trailing slash). Existing prod data used
--   the trailing-slash form '.../anzsrc-for/2020/', which the enum rejects on deserialisation,
--   causing HTTP 500 on the /raid/ list endpoint (RaidIngestService.findAll reads raid.metadata).
--
--   The earlier prod-only V40.1 tried to fix this in raid_history with a blind text replace of
--   '.../anzsrc-for/2020/' -> '.../anzsrc-for/2020'. That also stripped the slash from subject IDs
--   (e.g. '.../anzsrc-for/2020/310305' -> '.../anzsrc-for/2020310305'), corrupting them, and it
--   never touched the raid table (the actual cause of the outage).
--
-- This migration completes and corrects that work. Every statement is idempotent / a no-op where
-- the data is already clean.

-- 1. Reference lookup: drop the trailing slash on the schema URI itself only.
--    (Leave id_starts_with prefixes alone - those legitimately end in a slash.)
update api_svc.subject_type_schema
set uri = 'https://linked.data.gov.au/def/anzsrc-for/2020'
where uri = 'https://linked.data.gov.au/def/anzsrc-for/2020/';

-- 2. raid.metadata (denormalised cache): rewrite only subject[].schemaUri, preserving subject IDs
--    such as '.../anzsrc-for/2020/52'. jsonb-precise, NOT a text replace, to avoid V40.1's mistake.
update api_svc.raid r
set metadata = jsonb_set(metadata, '{subject}', (
    select jsonb_agg(
        case
            when elem ->> 'schemaUri' = 'https://linked.data.gov.au/def/anzsrc-for/2020/'
                then jsonb_set(elem, '{schemaUri}', '"https://linked.data.gov.au/def/anzsrc-for/2020"'::jsonb)
            else elem
        end)
    from jsonb_array_elements(r.metadata -> 'subject') elem))
where r.metadata -> 'subject' @> '[{"schemaUri":"https://linked.data.gov.au/def/anzsrc-for/2020/"}]';

-- 3. raid_history.diff: repair subject IDs corrupted by V40.1 by re-inserting the slash before the
--    numeric FoR code. The schemaUri value is followed by '"' (not a digit) so it is not matched and
--    stays in the canonical no-slash form. No-op where IDs already contain the slash.
update api_svc.raid_history
set diff = regexp_replace(diff, 'anzsrc-for/2020([0-9])', 'anzsrc-for/2020/\1', 'g')
where diff ~ 'anzsrc-for/2020[0-9]';
