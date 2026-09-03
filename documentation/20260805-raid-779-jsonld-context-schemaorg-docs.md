# RAID-779: Generate and publish JSON-LD @context and schema.org reference docs from LinkML

## Summary

Wired the LinkML datamodel generation chain to produce a JSON-LD `@context` and
schema.org reference documentation from the completed `researchproject.yaml`
schema (delivered by RAID-778). Follow-on from the RAID-759 investigation (Phase
2) under epic RAID-574.

## Links

- Story: [RAID-779](https://ardc.atlassian.net/browse/RAID-779)
- Epic: [RAID-574](https://ardc.atlassian.net/browse/RAID-574)
- Depends on: [RAID-778](https://ardc.atlassian.net/browse/RAID-778) (LinkML schema completion)
- Related: [RAID-759](https://ardc.atlassian.net/browse/RAID-759) (investigation), [RAID-758](https://ardc.atlassian.net/browse/RAID-758) (hand-written reference page)
- PR: https://github.com/au-research/raid-au/pull/598
- ADR: `doc/adr/2026-08-05_schema-org-http-prefix-jsonld-context.md`

## What changed and why

### AC1 — JSON-LD `@context` from `researchproject.yaml`

`generateJSONLDContextV2` previously targeted `raid-core.yaml` and, as written,
produced nothing: it declared an output file but never redirected the generator's
stdout to it, and its declared output path was wrong (`api-svc/datamodel/...`
instead of the project-relative `generated/v2/...`). It was also not part of any
aggregate task.

Changes (`api-svc/datamodel/build.gradle`):

- Repointed the task to `researchproject.yaml` (the schema.org `ResearchProject`
  mapping).
- Fixed the task to redirect stdout to `generated/v2/researchproject-context.jsonld`,
  matching the JSON-schema task pattern.
- Added it to `generateAllV2` and to the `clean` task's keep-list.
- `.gitignore`: re-included `api-svc/datamodel/generated/v2/*.jsonld` so the
  committed artefact survives (CI has no Docker/LinkML to regenerate it).

The generator only supports static enumerations, which is why it failed against
`raid-core.yaml` (SPARQL-materialised enums) but succeeds against
`researchproject.yaml` (static enums).

### AC2 — schema.org reference docs

`generateSchemaOrgDocsV2` already targeted `researchproject.yaml` and was already
wired into `generateAllV2`, and the site nav (`.nav.yml`) already had a
`schema.org: schemaorg-docs` entry. Verified it generates cleanly (63 files:
classes, slots, enums, types). No CI publish exists; publishing is the existing
manual `mike deploy --push` → `gh-pages` → docs.raid.org runbook documented in
`api-svc/datamodel/readme.md`. Per decision, this ticket prepares and documents
publishing but does not push to docs.raid.org.

### AC3 — supersede note on RAID-758's page

Added a note to `doc/reference/schema-org-json-ld-mapping.md` flagging it will be
superseded by the generated docs, accurately distinguishing the two sources: that
page documents the landing-page JSON-LD emitted by the static site
(`raid-agency-app-static/src/utils/json-ld.ts`), while the generated docs derive
from the LinkML schema.

### Supporting change — `schema` prefix

`researchproject.yaml`'s `schema` prefix was changed from `https://schema.org/`
to `http://schema.org/`. `gen-jsonld-context` refuses to run unless the prefix
matches the value `linkml:types` supplies (`http://schema.org/`). The change
leaves `researchproject.json` byte-identical (JSON Schema carries no schema.org
URIs). `http` and `https` schema.org are equivalent for consumers. Full rationale
and alternatives are in the ADR.

Also corrected the datamodel `readme.md`, which stated `generateJSONLDContextV2`
fails — no longer true for `researchproject.yaml`.

## Decisions

Two decisions were confirmed with the product owner during the work:

1. **Publish scope:** prep and document the manual `mike` publish; do not push to
   docs.raid.org as part of this ticket.
2. **schema.org URI:** flip the source schema prefix to `http://schema.org/`
   (simplest, standard LinkML, no change to `researchproject.json`), accepting a
   documented `http`/`https` divergence from the landing-page JSON-LD.

## Validation

- `generateJSONLDContextV2` and `generateSchemaOrgDocsV2` run successfully
  (Docker + `linkml/linkml:1.9.2`, `--rerun-tasks`).
- `generateAllV2 --dry-run` confirms both tasks are in the graph with no cycles.
- `researchproject.json` md5 unchanged after regeneration.
- The `datamodel` module has no unit-test harness; its gates are LinkML
  generation success and downstream OpenAPI `validateSpec`. Validated by hand.

## Follow-ups (out of scope)

- Actually publish the schema.org docs and `@context` to docs.raid.org via `mike`
  (manual runbook), when the product owner is ready.
- Retire `doc/reference/schema-org-json-ld-mapping.md` once the generated docs are
  live (per the note added to it).
- The generated `@context` embeds a `generation_date` timestamp, so the committed
  artefact will show a diff on every regeneration. Not addressed here; strip it in
  the Gradle task if the churn becomes a nuisance.
