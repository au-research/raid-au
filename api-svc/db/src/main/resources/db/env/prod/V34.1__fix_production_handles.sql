UPDATE api_svc.raid_history
SET diff = (
    SELECT jsonb_agg(
                   CASE
                       WHEN elem->>'path' = '/identifier' THEN
                           jsonb_set(
                                   elem,
                                   '{value,id}',
                                   to_jsonb(
                                           CASE
                                               -- If it's http://raid.org, change to https://raid.org
                                               WHEN elem->'value'->>'id' LIKE 'http://raid.org%' THEN
                                                   regexp_replace(elem->'value'->>'id', '^http://raid\.org', 'https://raid.org')
                                               -- If it's http://raid.local, change to https://raid.org
                                               WHEN elem->'value'->>'id' LIKE 'http://raid.local%' THEN
                                                   regexp_replace(elem->'value'->>'id', '^http://raid\.local', 'https://raid.org')
                                               -- If it's http://localhost:8080, change to https://raid.org
                                               WHEN elem->'value'->>'id' LIKE 'http://localhost:8080%' THEN
                                                   regexp_replace(elem->'value'->>'id', '^http://localhost:8080', 'https://raid.org')
                                               -- If it's https://raid.org, keep as is
                                               WHEN elem->'value'->>'id' LIKE 'https://raid.org%' THEN
                                                   elem->'value'->>'id'
                                               -- For any other URLs, keep as is
                                               ELSE elem->'value'->>'id'
                                               END
                                   )
                           )
                       ELSE elem
                       END
           )
    FROM jsonb_array_elements(diff::jsonb) AS elem
)
WHERE diff::jsonb @> '[{"path": "/identifier"}]'::jsonb
  AND (
    diff::text LIKE '%"http://raid.org%' OR
    diff::text LIKE '%"http://raid.local%' OR
    diff::text LIKE '%"http://localhost:8080%'
    );