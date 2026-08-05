# RAID-782: Validate static-site json-ld.ts output against the LinkML-derived schema in CI

## Summary

Added a CI validation check that fails the build if the static site's
hand-maintained JSON-LD builder (`raid-agency-app-static/src/utils/json-ld.ts`)
drifts from the canonical LinkML schema (`researchproject.yaml` ->
`researchproject.json`). Chose validation-only over generated TypeScript types
(recorded in an ADR). Fixed two pre-existing emitter/schema drifts that the new
check surfaced. Follow-on from the RAID-759 investigation (Phase 3a) under epic
RAID-574.

## Links

- Story: [RAID-782](https://ardc.atlassian.net/browse/RAID-782)
- Epic: [RAID-574](https://ardc.atlassian.net/browse/RAID-574)
- Follow-on from: [RAID-759](https://ardc.atlassian.net/browse/RAID-759) (investigation, Phase 3a)
- Depends on: [RAID-778](https://ardc.atlassian.net/browse/RAID-778) (LinkML schema), [RAID-779](https://ardc.atlassian.net/browse/RAID-779) (generated artefacts)
- Related: [RAID-757](https://ardc.atlassian.net/browse/RAID-757) (citation), [RAID-794](https://ardc.atlassian.net/browse/RAID-794) (resolved ROR org name)
- PR: _to be added_
- ADR: `doc/adr/2026-08-06_validate-static-jsonld-against-linkml-schema.md`

## What changed and why

### AC1 — CI fails when json-ld.ts output diverges from the canonical schema

New test `raid-agency-app-static/src/utils/json-ld.schema.test.ts`:

- Reads the committed `api-svc/datamodel/generated/v2/researchproject.json` at
  runtime (relative path across the monorepo; kept out of the TS type graph and
  `astro check`, which already excludes `*.test.ts`).
- Compiles the strict `#/$defs/ResearchProject` subschema
  (`additionalProperties: false`) with `ajv` (draft 2019-09) + `ajv-formats` (for
  the `date` format), and validates `buildResearchProjectJsonLd()` output.
- Fixtures: a minimal RAiD, a **maximal** RAiD exercising every emitted field (so
  every schema class is covered — CreativeWork, RelatedResearchProject, Member
  with a resolved name, DefinedTerm, PropertyValue, Organization), plus targeted
  cases for the two drifts below.
- A **negative-control** test confirms the suite validates against the strict
  subschema (not the permissive schema root), so a real divergence is genuinely
  rejected.

Runs in the existing `npm test` step of
`.github/workflows/raid-agency-app-static.yml` — no workflow change needed.
`ajv` and `ajv-formats` added as devDependencies.

Manually verified the guard bites by temporarily injecting a stray field into
the emitter output (4 schema tests failed), then reverting.

### AC2 — documented choice: validation-only vs generated types

Recorded in `doc/adr/2026-08-06_validate-static-jsonld-against-linkml-schema.md`.
Chose validation-only: the static site has no codegen toolchain and the LinkML
generators need Docker (absent from the static-site CI job), whereas validation
reuses the existing Vitest/`npm test` step and gives the drift guarantee the
ticket asks for. RAID-759 names validation-only as an acceptable minimum.

### Two drifts found and fixed

The check was inert until the emitter and schema agreed. Two pre-existing
divergences were surfaced and resolved:

1. **`CreativeWork.additionalType`** (emitter fix). The schema models it as
   multivalued (array); `json-ld.ts` emitted a bare scalar for a single category.
   `buildRelatedObjectCitations` now always emits `additionalType` as an array
   (omitted when there is no category). Harvester-safe: JSON-LD expansion treats a
   single-element array and a scalar identically. Two assertions in
   `json-ld.test.ts` updated accordingly.

2. **`Member.name`** (schema fix). RAID-794 added the resolved ROR organisation
   name onto organization member/funder nodes, but those validate against the
   `Member` class, which had no `name` and `additionalProperties: false`. Added an
   optional `name` to the `Member` class in `researchproject.yaml`.

### Regenerated derived artefacts

After the `Member.name` schema change, regenerated locally (Docker/LinkML,
`--rerun-tasks`) and committed:

- `api-svc/datamodel/generated/v2/researchproject.json`
- `api-svc/idl-raid-v2/src/raid-openapi-3.1.yaml`
- `api-svc/idl-raid-v2/src/raid-openapi-strict-3.1.yaml`

Each diff is limited to the `Member.name` addition. CI has no Docker/LinkML, so
the committed JSON is authoritative; regenerated Java models (gitignored) are
unused dead code. Verified `:api-svc:idl-raid-v2:openApiGenerate` +
`compileJava` still succeed.

## Ticketing note

The `Member.name` schema gap was strictly a coordination miss between the RAID-778
schema completion and the later RAID-794 emitter change — exactly the class of
drift this CI check now prevents. It was fixed here under RAID-782 (referencing
RAID-794) rather than reopening a separate ticket, because the validation check is
meaningless until the schema and emitter agree.

## Verification

- `raid-agency-app-static`: `npm test` -> 49 tests pass (existing suite + new
  schema suite); `npx astro check` -> 0 errors; `npx astro build` completes.
- `./gradlew :api-svc:idl-raid-v2:compileJava` -> BUILD SUCCESSFUL (specs valid,
  Java generation compiles).
- Negative check: stray field injected into emitter output -> schema tests fail
  as expected; reverted.
