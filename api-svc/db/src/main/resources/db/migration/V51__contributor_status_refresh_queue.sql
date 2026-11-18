-- Flags every RAiD that names an ORCID contributor, so the contributor status refresh worker
-- brings it in step once it has reset the contributors V50 recorded. A DOI now lists an ORCID
-- contributor only while they are AUTHENTICATED, so every such DOI needs updating once.
--
-- For each flagged RAiD, the worker saves a new version and updates DataCite if the status the
-- RAiD records differs from the contributor's current status, and otherwise updates DataCite
-- alone. It then clears the flag.

alter table raid
    add column if not exists contributor_status_refresh_required boolean not null default false;

update raid
set contributor_status_refresh_required = true
where exists (select 1
              from raid_contributor rc
                       join contributor c on c.id = rc.contributor_id
                       join contributor_schema s on s.id = c.schema_id
              where rc.handle = raid.handle
                and s.uri in ('https://orcid.org/', 'https://sandbox.orcid.org/'));
