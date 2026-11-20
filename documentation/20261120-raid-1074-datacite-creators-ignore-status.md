# RAID-1074: Send every contributor to DataCite whatever their status

- JIRA: [RAID-1074](https://ardc.atlassian.net/browse/RAID-1074), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976)
- Branch: `feature/RAID-1074` into `feature/RAID-976`
- PR: not yet opened
- Status: in progress

## What changed and why

Rob's direction (2026-11-19): a contributor's status has no effect on what the
API sends to DataCite. All contributors are sent, whatever their status. This
reverses the creator rule from
[RAID-1057](https://ardc.atlassian.net/browse/RAID-1057), and matches main and
production.

Because status is no longer part of the DOI, a status change doesn't need a
DataCite call (Rob, 2026-11-20). The ORCID updater's `PUT /contributor/status`
already gives every RAiD naming the contributor the new status, so the
catch-up stage from [RAID-1064](https://ardc.atlassian.net/browse/RAID-1064)
that brought DOIs in line is no longer needed.

- **DataCite creators**: `DataciteAttributesDtoFactory` lists every
  contributor as a creator. When there are none, DataCite still gets the single
  empty creator it requires.
- **Status change**: `RaidService.refreshContributorStatus` saves the new
  version, updates the ingest copy and the materialised metadata. It no longer
  calls DataCite or flags the RAiD for the DataCite resync worker.
- **One-off reset kept, DOI stage removed**: `ContributorStatusRefreshWorker`
  is now `ContributorStatusResetWorker`, which only resets the contributors
  V50 recorded. Properties move from `raid.contributor-status-refresh.*` to
  `raid.contributor-status-reset.*`.
- **Migration V51 removed** with `ContributorStatusRefreshRepository`. V51 had
  not reached any deployed environment.
- **V50's comment** no longer mentions a DataCite update. V50 is unreleased,
  but a local database that already applied it needs a Flyway repair.
- **Docs**: both API specs, the ORCID updater spec,
  `doc/architecture/orcid-updater-communication.md` and
  `doc/reference/deployment-configuration.md`.

## Testing

- Unit tests: `DataciteAttributesDtoFactoryTest` (an ORCID contributor is a
  creator whatever their status, sandbox ORCID and ISNI contributors are
  creators, every contributor appears in create, update and RaidDto payloads),
  `RaidServiceTest` (a status change saves a new version and calls neither
  DataCite nor the resync repository) and `ContributorStatusResetWorkerTest`.
  1,115 run, 0 failures, 18 skipped.
- Integration: `ContributorStatusIntegrationTest` checks that a status change
  sends no DataCite request and that the DOI lists the contributor while
  their status is AUTHENTICATION_REVOKED.
- Full integration suite against a freshly created local stack (migrations
  applied up to V50, no V51): 275 run, 0 failures, 16 skipped (existing).

## Environments

- The local Docker stack was dropped and recreated so the edited V50 and the
  removed V51 apply cleanly.
- The `feature/RAID-976` branch environment was torn down with the
  `Branch-Cleanup` CodeBuild project (2026-11-20, succeeded). It applied the
  earlier V50 and V51, so it starts from a fresh schema when it next deploys.
