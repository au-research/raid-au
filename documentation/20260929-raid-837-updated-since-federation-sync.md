# RAID-837: Add `updatedSince` incremental sync filter to `/raid/all-public` and `/raid/all-embargoed`

- Ticket: [RAID-837](https://ardc.atlassian.net/browse/RAID-837) (parent story, epic RAID-739)
- Sub-tasks:
  - [RAID-897](https://ardc.atlassian.net/browse/RAID-897) — OpenAPI spec
  - [RAID-898](https://ardc.atlassian.net/browse/RAID-898) — Flyway index
  - [RAID-899](https://ardc.atlassian.net/browse/RAID-899) — repository/service/controller filtering
  - [RAID-900](https://ardc.atlassian.net/browse/RAID-900) — malformed-parameter 400 handling
  - [RAID-901](https://ardc.atlassian.net/browse/RAID-901) — local-dev realm role for intTest
  - [RAID-902](https://ardc.atlassian.net/browse/RAID-902) — integration tests
  - [RAID-903](https://ardc.atlassian.net/browse/RAID-903) — this sub-task (ADR + completion doc)
- ADR: `doc/adr/2026-09-29_updated-since-filter-on-materialised-metadata.md`
- PRs:
  - https://github.com/au-research/raid-au/pull/678 (RAID-897)
  - https://github.com/au-research/raid-au/pull/680 (RAID-898)
  - https://github.com/au-research/raid-au/pull/681 (RAID-899)
  - https://github.com/au-research/raid-au/pull/682 (RAID-900)
  - https://github.com/au-research/raid-au/pull/683 (RAID-901)
  - https://github.com/au-research/raid-au/pull/684 (RAID-902)
  - https://github.com/au-research/raid-au/pull/685 (RAID-903)

## What changed and why

Federation API consumers previously had to re-fetch the entire
`/raid/all-public` or `/raid/all-embargoed` corpus on every sync, with no
way to ask for only what changed since their last call. RAID-837 adds an
`updatedSince` ISO 8601 query parameter (requiring a timezone offset,
`OffsetDateTime`) to both endpoints, so a consumer can pass the cursor
from its last sync and receive only RAiDs updated after it. The value is
filtered on the RAiD's existing `metadata.updated` field (materialised
inside the JSONB metadata blob as Unix epoch seconds, the same value
already returned in the response body) rather than a new database column,
backed by a matching Postgres expression index. Filtering the fan-out join
that both endpoints used to compute "latest revision per raid" was also
simplified in the same body of work, since it was fetching every
`raid_history.diff` CLOB per revision only to discard nearly all of it.
Full design rationale, alternatives considered (a new `raid.updated`
column, `raid_history.created`), and known limitations (one-second
granularity, the index/predicate textual-coupling risk) are in the ADR.

## This sub-task (RAID-903)

Documentation only — no implementation code was changed. This sub-task
adds:

- The ADR recording the design decisions made across RAID-897–902.
- This completion document.

## Tests

No code changed in this sub-task, so no new tests were added. Test
coverage for the feature itself was added in RAID-902
(`UpdatedSinceIntegrationTest`, 9 cases covering filtering, the
strictly-after boundary, backward compatibility with the parameter
omitted, malformed/offset-less input 400s, and embargo-to-open visibility,
for both endpoints). The full intTest suite (249 tests) was green after
RAID-902.
