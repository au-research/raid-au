-- Add service point 20000005 to match TEST environment.
-- This service point has a non-null group_id, which is required by the
-- ServicePointUpdateRequest bean validation (@NotNull on groupId).
-- Used by ServicePointIntegrationTest update tests.
insert into api_svc.service_point (id, name, identifier_owner, repository_id, prefix, group_id, tech_email, admin_email, enabled, app_writes_enabled)
overriding system value
values (20000005, 'RAiD AU Test Registry 2', 'https://ror.org/038sjwq14', 'ATHH.KFOHZB', '10.83483', 'ba0b01a6-726f-464f-b501-454a10096826', 'raid.services@ardc.edu.au', 'raid.services@ardc.edu.au', true, true)
on conflict (id) do nothing;
