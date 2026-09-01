# RAID-831 PoC: the RAiD→DataCite crosswalk as a `linkml-map` transformation

Can the RAiD→DataCite crosswalk be *configured* in LinkML rather than hand-written
in Java? This proof of concept answers "yes, for the declarable subset" and
demonstrates the irreducible imperative floor, by actually running the tool. It
accompanies the prose crosswalk at `doc/reference/raid-datacite-crosswalk.md` and
the DataCite LinkML schema at `api-svc/datamodel/src/v2/datacite.yaml`.

This is spike/investigation output (RAID-831). It is not wired into any build.

## Files

- `raid-to-datacite.transform.yaml` — the transformation specification (the answer).
- `sample-raid.yaml` — a synthetic `RaidDto` instance used as input.
- `out-datacite.yaml` — the real transformed output produced by the run below.

## Tool (verified, not asserted)

- Package: **linkml-map** (the LinkML Transformer). PyPI
  <https://pypi.org/project/linkml-map/>, GitHub/docs
  <https://github.com/linkml/linkml-map>. Verified with version **0.5.3**.
- It is not a project dependency; run it ad hoc in a throwaway environment, e.g.
  `uv venv && uv pip install linkml-map`, or `pipx run --spec linkml-map linkml-map ...`.
- CLI: `linkml-map` with subcommands `map-data`, `derive-schema`, `compile`,
  `invert`, `validate-spec`.
- Spec format: a `TransformationSpecification` YAML (top-level keys `id`, `title`,
  `description`, `prefixes`, `class_derivations`, `enum_derivations`; it does NOT
  accept `name`, `imports`, or `default_prefix`).
  - `class_derivations` is keyed by TARGET class; each has `populated_from`
    (SOURCE class) and `slot_derivations`.
  - `SlotDerivation` supports `populated_from` (including dotted inline paths like
    `type.id`), `expr` (LinkML expression language), a constant `value` (including
    a constant nested object as a literal dict), and `value_mappings`.
  - `enum_derivations` is keyed by TARGET enum; each `PermissibleValueDerivation`
    is keyed by the TARGET permissible value and lists the SOURCE permissible
    value(s) under `populated_from`. A source value listed nowhere yields null, so
    the target slot is omitted. This is exactly how a permissible-value →
    permissible-value mapping is expressed, and it matches the Java factories'
    `Map.get(...)`-on-unmapped-key behaviour.

## What the spec declares

Faithful to `doc/reference/raid-datacite-crosswalk.md`:

- **types** — constant nested object `{value: "RAiD", resourceTypeGeneral: "Project"}`.
- **titles[]** — `value` ← `text`; `lang` ← `language.id`; `titleType` ← `type.id`
  via the `TitleTypeEnum` derivation (schema/4 → AlternativeTitle, schema/156|157
  → Other; schema/5 primary is unmapped, so `titleType` is omitted on the primary).
- **dates[]** — one Date from RAiD's single `date`; `value` is the expr
  `startDate + '/' + endDate if endDate else startDate`; constant `dateType: Other`.
  RAiD single-value → DataCite list is coerced automatically.
- **descriptions[]** — `value` ← `text`; `lang` ← `language.id`; `descriptionType`
  ← `type.id` via `DescriptionTypeEnum` (318 → Abstract, 8 → Methods, 3|6|7|9|319
  → Other).

## Running it

From the repository root, with `linkml-map` available on the PATH:

```
linkml-map map-data \
  -T doc/spike/RAID-831-datacite-linkml-map-poc/raid-to-datacite.transform.yaml \
  -s api-svc/datamodel/src/v2/raid-core.yaml \
  --target-schema api-svc/datamodel/src/v2/datacite.yaml \
  --source-type RaidDto \
  -o /tmp/out-datacite.yaml \
  doc/spike/RAID-831-datacite-linkml-map-poc/sample-raid.yaml
```

Exit code 0. It runs against the **real** `raid-core.yaml` (34 classes load
cleanly). The dynamic SPARQL `reachable_from` enums do NOT break the transform and
trigger NO network calls: `transform_enum` matches the incoming value string
against each derivation's `populated_from` list, so it never materialises the
dynamic source enum. The output (`out-datacite.yaml`) is:

```yaml
resourceType: {value: RAiD, resourceTypeGeneral: Project}
titles:
- {value: Reef Genomics Activity, lang: eng}          # primary: titleType omitted
- {value: Reef Genomics, lang: eng, titleType: AlternativeTitle}
dates:
- {value: "2023-01-01/2025-12-31", dateType: Other}
descriptions:
- {value: "Sequencing coral symbiont genomes...", lang: eng, descriptionType: Abstract}
- {value: "Nanopore long-read sequencing...", lang: eng, descriptionType: Methods}
```

Every crosswalk rule in scope fired correctly.

### Honest caveats about the run

- linkml-map runs a static pre-validation pass that emits non-fatal `[error]`
  lines it does not understand: it flags `type.id` (a dotted inline path) and the
  constant `value:` derivations as "Source slot not found on source class". These
  are false positives from the static checker; the runtime resolves them, exit
  code is 0, and the output above is correct.
- `[warning] Required target slot '...' has no derivation` for `identifier`,
  `creators`, `publisher`, `publicationYear` is expected and correct: those are the
  imperative floor deliberately left unmapped.

## The limit: what CANNOT be in the spec

linkml-map's expression language is sandboxed and side-effect free. Its only
author-extension hook is `@safe_function` (`-F/--functions`), whose docstring
requires functions be "pure, bounded-time, and free of I/O"
(`linkml_map/utils/extensions.py`). So there is no supported hook for:

1. **Live name resolution** — `creators[].name` (ORCID/ISNI), `contributors[].name`
   / `fundingReferences[].funderName` / `publisher.name` (ROR) are outbound HTTP
   calls whose result is not present anywhere in the RAiD record. No
   `populated_from`, `expr`, `value_mappings`, or enum derivation can produce a
   value that does not exist in the source. This is the hard floor.
2. **The organisation split** — one RAiD `organisation[]` is partitioned into
   `contributors[]` vs `fundingReferences[]` by inspecting roles for the Funder id
   (…/186); a mixed-role org appears in both. Predicate-driven set partitioning.
3. **"Latest role" selection** — `contributorType` uses the non-Funder role with
   the maximum `startDate`; a sort/reduce over a collection.

## Verdict

Configuring the crosswalk in LinkML is possible for the declarable subset and is
proven by a real run: constants, structural renames, nested-object construction,
`expr` concatenation, single→list coercion, and the controlled-vocabulary lookups
(as `enum_derivations`) all transform correctly from the real `raid-core.yaml` to a
DataCite 4.7-shaped instance. Name resolution and the organisation-partition /
role-selection logic must remain code. Recommended shape (see the crosswalk note):
externalise the vocabulary crosswalks declaratively — the `enum_derivations` here
are directly reusable — and keep a thin imperative adapter for live PID name
resolution and the org split.
