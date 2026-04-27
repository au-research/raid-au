UPDATE api_svc.raid
SET schema_uri = 'https://raid.org/'
WHERE schema_uri IS NULL
   OR schema_uri != 'https://raid.org/';

UPDATE api_svc.raid_history
SET diff = jsonb_set(
    diff::jsonb,
    '{identifier,schemaUri}',
    '"https://raid.org/"'
)::text
WHERE diff::jsonb->'identifier'->>'schemaUri' IS DISTINCT FROM 'https://raid.org/';
