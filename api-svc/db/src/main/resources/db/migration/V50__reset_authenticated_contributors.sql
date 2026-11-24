-- Contributors authenticated through the central ORCID integration must authenticate again
-- through their registration agency's own ORCID updater. Record who they are, so the API can
-- reset them.
--
-- This migration changes no status. The contributor status reset worker resets each recorded
-- contributor to UNAUTHENTICATED through the same path as a status change from the updater, so
-- every RAiD naming them gets a new version in the same step, and sets reset_at. The record is kept indefinitely: it lists the researchers whose existing ORCID
-- entries the central integration wrote.
--
-- Only ORCID contributors are recorded, because other schemes such as ISNI don't authenticate.
-- Running this again changes nothing further.

create table if not exists contributor_status_reset
(
    contributor_id  int         primary key references contributor (id),
    pid             varchar     not null,
    schema_uri      varchar     not null,
    previous_status varchar     not null,
    recorded_at     timestamptz not null default now(),
    reset_at        timestamptz
);

insert into contributor_status_reset (contributor_id, pid, schema_uri, previous_status)
select c.id, c.pid, s.uri, c.status
from contributor c
         join contributor_schema s on s.id = c.schema_id
where c.status = 'AUTHENTICATED'
  and s.uri in ('https://orcid.org/', 'https://sandbox.orcid.org/')
on conflict (contributor_id) do nothing;
