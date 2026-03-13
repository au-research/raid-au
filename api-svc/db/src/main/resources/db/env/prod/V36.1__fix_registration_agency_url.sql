UPDATE api_svc.raid_history
SET diff = (
    SELECT jsonb_agg(
                   CASE
                       WHEN elem->>'path' = '/identifier' THEN
                           jsonb_set(
                                   elem,
                                   '{value,raidAgencyUrl}',
                                   to_jsonb(
                                           'https://static.prod.raid.org.au/raids/' ||
                                               -- Extract everything after the scheme and domain (after the third /)
                                           regexp_replace(elem->'value'->>'id', '^https?://[^/]+/', '')
                                   )
                           )
                       ELSE elem
                       END
           )
    FROM jsonb_array_elements(diff::jsonb) AS elem
)
WHERE diff::jsonb @> '[{"path": "/identifier"}]'::jsonb;

-- Update raidAgencyUrl in LEGACY format records (path = '/id')
UPDATE api_svc.raid_history
SET diff = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'path' = '/id' THEN
                jsonb_set(
                    elem,
                    '{value,raidAgencyUrl}',
                    to_jsonb(
                        'https://static.prod.raid.org.au/raids/' ||
                        -- Extract everything after the scheme and domain
                        regexp_replace(elem->'value'->>'identifier', '^https?://[^/]+/', '')
                    )
                )
            ELSE elem
        END
    )
    FROM jsonb_array_elements(diff::jsonb) AS elem
)
WHERE diff::jsonb @> '[{"path": "/id"}]'::jsonb
  AND diff::text LIKE '%raid.org.au%';