# Consolidating RAiD's schema.org JSON-LD implementations

**Ticket:** [RAID-759](https://ardc.atlassian.net/browse/RAID-759) — Investigate consolidating RAiD's schema.org JSON-LD implementations into a single governing definition
**Type:** Investigation (no code change)
**Author:** Rob Leney
**Date:** 2026-07-23
**Related:** [RAID-756](https://ardc.atlassian.net/browse/RAID-756), [RAID-757](https://ardc.atlassian.net/browse/RAID-757), [RAID-758](https://ardc.atlassian.net/browse/RAID-758), epic [RAID-574](https://ardc.atlassian.net/browse/RAID-574) (LinkML), [HELP-2753](https://ardc.atlassian.net/browse/HELP-2753)

---

## 1. Summary

The ticket describes three inconsistent RAiD → schema.org mappings. The investigation confirms the divergence and adds two material findings:

1. **There are effectively five schema.org-related mappings, not three**, and two of the five are dead code that governs nothing.
2. **The two "aspirational" definitions (the LinkML schema and the static-site `json-ld.ts`) already agree structurally.** The live API converter is the real outlier.

Recommendation, in brief: adopt the LinkML `researchproject.yaml` as the single canonical definition, complete it so it exactly describes the RAID-756/757/758-corrected `json-ld.ts` output, generate the JSON-LD `@context` and reference documentation from it, and then align or retire the API converter. This makes the in-flight RAID-756/757/758 work the validated target rather than throwaway effort, and it sits naturally under the LinkML epic (RAID-574).

> Everything below is drawn from reading the code on `main`/`feature/RAID-758` as of 2026-07-23. Claims about what each path emits are traceable to the cited files and lines. Where a property choice looks incorrect against schema.org convention this is stated as an observation, not a defect ruling.

---

## 2. What actually exists (inventory)

| # | Location | What it is | Live? | Consumer |
|---|----------|-----------|-------|----------|
| 1 | `api-svc/datamodel/src/v2/researchproject.yaml` | LinkML "RAiD → schema.org ResearchProject Mapping". Compiled by `generateJSONSchemaV2ResearchProject` to `researchproject.json`, merged into `raid-openapi-3.1.yaml` / `raid-openapi-strict-3.1.yaml`, which generates `idl.raidv2.model.ResearchProject*` Java classes. | Generates artefacts, but **the generated classes are dead** | none |
| 2 | `api-svc/raid-api/.../converter/RaidJsonLdConverter.java` | The API's content-negotiated `application/ld+json` output for `GET /raid/{prefix}/{suffix}`. Hand-builds JSON with Jackson `ObjectNode`s. | **Live** | no known consumer |
| 3 | `raid-agency-app-static/src/utils/json-ld.ts` | The Astro static-site landing-page JSON-LD, harvested by RDA via the sitemap. Being corrected by RAID-756/757/758. | **Live** | RDA (real, validating) |
| 4 | `api-svc/raid-api/.../model/schemaorg/*` (17 POJOs) | A hand-written schema.org object model (`ResearchProject`, `Person`, `Member`, `Sponsor`, `PrincipalInvestigator`, …). | **Dead** — referenced only within its own package | none |
| 5 | `api-svc/raid-api/.../service/rdf/RaidRdfService.java` | Builds an Apache Jena RDF model, serialised as Turtle / N-Triples / RDF-XML by sibling converters. | **Live** | RDF content negotiation |

Supporting facts established during the investigation:

- **The LinkML output really is wired into the build.** `idl-raid-v2/build.gradle` feeds `researchproject.json` into both `assembleOpenAPIV2Specs` and `assembleOpenAPIV2InternalSpecs`; `AssembleOpenAPI.java` copies its `$defs` into `components/schemas`. `openApiGenerate` then emits `ResearchProject.java`, `ResearchProjectRole.java`, `ResearchProjectLocation.java`, `ResearchProjectParentOrganization.java`. **No hand-written code imports any of them** (grep for `raidv2.model.ResearchProject` → zero hits outside the generated files). So the LinkML schema currently produces only dead classes and is preserved through `clean` purely so CI (which has no Docker/LinkML) can still assemble the spec.
- **The hand-written `model/schemaorg/*` package is also dead.** Every reference to it is inside the package itself. `RaidJsonLdConverter` does not use it — it constructs raw `ObjectNode`s.
- **`RaidJsonLdConverter` injects `RaidRdfService` but never calls it** (unused field). The RDF path (#5) is a genuinely separate vocabulary: it mints most properties in a custom `raid:` namespace (`https://raid.org/schema#`), not schema.org, so it is adjacent rather than a fourth schema.org emitter.
- **DataCite is a separate concern.** `factory/datacite/DataciteRelatedIdentifierFactory.java` maps `relatedObject` categories to DataCite `relationType` values (`References`, `IsSupplementedBy`, …), not schema.org. It is not part of this consolidation but is worth noting as yet another independent RAiD-relationship mapping.
- **The API's `application/ld+json` is negotiated on `GET /raid/{prefix}/{suffix}`** (`RaidController.findRaidByName`). The 3.0 human-facing spec advertises `application/ld+json` as an opaque `type: object` only; the ResearchProject structure exists solely in the generated 3.1 specs.

---

## 3. Field-by-field comparison of the two live emitters and the LinkML schema

Legend: **API** = `RaidJsonLdConverter` (#2), **Static** = `json-ld.ts` (#3, post RAID-756/757), **LinkML** = `researchproject.yaml` (#1).

| RAiD field | API (`RaidJsonLdConverter`) | Static (`json-ld.ts`) | LinkML (`researchproject.yaml`) |
|---|---|---|---|
| `@context` | Object: `@vocab: schema.org` + custom `raid/dcterms/foaf/skos` + per-property IRI remaps (e.g. `principalInvestigator`→`accountablePerson`) | String `"https://schema.org"` | `context` slot + `prefixes` block; `@context` generated separately by `gen-jsonld-context` on `raid-core.yaml` (not this file) |
| `@type` | `ResearchProject` | `ResearchProject` | `ResearchProject` ✓ all agree |
| RAiD identifier | `@id` = id; `identifier` = bare string, **then overwritten later** by an `alternateIdentifier` `PropertyValue[]` | `identifier` = `PropertyValue{propertyID: …/raid, name:"RAiD", value:id}` | `identifier` range `PropertyValue`, required, `exact_mappings: raid:id` |
| Title(s) | `name` = first title; extra titles → `alternateName` = array of invented `@type:"Name"` objects | `name` = all titles joined by `" \| "`; `headline` = primary-type title | `name` range string; **no `headline`, no `alternateName`** |
| Description(s) | `description` = first; extra → `abstract` = array of invented `@type:"Description"` objects | `description` = primary-type description only | `description` range string |
| Dates | `startDate` / `endDate` | `foundingDate` / `dissolutionDate` | `foundingDate` / `dissolutionDate` (Static + LinkML agree; `ResearchProject` ⊂ `Organization`, so founding/dissolution are the correct properties) |
| Contributors (people) | `contributor` = `Person[]` with `roleName` (raw CRediT URIs), `hasOccupation` (positions), invented `leadOrSupervisor`/`contactPoint` booleans; first leader → `principalInvestigator` | Flattened into `member` = `Role[]` (`schema:Role`), each `member`→`Person` with ORCID `PropertyValue`; positions + CRediT roles both become `Role`s with **resolved labels** (RAID-756) | `member` range `ResearchProjectRole` (`Role`) → `Member` (Static + LinkML share the `Role`/`member` pattern) |
| Organisations | `sponsor` = **all** orgs; `funder` = orgs whose role string `.contains("fund"/"sponsor")`; org roles under invented `roleOccupation` | Org roles → `member` (non-funder) and `funder`, split by **exact vocab URI** (`…/organisation.role.schema/186`) | `member` + `funder` range `ResearchProjectRole`; owner → `parentOrganization` |
| Subjects | `about` = `DefinedTerm[]` with invented `termCode` + nested `hasDefinedTerm`; also root `keywords` | `knowsAbout` = `DefinedTerm[]` with resolved ANZSRC `name` (RAID-756) | `knowsAbout` range `DefinedTerm`; `keywords` range string (Static + LinkML agree on `knowsAbout`) |
| Related objects (outputs/inputs) | `isPartOf` = `CreativeWork[]` (`identifier` bare, `url`, `additionalType`, `category[]`) | `citation` = `CreativeWork[]` with `identifier` `PropertyValue`, formatted `name`, `additionalType` = category (RAID-757) | **absent** (ticket-noted gap) |
| Related RAiDs | `isRelatedTo` = all, `relationshipType` = raw URI | Split into `isPartOf`/`hasPart`/`isBasedOn`/`isRelatedTo`; `isRelatedTo` carries `relationshipType` + resolved `relationshipTypeName` | **absent** (ticket-noted gap) |
| Spatial coverage | `spatialCoverage` = `Place[]` | — | `location` range `Place` (single) |
| Access | `contentAccessMode` (remapped to `schema:accessMode`) = `CreativeWork` w/ `conditionsOfAccess`, `accessibilitySummary`, `embargo` | — | — |
| Alternate URLs | `url` (single) or `sameAs[]` | — | — |
| Alternate identifiers | `identifier` = `PropertyValue[]` (overwrites the RAiD identifier set earlier) | — | — |
| License | `license` string | — | — |
| Registration agency / owner | `publisher` = `Organization` (registration agency) | `parentOrganization` = `Organization` (registration agency ROR as `PropertyValue`) | `parentOrganization` = `Organization` described as "the RAiD **owner**" |

### Non-standard vocabulary in the API converter

The API converter uses several property names and `@type`s that match neither schema.org, the LinkML schema, nor `json-ld.ts`:

- Invented `@type`s `"Name"` and `"Description"` (not schema.org types).
- Invented properties `roleOccupation`, `hasOccupation` (mis-used), `leadOrSupervisor`, `contactPoint` (as a boolean), `termCode`, `hasDefinedTerm`.
- `sponsor` used for **all** organisations regardless of role.
- Funder detection by substring match on the role URI (`.contains("fund")`) rather than the vocabulary URI.
- Related objects placed under `isPartOf` and related RAiDs all under `isRelatedTo`, losing the relationship semantics that `json-ld.ts` preserves.
- `identifier` is set twice and the second write silently replaces the first.

### Why they diverge

- **Different authors, times, and consumers.** The API converter was written March 2025 as a one-shot hand mapping and has not been validated against any consumer. `json-ld.ts` is being shaped this sprint against a real consumer (RDA / Melanie Barlow harvesting landing pages for the NESP Domain Data Portal, HELP-2753). The LinkML schema is an intended model that was never finished or wired to a live emitter.
- **No shared source.** None of the paths derives from any other; there is no generation step or validation check linking them.
- **The good news:** the *intended* model (LinkML) and the *validated* live output (`json-ld.ts`) already converge on the important structural choices — `Role`/`member`, `knowsAbout`, `funder`/`member`, `parentOrganization`, `foundingDate`/`dissolutionDate`, `PropertyValue` identifiers. Consolidation is therefore mostly a matter of completing the LinkML schema to match `json-ld.ts` and retiring/aligning the API converter, not reconciling three equally-divergent designs.

---

## 4. Recommendation: a single canonical definition

**Adopt the LinkML `researchproject.yaml` as the single governing definition of RAiD's schema.org representation.**

Rationale:

- It is Matthias's stated end goal (17 July 2026) and the natural home for this work under the LinkML epic (RAID-574).
- LinkML is purpose-built to be the single source that other artefacts derive from: it can emit JSON Schema, a JSON-LD `@context`, SHACL, OWL, and documentation (all of those generators already exist in `datamodel/build.gradle`).
- It already matches the validated `json-ld.ts` output structurally, so completing it is low-risk.

Treat the **RAID-756/757/758-corrected `json-ld.ts` output as the reference specification** the canonical schema must describe. In other words: the static site defines *what correct output looks like today* (because it is the only consumer-validated path); the LinkML schema becomes the *authoritative definition* of that output; both live emitters then derive from, or are validated against, the LinkML schema.

### Rough migration plan

**Phase 0 — land the in-flight fixes (already under way).** Complete RAID-756 (resolved labels), RAID-757 (outputs → `citation`), RAID-758 (reference doc). These define the target output.

**Phase 1 — complete the canonical LinkML schema.** Bring `researchproject.yaml` up to what `json-ld.ts` now emits:
- add `relatedObject` → `citation` (`CreativeWork` with `PropertyValue` identifier + formatted `name` + `additionalType`);
- add `relatedRaid` → `isPartOf`/`hasPart`/`isBasedOn`/`isRelatedTo` with `relationshipType`/`relationshipTypeName`;
- add `headline`; add label slots to `Role`/`DefinedTerm`;
- resolve the `parentOrganization` semantics (registration agency vs owner — the two live emitters and the schema description disagree; pick one and document it).

**Phase 2 — generate downstream artefacts from the schema.**
- Generate the JSON-LD `@context` (`generateJSONLDContextV2`, currently only run for `raid-core.yaml`).
- Generate the schema.org reference documentation (`generateSchemaOrgDocsV2` exists but nothing publishes its output). This is the *generated* successor to RAID-758's hand-written page.
- Optionally generate SHACL for validation.

**Phase 3 — make the emitters derive from / validate against the canonical schema.**
- **Static site:** generate the TypeScript types (and ideally the `@context`) from the schema so `json-ld.ts` stops being hand-maintained, or at minimum add a CI check that its output validates against the LinkML-derived JSON Schema / SHACL.
- **API converter:** decide between (a) rewriting `RaidJsonLdConverter` to emit the canonical structure (replacing all invented property names), or (b) **retiring the API's `application/ld+json` negotiation** if it has no consumer. Confirm first whether anything consumes the API `ld+json` — current evidence (HELP-2753, the sitemap-harvesting approach) is that RDA harvests the static site, not the API. If there is no consumer, retirement is the lowest-risk consolidation and removes the worst-diverging path outright.

**Phase 4 — delete dead code.** Remove the generated `idl.raidv2.model.ResearchProject*` classes' dead status (they will still be generated as spec artefacts, but confirm intent) and delete the hand-written `model/schemaorg/*` POJO package unless a phase above adopts it.

---

## 5. How RAID-756/757/758 carry over (AC3)

**None of that work is wasted under this recommendation** — it is precisely what defines the canonical target.

- **RAID-756 (resolved labels)** and **RAID-757 (outputs → `citation`)** change `json-ld.ts`, which becomes the reference output the completed LinkML schema must describe. Their corrections become schema slots in Phase 1.
- **RAID-758 (reference documentation)** is the interim, hand-written mapping doc. Under this plan its long-term successor is the *generated* documentation from Phase 2 (`generateSchemaOrgDocsV2`). Recommend RAID-758's page carry a note that it will be superseded by schema-generated docs tracked under RAID-759's follow-on, and that it doubles as the acceptance test for the generated version.

The only rework risk is if RAID-759's follow-on chooses a materially different output shape from what 756/757 produce. It should not: the recommendation explicitly freezes the 756/757 output as the target.

---

## 6. Relationship to RAID-574 and HELP-2753 (AC4)

- **RAID-574 (LinkML epic, currently To Do).** Consolidating on the LinkML schema is squarely this epic's remit. Recommend RAID-759's follow-on implementation tickets (schema completion, generator wiring, emitter alignment/retirement) be created as children of RAID-574. Its existing children (RAID-575 validation inventory, RAID-461 LinkML integration with Joonas, both Done) are consistent groundwork.
- **HELP-2753** raised the separate architecture question of LinkML vs the API's content-negotiation JSON-LD. This investigation answers part of it: the API `ld+json` is non-standard, has no known consumer, and its LinkML-generated model is dead code. That directly supports treating the content-negotiation JSON-LD as currently *unmanaged* and either bringing it under the canonical definition or retiring it (Phase 3b). Flag this conclusion to Matthias as the input HELP-2753 was waiting on.

---

## 7. Suggested follow-on tickets (for discussion, not created)

1. Complete `researchproject.yaml` to match the RAID-756/757 output (Phase 1). — child of RAID-574
2. Generate and publish the schema.org `@context` + reference docs from the LinkML schema (Phase 2); supersede RAID-758's hand-written page.
3. Decide the fate of the API `application/ld+json`: align to canonical or retire (Phase 3b) — needs a consumer check first.
4. Validate `json-ld.ts` output against the LinkML-derived schema in CI (Phase 3a).
5. Remove dead `model/schemaorg/*` POJOs (Phase 4).
