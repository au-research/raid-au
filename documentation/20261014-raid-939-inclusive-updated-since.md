# RAID-939: Make the Member API `updatedSince` boundary inclusive

- JIRA: [RAID-939](https://ardc.atlassian.net/browse/RAID-939) (story, no sub-tasks)
- Related: [RAID-837](https://ardc.atlassian.net/browse/RAID-837) (original filter), [RAID-902](https://ardc.atlassian.net/browse/RAID-902) (regression-guard suite)
- PR: [au-research/raid-au#723](https://github.com/au-research/raid-au/pull/723)
- ADR: [2026-10-14 inclusive boundary](../doc/adr/2026-10-14_updated-since-boundary-is-inclusive.md)

## What changed and why

`GET /raid/all-public` and `GET /raid/all-embargoed` now return RAiDs whose
`metadata.updated` equals the supplied `updatedSince`. Previously they returned
only RAiDs updated strictly after it. `metadata.updated` has one-second
resolution, so the strict boundary could miss a RAiD updated in the same second
as a consumer's last sync. The Federation API's `updatedSince` is already
inclusive, so the two APIs now agree. Matthias made this decision on
2026-10-06, superseding the RAID-837 wording.

- `RaidRepository.findAllPublic` and `findAllEmbargoed`: `gt` changed to `ge`.
  The expression is unchanged, so the V49 index still applies.
- OpenAPI `updatedSince` descriptions: changed in the 3.0 spec, the 3.1
  template, and both 3.1 specs (regenerated from the template).
- New ADR. The RAID-837 ADR is marked superseded in part.
- No schema or data migration.

## Tests

- `RaidRepositoryTest`: two new cases assert the `>=` predicate. Both were
  confirmed to fail against `gt`.
- `UpdatedSinceIntegrationTest`: exact-boundary cases assert inclusion on both
  endpoints. Before-boundary cases assert exclusion of an earlier RAiD. The
  embargo-to-open case now asserts the `metadata.updated` bump directly.
- Full local intTest: 256 tests, 0 failures, 0 errors, 16 skipped.

## Consumer check (ticket task)

The ticket asked to confirm that the Federation ingest overlaps each run and
de-duplicates by identifier. Verified from source on `main`:

- Federation ingest (`raid-aws-private`,
  `handler/federation-ingest/src/source/http-agency-source.ts`) calls
  `/raid/all-public` without `updatedSince` and replaces each agency's
  data on every run.
- Static site build (`raid-agency-app-static/scripts/fetch-raids.js` and
  `fetch-embargoed-raids.js`) also fetches the full sets without
  `updatedSince`.

So neither consumer is affected. Any future incremental consumer must
de-duplicate by handle and version, because a RAiD at the boundary is returned
again on the next sync.

## Release

Ships with the next API release. After release, run a smoke test of
`/raid/all-public?updatedSince=` against one known RAiD's `updated` value
(the RAiD must be included).
