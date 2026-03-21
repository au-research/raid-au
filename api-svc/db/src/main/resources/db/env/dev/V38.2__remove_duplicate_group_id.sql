-- Remove group_id from service point 20000002 to avoid duplicate conflict
-- with service point 20000005 (both had 'ba0b01a6-726f-464f-b501-454a10096826').
-- The findByGroupId repository method uses fetchOptional which throws
-- TooManyRowsException when multiple rows share the same group_id.
update api_svc.service_point
set group_id = null
where id = 20000002;
