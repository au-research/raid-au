-- Contributors authenticated through the central ORCID integration must authenticate again
-- through their registration agency's own ORCID updater. Keep a record of who they were, then
-- reset them to UNAUTHENTICATED.
--
-- The record is kept indefinitely: it lists the researchers whose existing ORCID entries the
-- central integration wrote. Only ORCID contributors are reset, because other schemes such as
-- ISNI don't authenticate. Running this again changes nothing further.

create table if not exists contributor_status_reset
(
    contributor_id  int         primary key references contributor (id),
    pid             varchar     not null,
    schema_uri      varchar     not null,
    previous_status varchar     not null,
    reset_at        timestamptz not null default now()
);

insert into contributor_status_reset (contributor_id, pid, schema_uri, previous_status)
select c.id, c.pid, s.uri, c.status
from contributor c
         join contributor_schema s on s.id = c.schema_id
where c.status = 'AUTHENTICATED'
  and s.uri in ('https://orcid.org/', 'https://sandbox.orcid.org/')
on conflict (contributor_id) do nothing;

update contributor c
set status = 'UNAUTHENTICATED'
from contributor_schema s
where s.id = c.schema_id
  and c.status = 'AUTHENTICATED'
  and s.uri in ('https://orcid.org/', 'https://sandbox.orcid.org/');
