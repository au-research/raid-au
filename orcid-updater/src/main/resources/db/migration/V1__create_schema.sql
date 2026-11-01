-- RAID-1001: baseline for the ORCID updater's own schema. Flyway creates the orcid_updater schema
-- on first start. Tables arrive with the sub-tasks that use them (RAID-1002 onwards).
COMMENT ON SCHEMA orcid_updater IS 'RAiD ORCID updater (RAID-975). Holds researcher tokens: never grant the RAiD API login access.';
