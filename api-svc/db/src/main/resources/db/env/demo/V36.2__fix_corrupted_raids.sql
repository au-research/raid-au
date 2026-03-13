UPDATE api_svc.raid_history
SET diff = (
    SELECT jsonb_agg(elem)
    FROM jsonb_array_elements(diff::jsonb) AS elem
    WHERE jsonb_typeof(elem) != 'null'  -- Filter out JSON null, not SQL NULL
)::jsonb
WHERE jsonb_typeof(diff::jsonb) = 'array';