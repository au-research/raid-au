# Complete researchproject.yaml LinkML schema to match json-ld.ts

**Ticket:** [RAID-778](https://ardc.atlassian.net/browse/RAID-778) — Complete researchproject.yaml LinkML schema to match RAID-756/757 json-ld.ts output
**Type:** Story · **Parent epic:** [RAID-574](https://ardc.atlassian.net/browse/RAID-574)
**Author:** Rob Leney
**Date:** 2026-07-27
**PR:** [au-research/raid-au#577](https://github.com/au-research/raid-au/pull/577)
**Related:** [RAID-759](https://ardc.atlassian.net/browse/RAID-759) (investigation), [RAID-756](https://ardc.atlassian.net/browse/RAID-756), [RAID-757](https://ardc.atlassian.net/browse/RAID-757), [RAID-784](https://ardc.atlassian.net/browse/RAID-784) (follow-on: organisation names)

---

## 1. What changed and why

Phase 1 of the RAID-759 consolidation recommendation: bring the canonical LinkML schema
`api-svc/datamodel/src/v2/researchproject.yaml` up to what the RAID-756/757-corrected
static-site `json-ld.ts` now emits. The static site is the only consumer-validated
schema.org emitter (harvested by RDA / NESP, HELP-2753), so its output is treated as the
reference specification the schema must describe.

Changes to `researchproject.yaml`:

- **`headline`** slot added (`schema:headline`) — the primary-type title. `name` remains the
  pipe-joined list of all titles.
- **`citation`** slot (`schema:citation`, multivalued) + new **`CreativeWork`** class —
  related objects (research inputs/outputs) as a flat CreativeWork list with a PropertyValue
  identifier (DOI/ARK/ISBN recognised, otherwise a generic URL), an optional formatted
  citation `name`, and `additionalType` for the category (RAID-757).
- **`isPartOf` / `hasPart` / `isBasedOn` / `isRelatedTo`** slots + new
  **`RelatedResearchProject`** class — related RAiDs split by relationship type. Its
  `identifier` is a plain URI string (not a PropertyValue); `isRelatedTo` additionally
  carries `relationshipType` (raw vocab URI) and `relationshipTypeName` (resolved label)
  (RAID-706/756).
- **`parentOrganization`** semantics resolved to the **registration agency** (AC#2). Both
  live emitters already use the registration agency (static site as `parentOrganization`,
  the API `RaidJsonLdConverter` as `publisher`); only the schema *description* said "owner".
  The description is corrected and the resolution documented in the slot.
- **`Organization.name`** `required` flag dropped — the live `parentOrganization` emits only
  the ROR identifier, no name.

Regenerated `researchproject.json` (LinkML `gen-json-schema`) and reassembled the 3.1 OpenAPI
specs, which merge that JSON. Four tracked files changed: the YAML source, the generated
JSON, and the two assembled `raid-openapi-*3.1.yaml` specs.

## 2. Decisions

- **No bare `label` slots on Role/DefinedTerm.** The ticket's Phase 1 text listed "label slots
  to Role/DefinedTerm", but the current `json-ld.ts` emits no `label` field; labels are carried
  by `roleName` (Role), `name` (DefinedTerm) and `relationshipTypeName` (related RAiDs). Adding
  bare `label` slots would introduce a discrepancy that AC#3 ("no structural discrepancies vs
  current json-ld.ts") forbids, so they were deliberately omitted.
- **`parentOrganization` = registration agency** (not owner) — see above. The RAID-778 ticket
  said "two live emitters disagree"; in fact both live emitters use the registration agency, so
  there was no real disagreement to reconcile beyond the stale schema description.
- **`additionalType` modelled as `multivalued`.** The live output emits a scalar for one category
  and a list for several; LinkML cannot cleanly express that scalar-or-list union, so multivalued
  is the closest single representation (documented in the slot).

## 3. AC#3 — by-hand validation against current `json-ld.ts`

| json-ld.ts output | Schema slot/class | Match |
|---|---|---|
| `@context` / `@type` / `@id` | `context` / `type` / `id` | ✓ |
| `name`, `headline` | `name`, `headline` | ✓ |
| `identifier` (RAiD PropertyValue) | `identifier` → `PropertyValue` | ✓ |
| `parentOrganization` `{@type,@id,identifier}` (no name) | `parentOrganization` → `Organization` (name optional) | ✓ |
| `description?`, `foundingDate`, `dissolutionDate?` | same | ✓ |
| `member[]`, `funder[]` (`Role` → Person/Org member) | `member`/`funder` → `ResearchProjectRole` → `Member` | ✓ |
| `knowsAbout[]` (`DefinedTerm`) | `knowsAbout` → `DefinedTerm` | ✓ |
| `citation?[]` (`CreativeWork`, PropertyValue id, name, additionalType) | `citation` → `CreativeWork` | ✓ |
| `isPartOf/hasPart/isBasedOn/isRelatedTo?[]` (string identifier, relationshipType/Name) | 4 slots → `RelatedResearchProject` | ✓ |

No structural discrepancies remain. The one representational nuance is `additionalType`
(scalar-or-list, see §2).

## 4. Verification

- `./gradlew :api-svc:datamodel:generateJSONSchemaV2ResearchProject --rerun-tasks` — regenerated
  `researchproject.json` (the task declares no `inputs`, so a forced rerun is needed after a source
  edit).
- `./gradlew build` — green. OpenAPI spec validation (`validateSpec = true`) accepts the merged
  schema, and unit tests pass. The new `RelatedResearchProject.identifier` (string) vs
  `CreativeWork.identifier` (PropertyValue) resolved to correct per-class definitions with no
  global-slot conflict.

Note on tests: the `datamodel` module has no unit-test harness, and the LinkML YAML is
documentation/source-of-truth compiled manually to committed JSON (CI has no Docker/LinkML). Its
automated gates are LinkML compilation and the OpenAPI spec validation in `./gradlew build`;
structural correctness against the reference output is verified by hand per AC#3 (§3).

## 5. Out of scope / follow-on

Organisation names are not emitted by `json-ld.ts` today (organisations carry only a ROR id;
names are a build-time ROR-API enrichment used only by the UI). Emitting them requires changes to
`json-ld.ts` and `fetch-ror.js`, tracked separately under **[RAID-784](https://ardc.atlassian.net/browse/RAID-784)**.
Once names are emitted, this schema should be updated to describe them.
