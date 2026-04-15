insert into api_svc.related_object_schema (uri)
select 'https://web.archive.org/'
    where not exists (
    select 1 from api_svc.related_object_schema where uri = 'https://web.archive.org/'
);