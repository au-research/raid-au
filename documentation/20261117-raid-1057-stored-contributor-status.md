# RAID-1057: Take contributor ORCID status from the API's stored value

- JIRA: [RAID-1057](https://ardc.atlassian.net/browse/RAID-1057), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976)
- Branch: `feature/RAID-1057` into `feature/RAID-976`
- PR: [#793](https://github.com/au-research/raid-au/pull/793)
- Status: in review

## What changed and why

The API no longer asks the central ORCID integration for a contributor's
status. Each registration agency's ORCID updater sets status in the API, and
the API keeps it.

- **New endpoint `PUT /contributor/status`** (`contributor-writer` role only).
  It sets the stored status on the shared contributor row, then brings every
  RAiD naming the contributor up to date. A RAiD whose status differs gets a
  new version and a DataCite update. Each RAiD has its own transaction, so one
  failure doesn't hold back the others. A DataCite failure keeps the new
  version and flags the RAiD for the DataCite resync worker. Any other failure
  makes the call return 500, so the updater retries. Repeating the call
  changes nothing. Responses: 204, 400 (unknown status or missing field), 404
  (no contributor with this id).
- **Mint, update and patch use the stored status** and ignore a status sent by
  the client. A contributor the API hasn't seen is UNAUTHENTICATED. On mint,
  the status is now applied before DataCite is called. Saving a RAiD no
  longer writes status to the contributor row.
- **DataCite creators** (Rob's direction, 2026-11-16): a DOI lists an ORCID
  contributor only while they are AUTHENTICATED. ISNI contributors always
  appear. When none qualify, DataCite gets the single empty creator it has
  received since March 2025 for that case. The rule applies whether or not
  the ORCID integration is enabled, because every agency will run an updater.
- **Removed the central lookup**: `OrcidIntegrationClient.findByOrcid`,
  `ContributorLookupResponse`, the `raid.orcid-integration.contributor-id-lookup`
  property and its MockServer expectations.
- **Updater contract** (`orcid-updater-openapi-3.1.yaml`) corrected: the call
  now creates RAiD versions and updates DataCite.
- **Docs**: `doc/architecture/orcid-updater-communication.md` and
  `doc/reference/deployment-configuration.md`.

The endpoint is defined in `template-raid-openapi-3.1.yaml` (the generator's
source, assembled into the two 3.1 specs) and mirrored in
`raido-openapi-3.0.yaml`. CLAUDE.md names only the 3.0 file, but the
generator doesn't read it.

## Testing

- Unit tests: 1,106 run, 0 failures. New or changed: `ContributorStatusServiceTest`,
  `ContributorControllerTest`, `ContributorServiceTest`, `RaidServiceTest`
  (refresh, and status before DataCite on mint and update),
  `DataciteAttributesDtoFactoryTest` (creator rule for each status and schema).
- Integration tests: 273 run, 16 skipped (existing), 0 failures, against the
  local stack. New `ContributorStatusIntegrationTest` covers versioning,
  idempotence, update ignoring client status, DOI creators following status
  (through MockServer's recorded DataCite requests), 404, 403 for a
  service-point-user and 400. It replaces the test disabled in RAID-1056.

## Follow-ups

- Existing DOIs still list every contributor until their RAiD is next saved.
  [RAID-1064](https://ardc.atlassian.net/browse/RAID-1064) updates them after the RAID-1058 reset.
- The status tests use sandbox iD `0009-0005-9091-4416` and take it off their
  RAiDs afterwards, so a status change touches only the test's RAiD. The
  existing `removeContributor` test also mints RAiDs with it, then removes it.
