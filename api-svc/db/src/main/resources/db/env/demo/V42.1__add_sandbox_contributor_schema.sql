-- RAID-737: register the ORCID sandbox contributor schema in non-production environments.
-- Non-production accepts sandbox ORCID contributors
-- (raid.contributor-validation.orcid.schema-uri = https://sandbox.orcid.org/), and
-- ContributorService requires a matching contributor_schema row to persist them.
-- Production and stage intentionally omit this row (they accept only https://orcid.org/).
-- Guarded so a re-seeded/dump-restored database cannot end up with duplicate rows
-- (ContributorSchemaRepository.findByUri uses fetchOptional, which fails on >1 row).
insert into contributor_schema (uri, status)
select 'https://sandbox.orcid.org/', 'active'::schema_status
where not exists (
    select 1 from contributor_schema where uri = 'https://sandbox.orcid.org/'
);
