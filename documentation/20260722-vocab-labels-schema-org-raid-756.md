# RAID-756: Include resolved vocabulary labels in schema.org output

- **JIRA:** [RAID-756](https://ardc.atlassian.net/browse/RAID-756) (Story)
- **PR:** [au-research/raid-au#574](https://github.com/au-research/raid-au/pull/574)
- **Date:** 2026-07-22

## What changed and why

RDA (Melanie Barlow) is mapping RAiD's schema.org output for the NESP Domain
Data Portal and flagged that vocabulary-backed fields exposed only the raw
vocabulary URI, with no resolved label. Consumers had to resolve the URIs
themselves, which RDA described as cumbersome. The RAiD UI already displays
resolved labels; this change brings the harvestable schema.org output in line.

All changes are in `raid-agency-app-static/src/utils/json-ld.ts`. Labels are
resolved **at build time** from the same static mapping files the UI renders
from (`src/mapping/data/general-mapping.json`, `subject-mapping.json`), so no
runtime fetching or new data dependency is introduced.

### Field groups (all four implemented together, per AC4)

| Field group | Change | Label source |
|---|---|---|
| contributor.position | `Role.roleName` now the resolved label (`@id` keeps the URI) | `general-mapping.json` (`key === id`) |
| contributor.role (CRediT) | `roleName` derived from the URI slug via `kebabToTitle` | mirrors `contributor-roles.astro`; not present in general-mapping.json |
| organisation.role | `Role.roleName` now the resolved label | `general-mapping.json` |
| subject (ANZSRC FoR) | `knowsAbout` DefinedTerm gains a `name` property | `subject-mapping.json` (`definition === id`) |
| relatedRaid.type (isRelatedTo) | adds `relationshipTypeName` alongside the existing `relationshipType` URI | `general-mapping.json` |

### Design decisions

- **Roles:** `@id` keeps the vocabulary URI; `roleName` carries the resolved
  label (schema.org `roleName` accepts text). No information is lost.
- **relatedRaid.type:** used a non-breaking sibling field
  (`relationshipTypeName`) rather than restructuring `relationshipType` into a
  nested object, so any existing consumer reading it as a string keeps working.
- **Graceful degradation:** `roleName` falls back to the URI when a term is
  unmapped; subject `name` and `relationshipTypeName` are omitted when unmapped
  (no redundant URI duplication).

### Out of scope (per ticket)

title.type, description.type, access.type, traditionalKnowledge, and
relatedObject (the last is a separate missing-data concern, RAID-757).

## Testing

- `npx vitest run src/utils/json-ld.test.ts` — 38 passing. Existing assertions
  updated for the new labels; new cases added for multi-word CRediT slug
  derivation, subject-name omission on an unmapped code, `relationshipTypeName`
  omission on an unmapped type, and `roleName` URI fallback.
- `npx tsc --noEmit` — 0 errors.
