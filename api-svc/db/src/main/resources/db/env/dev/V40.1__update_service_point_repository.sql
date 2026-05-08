delete from raido.api_svc.service_point
where id = 20000002;

insert into raido.api_svc.service_point (name, identifier_owner, prefix, group_id, tech_email, admin_email, enabled,
                                         app_writes_enabled, password, repository_id)
values ('RAiD AU Test Registry 2', 'https://ror.org/038sjwq14', '10.83483', 'ba0b01a6-726f-464f-b501-454a10096826', '',
        '', true, true, 'vbYzhjr5LL9k6to1WayH2Be1SxwKkRjC6ax+08i5RMA=', 'ATHH.KFOHZB');
