# RAID-1007: Importing put codes into the ORCID updater

- JIRA: [RAID-1007](https://ardc.atlassian.net/browse/RAID-1007), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-task: [RAID-1006](https://ardc.atlassian.net/browse/RAID-1006) (PR [#750](https://github.com/au-research/raid-au/pull/750))
- Related: [RAID-978](https://ardc.atlassian.net/browse/RAID-978) exports the file and runs the import
- PR: not yet opened (will target `feature/RAID-975`)
- Status: built and tested locally

## What changed and why

RAiD AU keeps its existing ORCID client (RAID-975 Q1, Decision 2026-10-23).
The central updater has already written membership entries to researchers'
ORCID records with it. Without their put codes, RAiD AU's updater would
create a second entry for each one.

- `POST /admin/put-code-imports` takes a CSV file (`text/csv`) with the
  header `handle,orcidId,putCode`. It needs the realm role
  `orcid-updater-admin`, like the deletion endpoint.
- The iD can be bare, as the central updater keeps it, or a URL. It must be
  an iD on the ORCID the updater is configured for, so a sandbox export
  can't be imported into production.
- Each row is imported in its own transaction under the iD's lock, so it
  can't race a write for the same researcher, and a large file never holds
  many locks.
- An entry that already has a put code is left alone and counted as
  skipped, so the import can be repeated. An entry without one (the
  updater's own write was never accepted) takes the imported put code.
- Invalid rows are reported by line number, without repeating the row, and
  the rest are imported. A file that is empty or has no header row gives
  400 `INVALID_IMPORT_FILE` and imports nothing.
- Nothing is written to ORCID by the import. When the researcher
  authenticates, the updater updates the imported entry. Access tokens are
  never imported.
- Only the counts are logged; the rows name researchers.

### The central updater's data

Checked in raid-aws-private (`handler/orcid-writer`): the `ContributorRaid`
DynamoDB table holds an item per contribution with `PK` =
`CONTRIBUTOR#<bare iD>`, `SK` = `RAID#<handle>` and `location`, the ORCID
URL of the entry, whose last path segment is the put code. The RAID-978
export maps those to `orcidId`, `handle` and `putCode`.

## Testing

- Unit tests: 247 pass. New: `PutCodeImportServiceTest` (7) and
  `PutCodeImportControllerTest` (4); `SecurityConfigTest` now reaches the
  import endpoint.
- Integration tests (Postgres): 54 run, 1 skipped (the manual ORCID sandbox
  test), 0 failures. `PutCodeImportIntTest` (3) covers importing, keeping
  put codes already held, filling a failed entry's, running again, the
  writer updating an imported entry instead of creating one, and a file
  without a header.
- Built with `--release 17`.
- The contract validates with `openApiValidate`.

## Documentation

- `orcid-updater/idl/orcid-updater-openapi-3.1.yaml`: the file format, the
  rules above, the 400 and 415 responses, and an example.
- `orcid-updater/README.md`: a section on importing put codes.
- `doc/adr/2026-10-23_per-ra-orcid-updater.md`: the file and where the
  central updater keeps put codes.
