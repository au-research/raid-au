# RAID-981: Emit the related object type in the landing page JSON-LD

- JIRA: [RAID-981](https://ardc.atlassian.net/browse/RAID-981) (story, no sub-tasks)
- Requested by RDA: HELP-3196 and HELP-2753 (ARDC service desk)
- PR: [au-research/raid-au#752](https://github.com/au-research/raid-au/pull/752) (merged 2026-10-28, merge commit `95859cbc`)
- Status: Done (live in production 2026-10-29, RDA notified 2026-10-30)

## What changed and why

Research Data Australia (RDA) harvests the schema.org JSON-LD on RAiD's static
landing pages. Every related object was emitted as a `citation` CreativeWork
with only its category on `additionalType`, so RDA had no way to tell a
dataset from a journal article and filed everything under Related Websites.

`buildResearchProjectJsonLd()` (`raid-agency-app-static/src/utils/json-ld.ts`)
now adds the related object type to `additionalType`, after the category:

```json
"additionalType": [
  "https://vocabulary.raid.org/relatedObject.category.id/190",
  "https://vocabulary.raid.org/relatedObject.type.schema/269",
  "Dataset"
]
```

- Order: category URI(s), then the type URI, then the type label.
- The label comes from the existing vocabulary mapping (`lookupVocabLabel()`,
  RAID-756), which already covers all 28 related object types. The labels match
  the landing page.
- No type: the output is unchanged (category only). An unmapped type URI is
  emitted without a label. No category and no type: `additionalType` is omitted.
- `@type` stays `CreativeWork`, and `@id`, `identifier` and `name` are unchanged,
  so existing harvesters keep working.
- `doc/reference/schema-org-json-ld-mapping.md` describes the new order and adds
  the related object type to the vocabulary labels table.

## Decisions

RDA first suggested putting the type in `@type` instead (for example
`Dataset`). We kept `CreativeWork` with the type on `additionalType` because:

- many RAiD type labels aren't schema.org classes, and some names differ (Journal
  Article is `ScholarlyArticle`)
- several RAiD types (Event, Funding, Prize, Instrument, Service, Physical
  Object) aren't creative works, so they can't sit under `citation` as one
- changing `@type` would change existing output for every consumer

RDA agreed ("do what's best for RAiD"). Mapping to schema.org CreativeWork
subtypes is logged as a follow-up (RAID-1017), and RDA will be told before the
output changes.

## Tests

- `json-ld.test.ts`: type URI and label after the category, each object carries
  only its own type, no type keeps the category only, an unmapped type emits the
  URI only, and type without category.
- `json-ld.schema.test.ts`: output still validates against the LinkML
  `researchproject.json`.
- Static site unit suite: 66 tests, 0 failures. PR CI passed.

## Release and verification

- Live on the production landing pages from the build on 2026-10-29, and still
  correct after later rebuilds on 2026-10-30.
- Checked on https://raid.org/10.71613/86e95be1: all 8 citations keep `@type`
  `CreativeWork` and carry the type URI and label (Dataset, Journal Article,
  Conference Poster, Software).
- Also cherry-picked to `prod-stable` (`1d07918a`).
- RDA was told on HELP-3196 and HELP-2753 on 2026-10-30.

## Follow-ups

All are linked to RAID-981 in JIRA.

- [RAID-1015](https://ardc.atlassian.net/browse/RAID-1015): production static
  site build configuration (details in JIRA)
- [RAID-1016](https://ardc.atlassian.net/browse/RAID-1016): landing page
  rendering fix (details in JIRA)
- [RAID-1017](https://ardc.atlassian.net/browse/RAID-1017): map related object
  types to schema.org CreativeWork subtypes
- [RAID-1018](https://ardc.atlassian.net/browse/RAID-1018): update the LinkML
  `CreativeWork.additionalType` definition, which still describes category ids
  only
