### Validate the static-site JSON-LD output against the LinkML-derived schema in CI (validation-only, not generated types)

* Status: final
* Who: proposed by Claude, decided by RL
* When: 2026-08-06
* Related: RAID-782 (parent RAID-574), follow-on from RAID-759 Phase 3a, depends on RAID-778 (LinkML schema) and RAID-779 (generated artefacts)

# Context

The static site's landing-page JSON-LD is produced by a hand-maintained builder,
`raid-agency-app-static/src/utils/json-ld.ts`, whose output is embedded as
`<script type="application/ld+json">` on each raid page and harvested by RDA for
the NESP Domain Data Portal (HELP-2753). The canonical definition of RAiD's
schema.org `ResearchProject` shape lives in the LinkML schema
`api-svc/datamodel/src/v2/researchproject.yaml`, compiled by `gen-json-schema`
to the committed `api-svc/datamodel/generated/v2/researchproject.json`.

Nothing linked the two. `json-ld.ts` could drift from the canonical schema
silently, with no consumer-visible signal until a harvester broke. RAID-759's
Phase 3a asked us to close that gap. It framed the relationship as: `json-ld.ts`
is the only consumer-validated emitter, so it is the *reference output*, and the
LinkML schema is the *authoritative definition that must describe it*.

RAID-782 required a documented choice between two mechanisms (AC#2):

1. **Generate TypeScript types** (and ideally the `@context`) from the LinkML
   schema so `json-ld.ts` stops being hand-maintained and drift becomes a compile
   error.
2. **Validation-only** — add a CI check that validates `json-ld.ts` output against
   the LinkML-derived JSON Schema.

# Decision

Adopt **validation-only**. A Vitest suite
(`raid-agency-app-static/src/utils/json-ld.schema.test.ts`) runs
`buildResearchProjectJsonLd()` over representative fixtures and validates the
output against `researchproject.json` with `ajv` (draft 2019-09) against the
`#/$defs/ResearchProject` subschema (`additionalProperties: false`). It runs in
the static site's existing `npm test` step
(`.github/workflows/raid-agency-app-static.yml`), so any PR that makes the
emitter diverge from the canonical schema fails CI.

The schema is read at runtime from the datamodel module via a relative path,
keeping it out of the TypeScript type graph and `astro check`; CI checks out the
whole monorepo so the path resolves there.

# Alternatives considered

* **Generate TypeScript types from LinkML.** Rejected for now. The static site
  has no codegen toolchain (its deps are Astro/Vitest/Tailwind only), and the
  LinkML generators require Docker, which the static-site CI job does not have.
  Wiring a LinkML-to-TypeScript generator into the site's build is a large,
  standalone piece of infrastructure disproportionate to a low-priority ticket,
  and it would still not validate the *values* the emitter produces — only its
  static shape. Validation-only gives the drift guarantee the ticket asks for at
  a fraction of the cost, and RAID-759 explicitly names it as an acceptable
  minimum. Generated types remain a future option if `json-ld.ts` grows.

* **Validate loosely (allow additional properties).** Rejected. It would not
  catch the divergences that matter (renamed or stray fields), undercutting the
  point of the check. We validate against the strict `ResearchProject` subschema
  and the suite includes a negative-control test proving `additionalProperties:
  false` is actually enforced.

# Consequences

* Two pre-existing emitter/schema drifts, invisible until the check existed, were
  found and fixed as part of this work:
  * **`CreativeWork.additionalType`** — the schema models it as multivalued
    (array), but `json-ld.ts` emitted a bare scalar for a single category.
    Resolved in the emitter: `additionalType` is now always emitted as an array
    (omitted when there is no category). Harvester-safe — JSON-LD expansion
    treats a single-element array and a scalar identically.
  * **`Member.name`** — RAID-794 added the resolved ROR organisation name onto
    organization member/funder nodes, but those validate against the `Member`
    class, which had no `name` and `additionalProperties: false`. Resolved in the
    schema: an optional `name` was added to the `Member` class in
    `researchproject.yaml`, and `researchproject.json` plus the two committed
    OpenAPI specs were regenerated.
* The check couples the static-site test to the committed `researchproject.json`.
  Because the datamodel is regenerated only locally (Docker/LinkML) and CI relies
  on the committed JSON, any future schema change must commit the regenerated
  `researchproject.json` or the check validates against a stale schema. This is
  already the datamodel's operating model.
* This addresses the "or at minimum" half of RAID-759 Phase 3a. Generating types
  and/or the `@context` for the emitter remains available as later work if the
  hand-maintained builder becomes harder to keep correct by hand.
