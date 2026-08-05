### Use http://schema.org/ as the `schema` prefix in researchproject.yaml so the JSON-LD context can be generated

* Status: final
* Who: proposed by Claude, decided by RL
* When: 2026-08-05
* Related: RAID-779 (parent RAID-574), depends on RAID-778, supersedes reference in RAID-758

# Context

RAID-779 wires the LinkML `gen-jsonld-context` generator to run against
`api-svc/datamodel/src/v2/researchproject.yaml` (the schema.org `ResearchProject`
mapping) so a JSON-LD `@context` is produced as a committed build artefact
(`generated/v2/researchproject-context.jsonld`).

`researchproject.yaml`, as delivered by RAID-778, declared the `schema` prefix as
`https://schema.org/`. This is consistent with the rest of the RAiD codebase: the
static-site landing-page JSON-LD (`raid-agency-app-static/src/utils/json-ld.ts`)
emits `"@context": "https://schema.org"`, and RAID-758's hand-written reference
page documents `https://schema.org`.

The `gen-jsonld-context` generator refuses to run on this schema. It merges the
prefix map from the imported `linkml:types`, which hardcodes
`schema: http://schema.org/`, and raises `ValueError: Prefix: schema mismatch
between raid-schema-org and types` when the source schema declares a different
value for the same prefix. The `--no-mergeimports` flag does not avoid this; the
types prefix map is loaded regardless. The LinkML image is pinned to
`linkml/linkml:1.9.2`.

`gen-json-schema` (the RAID-778 generator that produces the committed
`researchproject.json`) does not have this constraint, which is why the mismatch
was never surfaced before.

# Decision

Change the `schema` prefix in `researchproject.yaml` from `https://schema.org/`
to `http://schema.org/` so it matches `linkml:types` and the JSON-LD context
generator runs.

Consequences of the change were verified:

* `researchproject.json` (RAID-778's committed artefact) is **byte-identical**
  after regeneration. JSON Schema output carries no schema.org URIs, so the
  prefix value does not affect it.
* The generated `@context` maps every slot and class to `schema:` /
  `http://schema.org/` terms as expected.
* The static-site landing-page JSON-LD (`json-ld.ts`) is unaffected and stays on
  `https://schema.org`.

`http://schema.org/` and `https://schema.org/` are treated as equivalent by
schema.org itself (it serves both) and by search engines (Google normalises
them), so consumers of the generated `@context` and of the landing-page JSON-LD
resolve terms to the same vocabulary in practice.

# Alternatives considered

* **Keep `https://schema.org/` and rewrite the generated `@context` back to https
  at build time** (generate from a temp http-flipped copy of the schema, then
  `sed` the output). Rejected as over-engineered for the benefit: it adds a
  fragile, multi-step Gradle task and leaves the source schema and the emitted
  artefact disagreeing on the prefix value, which is more confusing for
  maintainers than a single consistent `http` source.
* **Drop the `linkml:types` import to avoid the clash.** Rejected: the import
  supplies the standard type ranges (`string`, `date`, `uri`, ...) that the
  schema and `researchproject.json` generation depend on.
* **Upgrade LinkML past 1.9.2** in the hope the strict prefix check is relaxed.
  Rejected as out of scope for a low-priority docs-generation ticket; the image
  is pinned deliberately for reproducibility.

# Consequences

* The generated `@context` uses `http://schema.org/` while the landing-page
  JSON-LD uses `https://schema.org`. This is a deliberate, documented divergence,
  not a defect. Both are valid and equivalent for schema.org consumers.
* If schema.org / consumer behaviour ever requires the generated `@context` on
  `https`, revisit this decision (the rewrite-at-build alternative above becomes
  the fallback).
* The datamodel `readme.md` "Others" section, which previously stated
  `generateJSONLDContextV2` fails, has been corrected: it now succeeds against
  `researchproject.yaml`. Only `generateSHACLV2` (still targeting `raid-core.yaml`
  with dynamic enumerations) fails.
