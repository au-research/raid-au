-- RAID-737: register the ORCID sandbox contributor schema in the stage environment.
-- Stage now points at the ORCID sandbox (raid.contributor-validation.orcid.schema-uri
-- = https://sandbox.orcid.org/), matching the agency UI, so ContributorService needs a
-- matching contributor_schema row to persist sandbox contributors. Production stays
-- orcid.org-only and omits this row.
-- Guarded so a re-seeded/dump-restored database cannot end up with duplicate rows
-- (ContributorSchemaRepository.findByUri uses fetchOptional, which fails on >1 row).
insert into contributor_schema (uri, status)
select 'https://sandbox.orcid.org/', 'active'::schema_status
where not exists (
    select 1 from contributor_schema where uri = 'https://sandbox.orcid.org/'
);
