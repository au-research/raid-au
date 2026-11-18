# RAID-1064: Reset contributors and update existing RAiDs and DOIs in step

- JIRA: [RAID-1064](https://ardc.atlassian.net/browse/RAID-1064), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976)
- Branch: `feature/RAID-1064` into `feature/RAID-976`
- PR: [#795](https://github.com/au-research/raid-au/pull/795)
- Status: in review

## What changed and why

After [RAID-1057](https://ardc.atlassian.net/browse/RAID-1057), a DOI lists an
ORCID contributor as a creator only while they are AUTHENTICATED, and
`PUT /contributor/status` keeps each contributor's current status and the
status recorded in every RAiD naming them in step.

Two things were still out of step:

- [RAID-1058](https://ardc.atlassian.net/browse/RAID-1058)'s V50 reset
  authenticated contributors with a SQL update. That changed the contributor's
  current status without saving their RAiDs, so each RAiD still recorded
  AUTHENTICATED and its DOI still listed them.
- Existing DOIs were built under the old rule and list every contributor.

Decisions (Rob, 2026-11-18): any change to a RAiD increments its version, and a
migration that leaves the data inconsistent is no good.

- **V50 changed to record only.** It lists every AUTHENTICATED ORCID contributor
  in `contributor_status_reset` (with `recorded_at`, and a `reset_at` the API
  sets) and changes no status. V50 had not reached test, stage, demo or prod.
- **Migration V51** adds `raid.contributor_status_refresh_required` and flags
  every RAiD naming an ORCID contributor (`https://orcid.org/` or
  `https://sandbox.orcid.org/`).
- **`ContributorStatusRefreshWorker`**, a one-off catch-up in two stages, one
  instance at a time (Postgres advisory lock):
  1. **Reset.** Each recorded contributor is set to UNAUTHENTICATED through
     `ContributorStatusService`, the same path as a status change from the
     ORCID updater, so every RAiD naming them gets a new version and a
     DataCite update in the same step. A contributor the updater has since
     given another status keeps it. A reset that fails is retried.
  2. **DOIs**, once every reset is done. Each flagged RAiD gets a new version
     if the status it records differs from the contributor's current status,
     and otherwise a DataCite update alone, so its DOI follows the creator
     rule. A failed DataCite update goes to the DataCite resync worker.
- **On by default** (`raid.contributor-status-refresh.enabled: true`), so every
  agency is brought up to date without configuration. It updates DataCite
  itself rather than relying on the DataCite resync worker, which is off by
  default for other agencies. Once both stages are done, each pass is a no-op.
- The repositories read the new table and column by name rather than through
  regenerated JOOQ classes.
- **Docs**: `doc/reference/deployment-configuration.md`.

The ticket asked to check that the DataCite resync worker is on in every
environment before the release. It is on in every ARDC environment, including
branch APIs (`raid.datacite.resync.enabled: true` in the raido-v2-aws-private
CDK configuration).

## Testing

- Unit tests: `ContributorStatusRefreshWorkerTest` (reset, partial reset
  finished, updater status kept, failed reset retried, DOIs wait for resets,
  new version, DataCite only, DataCite failure, retry, missing RAiD, lock, tick
  never throws), `ContributorStatusResetRepositoryTest` and
  `ContributorStatusRefreshRepositoryTest` (the SQL names the right table and
  columns).
- Testcontainers: `ContributorStatusResetMigrationTest` (V50 records the right
  contributors and changes no status) and
  `ContributorStatusRefreshMigrationTest` (V51 flags the right RAiDs).
- Full integration suite against the local stack with V50 and V51 applied and
  the worker running at the same time: 276 run, 0 failures, 16 skipped
  (existing). Locally the worker reset the 1 recorded contributor first, then
  updated the 414 flagged RAiDs. Afterwards, none of the 524 ORCID contributor
  entries in RAiD metadata disagreed with the contributor's current status,
  and no RAiD was left for the DataCite resync worker.

## Release notes

- The `feature/RAID-976` branch environment applied the earlier V50, so its
  Flyway checksum no longer matches. That schema needs a reset or a Flyway
  repair before the branch redeploys.
- With 50 RAiDs per pass, a 1-second pause between them and a minute between
  passes, the worker processes roughly 1,500 RAiDs an hour.
