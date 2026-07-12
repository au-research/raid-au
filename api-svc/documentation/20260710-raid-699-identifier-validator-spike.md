# RAID-699 Spike — Identifier validator coverage for RAID-CL v1.7.x

**Ticket:** [RAID-699](https://ardc.atlassian.net/browse/RAID-699) — *Spike: For each identifier supported in RAID-CL v1.7.0, determine whether a validator already exists (e.g. DOI, ORCID) or whether one needs to be created (e.g. ARKs). Stretch goal: work out how to validate each one.*

**Author:** Rob Leney · **Date:** 2026-07-10 · **Status:** Spike (no code changes)

---

## TL;DR

- Of the identifier schemes RAID-CL enumerates, **8 already have working validators** (RAiD, ORCID, ISNI, ROR, DOI, Web Archive, GeoNames, OpenStreetMap).
- **4 have no validator** and would be rejected today even though the strict schema lists them: **ARK, Handle (hdl.handle.net), RRID (SciCrunch), ISBN** — all `relatedObject.schemaUri` values.
- The gap is concentrated in **`RelatedObjectValidator`**, which hard-codes an allow-list of only DOI + Web Archive and rejects the other four enum values.
- Two things need a **product/vocab decision** before implementation, called out below (ARK URI form, and the "proposed vs implemented" boundary).

---

## Scope note: which version are we actually validating against?

The ticket says **v1.7.0**. The codebase does not contain that string. The LinkML data model pins every controlled-list enum to **RAID-CL v2, version `1-7-1`**, served from the ARDC *demo* vocab server:

```
# datamodel/src/v2/core-enums.yaml (and extended-enums.yaml)
source_ontology: https://demo.vocabs.ardc.edu.au/repository/api/sparql/raid_raid-controlled-lists-v2_1-7-1
```

The allowed `schemaUri` values are baked into the **strict** OpenAPI spec (`idl-raid-v2/src/raid-openapi-strict-3.1.yaml`) and cached in `datamodel/sparql-cache/*.json`. This analysis is against those artefacts. Two follow-ups for the ticket author:

1. Confirm "v1.7.0" was shorthand for the `1-7-1` list we ship.
2. The pin targets the **demo** vocab server, not prod (`vocabs.ardc.edu.au`). Worth confirming that is intended.

---

## Coverage matrix

Legend: ✅ validator exists · ⚠️ partial · ❌ none (must build)

| # | Scheme | schemaUri (as in our strict spec) | Field(s) | Validator | Status |
|---|--------|-----------------------------------|----------|-----------|--------|
| 1 | **RAiD** | `https://raid.org/` | `Id` (the RAiD itself) | `IdentifierParser` (handle/URL structural parse) | ✅ |
| 2 | **ORCID** | `https://orcid.org/` | Contributor | `ContributorTypeValidator` + `OrcidClient` (regex + live HEAD) | ✅ |
| 3 | **ISNI** | `https://isni.org/` | Contributor | `ContributorTypeValidator` + `IsniClient` (existence). Standalone MOD-11-2 checksum validator `IsniValidator` exists but is **dead code** (unwired) | ⚠️ |
| 4 | **ROR** | `https://ror.org/` | Organisation, Owner/RegistrationAgency | `OrganisationValidator` + `RorClient` (regex + live existence) | ✅ |
| 5 | **DOI** | `https://doi.org/` | RelatedObject | `DoiService` (regex + HEAD) | ✅ |
| 6 | **Web Archive** | `https://web.archive.org/` | RelatedObject | `RelatedObjectValidator` (regex on `/web/{14-digit}/…`) | ✅ |
| 7 | **ARK** | `https://arks.org/` | RelatedObject | — | ❌ |
| 8 | **Handle** | `https://hdl.handle.net/` | RelatedObject | — | ❌ |
| 9 | **RRID (SciCrunch)** | `https://scicrunch.org/resolver/` | RelatedObject | — | ❌ |
| 10 | **ISBN** | `https://www.isbn-international.org/` | RelatedObject | — | ❌ |
| 11 | **GeoNames** | `https://www.geonames.org/` | SpatialCoverage | `GeoNamesUriValidator` (regex + GeoNames API) | ✅ |
| 12 | **OpenStreetMap** | `https://www.openstreetmap.org/` | SpatialCoverage | `OpenStreetMapUriValidator` (regex + HEAD) | ✅ |

**Not identifier PIDs** (RAiD-specific vocabulary URIs, validated by DB-backed allow-lists — out of scope for "identifier validators"): Subject (ANZSRC FoR/SEO), ContributorPosition, ContributorRole (CRediT), RelatedRaidType, Traditional Knowledge Labels. `AlternateIdentifier.id` / `AlternateUrl.url` are free-text with no scheme enum and only presence validation — a known soft spot, but not part of the controlled list.

---

## The gap: `RelatedObjectValidator`

`raid-api/.../validator/RelatedObjectValidator.java` hard-codes:

```java
DOI_SCHEMA_URI          = "https://doi.org/"
WEB_ARCHIVE_SCHEMA_URI  = "https://web.archive.org/"
// RELATED_OBJECT_SCHEMA_URI = [ the two above ]
```

Any other `schemaUri` is rejected as `INVALID_SCHEMA`. But the strict spec's `RelatedObjectSchemaUriEnum` now permits **six** values (ARK, DOI, Handle, RRID, Web Archive, ISBN). So a schema-valid ARK/Handle/RRID/ISBN related object passes OpenAPI validation and is then **refused by the service-layer validator**. Closing this is the substance of the follow-up implementation work.

---

## Two decisions needed before implementation

1. **ARK URI form.** Our repo enum uses `https://arks.org/`. The current public RAiD metadata docs (`metadata.raid.org`, "latest") list ARK as `https://n2t.net/ark:` and mark it *proposed / not yet implemented*. These are different namespaces and a validator must match whatever we actually store. Confirm the canonical ARK schemaUri for the `1-7-1` list before building.

2. **"Proposed" vs "implemented".** The published docs split ARK / Handle / ISBN / RRID into a *proposed, not-yet-implemented* tier, while our strict enum lists all six as allowed. Confirm product intent: do we validate-and-accept these now, or keep rejecting until the vocab formally promotes them? (Same question applies to ISNI for contributors — docs say ORCID-only, our enum allows ISNI.)

---

## Stretch goal — how to validate each missing scheme

Three established patterns exist in the codebase, so no new infrastructure is needed:

- **A. Regex + HTTP HEAD existence** — extend `AbstractUriValidator` (like Ror/Doi/Orcid/OpenStreetMap), add a stub, register in `ExternalPidService`.
- **B. Local checksum/format only** — standalone class returning `List<ValidationFailure>` (the ISNI checksum style).
- **C. DB-backed allow-list** — `*SchemaRepository.findActiveByUri` + `*Repository.findByUriAndSchemaId` (Subject / RelatedObjectType style).

Recommended approach per scheme:

| Scheme | Canonical format | Example | Resolvable? | Recommended validation |
|--------|------------------|---------|-------------|------------------------|
| **ARK** | `ark:/{NAAN}/{name}` — NAAN is a 5–9 digit assigning-authority number, then an opaque name; optional qualifier path | `https://arks.org/12345/x54xz321` (stored form) · `ark:/12345/x54xz321` (bare) | Yes, via N2T / `arks.org` resolver | **A** — structural regex on NAAN + name, then optional HEAD to the resolver. Pin the exact host per decision #1. |
| **Handle** | `{prefix}/{suffix}` — prefix is a numeric naming authority (e.g. `20.500.12345`), suffix opaque | `https://hdl.handle.net/20.500.12345/abc-def-123` | Yes, via `hdl.handle.net` | **A** — regex + HEAD. **Guard:** DOIs are Handles too (prefix `10.`); route `10.*` to the DOI validator so we don't accept a DOI under the Handle scheme. |
| **RRID** | `RRID:{Source}_{id}` e.g. `RRID:AB_123456`, `RRID:SCR_001905` | `https://scicrunch.org/resolver/RRID:AB_2298772` (stored form) · `RRID:AB_2298772` (bare) | Yes, via `scicrunch.org/resolver/` | **A** — regex on the `RRID:PREFIX_id` shape + optional resolver HEAD. |
| **ISBN** | ISBN-13 (13 digits, GS1 mod-10 check) or ISBN-10 (mod-11, trailing `X` allowed) | `978-3-16-148410-0` (ISBN-13) · `0-306-40615-2` (ISBN-10) | **No clean per-ISBN resolver** (isbn-international is not a resolver) | **B** — local checksum only. Normalise hyphens/spaces, validate both ISBN-10 and ISBN-13 check digits. No network call. |
| **ISNI** *(revisit)* | 16 digits, MOD-11-2 check digit (last char digit or `X`) | `https://isni.org/isni/0000000121032683` (stored form) · `0000 0001 2103 2683` (bare) | Yes, but existence already checked by `IsniClient` | **B** — wire the existing dead `IsniValidator` (checksum) into the contributor path so we gate malformed ISNIs before the external lookup. |

Notes:
- ISBN and ISNI are checksum-only because there is no reliable lightweight resolver; the value is self-validating. This matches pattern B and avoids a network dependency.
- ARK / Handle / RRID all suit pattern A (regex + existence), consistent with DOI/ROR/ORCID, and should each get a stub (`raid.stub.*`) so integration tests don't hit live resolvers.

---

## Recommended follow-up tickets

1. **Extend `RelatedObjectValidator`** to dispatch by `schemaUri` across all six enum values (refactor the hard-coded two-item allow-list into a scheme→validator map, mirroring `spatialCoverageUriValidatorMap`).
2. **Build ARK, Handle, RRID validators** (pattern A, each with a stub) — blocked on decisions #1 and #2.
3. **Build ISBN checksum validator** (pattern B).
4. **Wire the dormant `IsniValidator` checksum** into the contributor path (pattern B) — low-risk cleanup independent of the above.
5. **Housekeeping:** de-duplicate the two parallel ROR paths (`RorValidator`/`RorService` vs `OrganisationValidator`/`RorClient`) and the duplicated ROR regex — noted during the spike, not required for this work.

---

## Sources

- **Codebase:** `datamodel/src/v2/*.yaml`, `idl-raid-v2/src/raid-openapi-strict-3.1.yaml`, `datamodel/sparql-cache/*.json`, and the `raid-api/.../validator/` + `.../service/` + `.../client/` trees.
- **External (verified):** RAiD Metadata Schema docs (`metadata.raid.org` — identifier, contributors, organisations, relatedObjects), ARDC Research Vocabularies Australia RAID-CL v1.7.0 landing page (`vocabs.ardc.edu.au/viewById/682`, "Current", dated 24 June 2026), `au-research/raid-metadata` GitHub. Standardised as ISO 23527.
- Public docs mark ARK/Handle/ISBN/RRID (related objects) and ISNI (contributors) as *proposed / not yet implemented*, which is the source of decision #2. Exact regex/structural formats above are from the identifier authorities, not RAiD-validated patterns.
