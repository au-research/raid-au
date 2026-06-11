-- Fix subject schemaUri trailing slash in raid_history diffs
-- The enum SubjectSchemaURIEnum expects 'https://linked.data.gov.au/def/anzsrc-for/2020'
-- but some history entries have the trailing-slash variant which causes deserialization failure

update api_svc.raid_history
set diff = replace(
    diff::text,
    'https://linked.data.gov.au/def/anzsrc-for/2020/',
    'https://linked.data.gov.au/def/anzsrc-for/2020'
)::jsonb
where diff::text like '%linked.data.gov.au/def/anzsrc-for/2020/%';
