# RAID-644: Add name and headline to static page JSON-LD

## What changed and why

The JSON-LD structured data on all 564 static RAiD pages was missing the `name` field, causing structured-data consumers (Google rich results, Schema.org validators, scholarly aggregators) to see unnamed ResearchProject entries.

The root cause was the RAID-619 refactor that extracted inline JSON-LD into `buildResearchProjectJsonLd()` in `raid-agency-app-static/src/utils/json-ld.ts` — the `name` field present in the original inline template was omitted from the new function.

### Fix

- Added `name` and `headline` fields to the `ResearchProjectJsonLd` interface and the builder function
- Maps `raid.title[0].text` (the primary title) into both `schema:name` and `schema:headline`
- Added 2 new unit tests and updated the empty-fields test

### Files changed

- `raid-agency-app-static/src/utils/json-ld.ts`
- `raid-agency-app-static/src/utils/json-ld.test.ts`

## JIRA tickets

- [RAID-644](https://ardc.atlassian.net/browse/RAID-644) (this fix)
- [RAID-638](https://ardc.atlassian.net/browse/RAID-638) (parent)

## Pull request

- [PR #466](https://github.com/au-research/raid-au/pull/466)
