# RAID-834: Emit the RAiD licence to DataCite as `rightsList`

- JIRA: [RAID-834](https://ardc.atlassian.net/browse/RAID-834) (bug, no sub-tasks)
- Duplicate: [RAID-859](https://ardc.atlassian.net/browse/RAID-859) (closed; its acceptance criteria were copied into RAID-834)
- Source: [RAID-831](https://ardc.atlassian.net/browse/RAID-831) crosswalk spike, `doc/reference/raid-datacite-crosswalk.md` ("Known issues found in the code")
- PR: [au-research/raid-au#729](https://github.com/au-research/raid-au/pull/729)

## What changed and why

RAiD's licence (`identifier.license`) never reached DataCite, so no DOI RAiD
has minted carries rights metadata. `DataciteRightFactory` was injected into
`DataciteAttributesDtoFactory` but never called.

- `DataciteAttributesDtoFactory`: all three overloads (`RaidCreateRequest`,
  `RaidUpdateRequest`, `RaidDto`) now set `rightsList`. This covers mint,
  update, and post-to-DataCite (resync).
- `DataciteRightFactory`: now takes the `Id` and returns a list. The old
  version copied the free-text licence into `rightsUri` as well. The configured
  licence is `Creative Commons CC-0`, which is not a URI. The factory now maps
  CC0 to the canonical SPDX entry:

  ```json
  {
    "rights": "Creative Commons Zero v1.0 Universal",
    "rightsUri": "https://creativecommons.org/publicdomain/zero/1.0/legalcode",
    "rightsIdentifier": "cc0-1.0",
    "rightsIdentifierScheme": "SPDX",
    "schemeUri": "https://spdx.org/licenses/"
  }
  ```

  Any other licence is sent as `rights` text only. A blank licence produces an
  empty list. Rob chose the SPDX form on 2026-10-16.
- `DataciteRight`: adds `rightsIdentifier`, `rightsIdentifierScheme` and
  `schemeUri`, and omits null fields from the JSON.
- `doc/reference/raid-datacite-crosswalk.md`: `rightsList` is now shown as
  emitted, and the known issue is marked fixed.
- No schema, data or API contract change.

Both tickets suspected that the JSON key `rightsUri` was wrong. It is not. The
DataCite REST API uses `rightsUri` (checked against a live record,
`10.5281/zenodo.1003150`), which `DataciteRight` already emitted.

An update sends the licence from the submitted record. If a client leaves the
licence off an update, the stored licence and the DataCite rights both become
empty. This matches how the stored record already behaves, so it was left
unchanged.

## Tests

- `DataciteRightFactoryTest`: CC0 variants (text and URI forms), an
  unrecognised licence, blank and null input, and the serialised JSON keys.
- `DataciteAttributesDtoFactoryTest`: `rightsList` is set in each of the three
  overloads.
- Full `raid-api` unit suite: 1051 tests, 0 failures, 18 skipped.
- Full local intTest: 256 tests, 0 failures, 0 errors, 16 skipped.
- Branch pipeline for `ba71b906` passed (deploy, E2e-Test,
  Api-Integration-Test).
- `DataciteLiveRightsListIntegrationTest`: new gated live test
  (`DATACITE_LIVE_TEST=true`), following the RAID-797 pattern. It has not run.
  No working DataCite credentials are available outside demo and prod.

## Verification still to do

The test and branch environments stub DataCite and make no DataCite calls. A
passing branch pipeline only shows that building the payload does not break
mint or update. DataCite's acceptance of `rightsList` has not been confirmed.

After merge, in demo (which calls `api.test.datacite.org`):

1. Update a public RAiD.
2. Fetch `https://api.test.datacite.org/dois/<doi>` and confirm
   `data.attributes.rightsList` holds the CC0 entry above.

## Release

Ships with the next API release. After release:

1. Confirm a newly minted or updated prod RAiD shows `rightsList` on
   `api.datacite.org`.
2. Re-sync existing DOIs through the RAID-832 mechanism, so records minted
   before this fix get their rights metadata. No bespoke backfill script is
   needed.
