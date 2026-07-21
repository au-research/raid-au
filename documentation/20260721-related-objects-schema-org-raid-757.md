# RAID-757: Related objects missing from schema.org output

- **JIRA:** [RAID-757](https://ardc.atlassian.net/browse/RAID-757) (Bug)
- **Related:** HELP-2753 (RAiD Metadata in RDA — NESP Domain Data Portal)
- **PR:** [au-research/raid-au#572](https://github.com/au-research/raid-au/pull/572)
- **Date:** 2026-07-21

## What changed

Related objects (inputs and outputs) that appear on a RAiD landing page were
completely absent from the static site's schema.org JSON-LD. `json-ld.ts`
emitted contributors and organisations (via `member`) and related RAiDs (via
`isPartOf`/`hasPart`/`isBasedOn`/`isRelatedTo`), but had no handling for
`relatedObject` at all.

`buildResearchProjectJsonLd` now emits every related object as a schema.org
`CreativeWork` node in a flat `citation` array on the `ResearchProject`. Each
node carries:

- `@id` — the object's identifier URL.
- `identifier` — a `PropertyValue` whose `propertyID`/`name` are derived from
  the object's `schemaUri`. DOI, ARK and ISBN are typed explicitly (mirroring
  the backend DataCite export in `DataciteRelatedIdentifierFactory`); every
  other scheme falls back to a plain URL identifier.
- `additionalType` — the RAiD category id(s) (Input `.../191`, Output
  `.../190`, Internal process document `.../192`), a scalar when there is one
  and an array when there are several.

## Why

RDA flagged the gap while mapping RAiD records into their metadata pipeline
(HELP-2753). Example: `https://raid.org/10.83062/32801913` shows two outputs on
its landing page but neither appeared in its schema.org output, so downstream
harvesters could not discover a record's outputs.

## Design decision

Related objects are emitted as a single flat `citation` list rather than split
across distinct schema.org properties by category. This was chosen (over a
category-aware mapping such as Output→`subjectOf`) because it matches the flat
landing-page list, is the most reliably harvested schema.org shape, and avoids
a debatable Output→`subjectOf` semantic. `citation` is strictly a `CreativeWork`
property while `ResearchProject` descends from `Organization`; this deviation is
deliberate and consistent with the file's existing loose use of CreativeWork
properties on the project. A code comment records this so it is not "corrected"
later.

## Testing

`npx vitest run src/utils/json-ld.test.ts` — 32 passing (9 new), covering a
single citation, a mixed input/output flat list, DOI/ARK/ISBN typing, the URL
fallback, the multi-category `additionalType` array, no-category omission,
no-id skip, and empty-array omission.

## Verifying after deploy

Re-check `10.83062/32801913` (or an equivalent record with outputs) and confirm
its outputs appear in the `citation` array of the rendered
`application/ld+json` block.

## Notes

- The local fetched dataset (`src/raw-data/raids.json`, 42 records) contains no
  related objects, so verification relies on the unit tests plus a
  post-deploy check against a real record.
- `tsc --noEmit` reports one pre-existing, unrelated error (missing
  build-generated `raw-data/embargoed-raids.json`); the change itself is clean.
