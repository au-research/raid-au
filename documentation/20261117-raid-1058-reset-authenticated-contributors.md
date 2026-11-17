# RAID-1058: Reset authenticated contributor ORCID iDs on release

- JIRA: [RAID-1058](https://ardc.atlassian.net/browse/RAID-1058), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976)
- Branch: `feature/RAID-1058` into `feature/RAID-976`
- PR: to follow
- Status: in review

## What changed and why

ORCID directed us to have researchers authenticated through the central ORCID
integration authenticate again (Matthias, RAID-976, 2026-11-03). From this
release, each registration agency's own ORCID updater sets status.

Migration `V50__reset_authenticated_contributors.sql`:

- Creates `contributor_status_reset` and copies into it every contributor whose
  schema is `https://orcid.org/` or `https://sandbox.orcid.org/` and whose
  status is AUTHENTICATED: contributor id, iD, schema, previous status and
  reset time. The table is kept indefinitely as the list of these researchers.
- Sets those contributors to UNAUTHENTICATED.
- Leaves every other status alone, and leaves ISNI contributors alone, because
  ISNI contributors don't authenticate.
- Running it again changes nothing further: the table is created only if
  missing, an existing record is kept, and no AUTHENTICATED ORCID contributor
  is left to reset. Flyway applies it only once in any case.

The API database holds no ORCID put codes (the central updater holds them), so
the migration can't touch them. Removing the existing ORCID entries needs the
central updater's tokens and belongs to
[RAID-978](https://ardc.atlassian.net/browse/RAID-978).

The application doesn't read the new table, so JOOQ classes weren't
regenerated.

## Testing

- New `ContributorStatusResetMigrationTest` (Testcontainers, Postgres 16):
  migrates to V49, seeds ORCID, sandbox ORCID and ISNI contributors with each
  status, applies V50 and checks the statuses and the retained records. A
  second case runs the SQL again and checks nothing changes, and that Flyway
  doesn't reapply it.
- Full integration suite against the local stack with V50 applied: 275 run,
  0 failures, 16 skipped (existing).

## Follow-ups

- The reset changes the stored status only. Each RAiD's saved metadata and its
  DOI still show these contributors as AUTHENTICATED until the RAiD is next
  saved. [RAID-1064](https://ardc.atlassian.net/browse/RAID-1064) fixes that
  and must ship in the same release.
