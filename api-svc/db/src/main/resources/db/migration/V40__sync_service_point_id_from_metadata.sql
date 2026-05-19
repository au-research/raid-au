UPDATE api_svc.raid
SET service_point_id = (metadata -> 'identifier' -> 'owner' ->> 'servicePoint')::bigint
WHERE metadata IS NOT NULL
  AND metadata -> 'identifier' -> 'owner' ->> 'servicePoint' IS NOT NULL
  AND service_point_id != (metadata -> 'identifier' -> 'owner' ->> 'servicePoint')::bigint;
