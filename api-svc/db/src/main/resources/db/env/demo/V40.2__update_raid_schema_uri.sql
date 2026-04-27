UPDATE api_svc.raid
SET schema_uri = 'https://raid.org/',
    metadata = jsonb_set(
        metadata::jsonb,
        '{identifier,schemaUri}',
        '"https://raid.org/"'
    )
WHERE schema_uri IS NULL
   OR schema_uri != 'https://raid.org/'
   OR metadata::jsonb->'identifier'->>'schemaUri' IS DISTINCT FROM 'https://raid.org/';

UPDATE api_svc.raid
SET metadata = jsonb_set(
    metadata::jsonb,
    '{identifier,schemaUri}',
    '"https://raid.org/"'
)
WHERE metadata IS NOT NULL
  AND metadata::jsonb->'identifier'->>'schemaUri' IS DISTINCT FROM 'https://raid.org/';

UPDATE api_svc.raid_history
SET diff = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'path' = '/identifier/schemaUri' AND elem->>'value' IS DISTINCT FROM 'https://raid.org/'
                THEN jsonb_set(elem, '{value}', '"https://raid.org/"')
            ELSE elem
        END
    )
    FROM jsonb_array_elements(diff::jsonb) elem
)::text
WHERE EXISTS (
    SELECT 1 FROM jsonb_array_elements(diff::jsonb) elem
    WHERE elem->>'path' = '/identifier/schemaUri'
      AND elem->>'value' IS DISTINCT FROM 'https://raid.org/'
);
