-- First, handle duplicate repository_ids by setting them to NULL (keeping only the first occurrence)
UPDATE api_svc.service_point
SET repository_id = NULL
WHERE id IN (
    SELECT id FROM (
                       SELECT id,
                              ROW_NUMBER() OVER (PARTITION BY repository_id ORDER BY id) as rn
                       FROM api_svc.service_point
                       WHERE repository_id IS NOT NULL
                   ) t
    WHERE rn > 1
);

-- Now add the unique constraint
ALTER TABLE api_svc.service_point
    ADD CONSTRAINT unique_repository_id UNIQUE (repository_id);