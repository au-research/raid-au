select count(1) from raid
;

select * 
from raid
where handle = 'inmem/50d8b4a920230419032534212'
-- inmem/50d8b4a9 20230419 0325 34 212
;

-- select * 
select count(1)
from raid
where 
  date_created >= '2021-07-1' and
  date_created <  '2022-07-01'
;

SELECT indexname FROM pg_indexes
WHERE tablename = 'app_user' AND schemaname = 'api_svc' AND indexdef LIKE '%UNIQUE%'
;


select * 
from app_user
where 
  email = 'shorn.tolley@ardc.edu.au' and
  id_provider = 'GOOGLE'
order by date_created desc
;

select *
from api_svc.app_user
where (api_svc.app_user.email = 'shorn.tolley@ardc.edu.au' and
       api_svc.app_user.client_id =
       '112489799301-m39l17uigum61l64uakb32vjhujuuk73.apps.googleusercontent.com' and
       api_svc.app_user.subject = '114620854542932400092' and
       api_svc.app_user.enabled = true) 
;

select * 
from raid
order by date_created desc
;

select version()
;

DO $$ BEGIN     IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'api_user')     THEN CREATE ROLE api_user NOSUPERUSER NOCREATEDB NOCREATEROLE LOGIN;     END IF;     END $$
;

SELECT * FROM information_schema.tables
WHERE table_schema = 'pg_catalog'
;

