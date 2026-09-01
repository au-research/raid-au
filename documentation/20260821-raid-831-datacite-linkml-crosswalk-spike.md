# RAID-831: DataCite LinkML schema and RAiD→DataCite crosswalk (spike)

- Ticket: [RAID-831](https://ardc.atlassian.net/browse/RAID-831) (Spike)
- PR: [au-research/raid-au#620](https://github.com/au-research/raid-au/pull/620)
- Related: RAID-796 (DataCite schema alignment epic), RAID-832 (re-sync mechanism, defers the crosswalk to this spike), RAID-377 (mapping update to DataCite 4.7), RAID-776 (origin spike), RAID-797 (native RAiD relatedIdentifierType).
- Bugs raised out of this spike: [RAID-858](https://ardc.atlassian.net/browse/RAID-858), [RAID-859](https://ardc.atlassian.net/browse/RAID-859).

## What this spike answers

Whether the RAiD→DataCite metadata mapping can be driven declaratively from a LinkML
schema plus a crosswalk, instead of the current hand-written Java factories.

## What changed and why

Two design artifacts were added. No runtime behaviour changed.

- `api-svc/datamodel/src/v2/datacite.yaml`: a hand-authored LinkML rendering of the
  DataCite 4.7 metadata schema (24 classes, 10 controlled-vocabulary enums, values taken
  verbatim from the 4.7 XSD). It targets 4.7 to match RAID-377. It is an inert spike
  artifact: it is not referenced by the datamodel gradle build, so it does not affect
  codegen or CI. Validated with `gen-json-schema` via `linkml/linkml:1.9.2`.
- `doc/reference/raid-datacite-crosswalk.md`: a field-by-field RAiD→DataCite crosswalk
  authored from the Java factories under `api-svc/raid-api/.../factory/datacite/`, with the
  controlled-vocabulary lookup tables and a declarable-vs-imperative verdict.
- `doc/spike/RAID-831-datacite-linkml-map-poc/`: a runnable `linkml-map` (LinkML
  Transformer) proof of concept that configures the declarable subset of the crosswalk as a
  transformation spec and actually transforms a sample `RaidDto` into a DataCite 4.7-shaped
  instance (spec, sample input, real output, run instructions).

## Findings

- The automated importer `schemauto import-xsd` (linkml/schema-automator) does not handle
  DataCite's XSD: it fails on the `xs:all` root type, and after that workaround fails again
  in its simpleContent/extension handling. The schema was hand-authored from the XSD.
- The mapping is a hybrid. The vocabulary/structural layer (title/description/role/
  relation/resource-type lookups, constants, identifier renames and concatenations) is
  declarable via LinkML `exact_mappings` + `linkml-map`, with the code→enum maps expressible
  as SSSOM tables. The imperative floor that cannot be declarative: live name resolution
  (ORCID/ISNI/ROR at emission time) and the organisation-list partition into contributors
  vs fundingReferences with a "latest role" reduce. Recommendation: declarative vocab tables
  feeding a thin imperative adapter.
- The declarable half is proven, not just asserted: a `linkml-map` 0.5.3 spec
  (`doc/spike/RAID-831-datacite-linkml-map-poc/`) transforms a real `RaidDto` into a
  DataCite 4.7-shaped instance (exit 0) against the real `raid-core.yaml`, including the
  controlled-vocabulary lookups as `enum_derivations`. The imperative floor confirmably
  cannot be expressed: `linkml-map`'s only hook, `@safe_function`, forbids I/O.
- DataCite 4.7 adds `RAiD` as a native `relatedIdentifierType`, which is what makes
  RAID-797's native-`RAiD` emission schema-valid (RAID-797 emitted it ahead of the published
  XSD, relying on the REST API accepting it early).
- **A hand-rolled build-time generator is feasible**, and the objections to porting
  `linkml-map` do not apply to it: it reads a crosswalk dialect we define (no
  `linkml_runtime`), uses a small closed set of constructs instead of an expression
  language (no Python-semantics compatibility surface), and tracks no upstream. The
  precedent is already in the repo — `buildSrc` (`AddStaticEnums`,
  `GenerateReferenceDataTask`, `Utils`) parses LinkML YAML in the JVM with no Python and
  no Docker, and `openApiGenerate` already compiles generated Java into the API. Unlike
  the Docker-based LinkML tasks, a `buildSrc` generator can run in CodeBuild.
  It also buys a capability nothing else here offers: the build can **fail** on an
  unmapped vocabulary value, fixing the silent-`null` issue (Known issue 6).
  Recommended sequencing: externalise the vocabulary crosswalk as a governed table with
  the exhaustiveness check first; add generation only if field-level mapping proves a real
  maintenance burden.
- Resolution of PIDs to names should be an **enrichment step before** the crosswalk, not a
  capability the crosswalk invokes — it is what keeps the transformation pure and testable
  without a network, and it is the right boundary for DataCite Appendix 3 unknown values
  (`:unav`).
- The crosswalk should be **versioned per (RAiD schema version, DataCite schema version)
  pair**; re-sync of already-minted records plugs into RAID-832's flag + worker (Ready to
  Deploy), retiring per-correction backfill scripts. The gap is a "flag by predicate"
  operator entry point for selecting affected records.
- Measured against RAID-797 as the worked example, the mapping edit itself was two
  production lines. The expensive parts were **noticing** DataCite had changed and
  **re-pushing** minted records — neither of which any mapping representation fixes.

## Bugs surfaced (raised as their own tickets)

- [RAID-858](https://ardc.atlassian.net/browse/RAID-858): `publicationYear` is hardcoded to
  `Year.now()` in all three `DataciteAttributesDtoFactory` overloads (has a `// TODO`); an
  update in a later year silently rewrites the DOI's publication year.
- [RAID-859](https://ardc.atlassian.net/browse/RAID-859): `rightsList` is never emitted —
  `DataciteRightFactory` is injected but never called, so RAiD's licence never reaches
  DataCite. Also covers the `rightsUri` JSON key mismatch.

Both were re-verified against the post-merge code, remain unfixed on `main`, and are linked
to RAID-831 in JIRA. Neither is fixed here; the spike ships no runtime change.

## Verification performed

- `datacite.yaml` validates with `gen-json-schema` via `linkml/linkml:1.9.2` (exit 0, no
  warnings); the 4.7 enum additions were confirmed present in the generated JSON Schema.
- The `RelationType` and `RelatedIdentifierType` enums were diffed against the source
  4.7 XSD include files.
- No code or build change; CI is unaffected (the schema is not wired into the build).
