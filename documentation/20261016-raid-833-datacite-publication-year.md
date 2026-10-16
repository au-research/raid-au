# RAID-833: Derive DataCite `publicationYear` from the RAiD start date

- JIRA: [RAID-833](https://ardc.atlassian.net/browse/RAID-833) (bug, no sub-tasks), epic [RAID-796](https://ardc.atlassian.net/browse/RAID-796)
- Duplicate closed: [RAID-858](https://ardc.atlassian.net/browse/RAID-858)
- Source: [RAID-831](https://ardc.atlassian.net/browse/RAID-831) crosswalk spike
- PR: [au-research/raid-au#726](https://github.com/au-research/raid-au/pull/726)

## What changed and why

`DataciteAttributesDtoFactory` set the DataCite `publicationYear` to
`Year.now()` in all three overloads (`RaidCreateRequest`, `RaidUpdateRequest`,
`RaidDto`). Any write in a later calendar year rewrote the DOI's publication
year, so DataCite metadata drifted away from the activity it describes.

`publicationYear` now comes from the year of `date.startDate`, parsed with the
existing `DateUtil.parseDate`. This accepts `yyyy`, `yyyy-MM` and `yyyy-MM-dd`,
the same formats the date validator accepts. Mint, update and post-to-datacite
all behave the same way.

- `doc/reference/raid-datacite-crosswalk.md`: the mapping table and the
  "Known issues" entry now describe the fixed behaviour.
- No schema or data migration.

Existing DOIs keep their drifted year until their RAiD is next updated or
re-posted to DataCite. At that point the year corrects to the start-date year.

## Tests

- `DataciteAttributesDtoFactoryTest`: nine new parameterised cases cover the
  three overloads with the three start-date formats. The start year is 2019,
  so the cases fail against `Year.now()`.
- Full `raid-api` unit suite: green.
- Full local intTest: 256 tests, 0 failures, 16 skipped.
- Branch-test pipeline on `f7b30729`: every stage passed, including
  `Api-Integration-Test` and `E2e-Test`.

## Verification in demo (2026-10-16)

Demo was running the merge commit `c8c8551d`. Checked against the DataCite
test sandbox (`api.test.datacite.org`):

- Before: demo DOIs with a 2011 start date showed `publicationYear` 2025 or
  2026.
- Minted `10.83483/02390eb2` (open access, start date `2019-01-01`, titled as
  a test record): `publicationYear` 2019.
- Updated the same RAiD (title change, version 2): `publicationYear` stayed
  2019 and the new title reached DataCite, confirming the update path.

An unchanged PUT does not create a new version, so it does not re-post to
DataCite. A real change is needed to exercise the update path.

## Release

Ships with the next API release. After release, check one updated production
RAiD's DOI on `api.datacite.org` and confirm its `publicationYear` matches the
start-date year.
