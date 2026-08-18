# RAID-797: Emit related RAiDs with DataCite's native "RAiD" relatedIdentifierType

- **Date:** 2026-08-18
- **JIRA:** [RAID-797](https://ardc.atlassian.net/browse/RAID-797) (parent: [RAID-796](https://ardc.atlassian.net/browse/RAID-796))
- **PR:** [au-research/raid-au#617](https://github.com/au-research/raid-au/pull/617)
- **Commit:** `7c67b8d7`

## What changed and why

Related RAiDs were emitted to DataCite as generic `"DOI"` citations, which made them
indistinguishable from real DOI references in downstream harvesters, aggregators and other
registration agencies. DataCite introduced a dedicated `"RAiD"` `relatedIdentifierType` in
Metadata Schema 4.6/4.7, so we now emit the native type instead.

### Code
- Added `RAID("RAiD")` to `RelatedIdentifierType`
  (`api-svc/raid-api/.../vocabularies/datacite/RelatedIdentifierType.java`). The DataCite string is
  exactly `"RAiD"` (mixed case).
- Changed `DataciteRelatedIdentifierFactory.create(RelatedRaid)` to emit
  `RelatedIdentifierType.RAID` instead of `DOI`. `resourceTypeGeneral` stays `"Project"`, the
  identifier value stays the related RAiD's handle unchanged, and the `RAID_RELATION_TYPE_MAP`
  relation-type mapping is untouched.

### Tests
- Updated the eight `relatedRaid*` cases in `DataciteRelatedIdentifierFactoryTest` to assert the
  `"RAiD"` type, and added a casing-guard test locking `RelatedIdentifierType.RAID.getName()` to
  the exact string `"RAiD"`.
- Added `DataciteLiveRelatedRaidIntegrationTest` (in the `src/test` source set) — a permanent
  regression test that runs the **real** `DataciteRelatedIdentifierFactory` output against the live
  DataCite **test** API, asserts a draft DOI is accepted with `relatedIdentifierType: "RAiD"` /
  `resourceTypeGeneral: "Project"`, then deletes the draft. It lives in `src/test` (not `intTest`)
  to keep the deliberately black-box `intTest` source set free of a `src/main` classpath
  dependency.

## How to run the live DataCite test

Skipped by default. Enable it by setting the env vars (no secrets are committed):

```bash
DATACITE_LIVE_TEST=true \
DATACITE_TEST_REPOSITORY_ID=<repository/client id> \
DATACITE_TEST_PASSWORD=<password> \
DATACITE_TEST_PREFIX=<test DOI prefix, e.g. 10.82841> \
./gradlew :api-svc:raid-api:test --tests '*DataciteLive*'
```

`DATACITE_TEST_ENDPOINT` is optional and defaults to `https://api.test.datacite.org/dois`.

## Backfill of already-minted RAiDs

`scripts/backfill-datacite-related-raids.sh` re-pushes corrected metadata to DataCite for existing
records. It is **targeted** (only RAiDs that have a `relatedRaid` entry are re-posted; others are
skipped) and **idempotent** (each re-post is a full-document PUT via `/raid/post-to-datacite`, so
re-running produces the identical record with no duplicate `relatedIdentifier` entries). It is
rate-limited (0.5s between requests) and logs the posted-vs-skipped counts.

```bash
# environment is one of: local, test, demo, stage, prod
scripts/backfill-datacite-related-raids.sh <environment> <clientId> <clientSecret>
```

The client must hold the `RAID_UPGRADER_ROLE` (the same role the `/raid/non-legacy` and
`/raid/post-to-datacite` endpoints are gated on).

## Verification performed

- `./gradlew :api-svc:raid-api:test` — green (updated factory tests + casing guard).
- `./gradlew :api-svc:raid-api:intTest` — full suite green locally against a branch API on :8080.
- Live DataCite test confirmed to compile and skip when `DATACITE_LIVE_TEST` is unset.
