# Remove dead `model/schemaorg/*` POJO package

**Date:** 2026-08-06
**Ticket:** [RAID-783](https://ardc.atlassian.net/browse/RAID-783) — Remove dead model/schemaorg/* POJO package (Story, parent epic [RAID-574](https://ardc.atlassian.net/browse/RAID-574))
**PR:** _see linked pull request_

## What changed

Deleted the hand-written schema.org object model package `au.org.raid.api.model.schemaorg` from `api-svc/raid-api`. All 17 POJOs were removed:

`ContentAccessMode`, `Context`, `Description`, `Identifier`, `Keyword`, `Member`, `OrganizationRole`, `OwnerId`, `Person`, `Place`, `PrincipalInvestigator`, `PropertyValue`, `Publisher`, `RegistrationAgencyId`, `ResearchProject`, `Sponsor`, `TemporalCoverage`.

## Why

This is the cleanup follow-on from investigation [RAID-759](https://ardc.atlassian.net/browse/RAID-759) (Phase 4), which identified the package as dead code. The package was a hand-written schema.org model that no code path used.

### Confirmation that the package was safe to remove (AC1 and AC2)

- **No references anywhere.** There are no `import au.org.raid.api.model.schemaorg...` statements and no fully-qualified references to the package anywhere in the `api-svc` main or test source. Every class was referenced only from within its own package. The only apparent external hits were unrelated same-simple-name classes reached through different imports, plus `"ResearchProject"` used as a plain string literal.
- **No non-Java references.** The package is not referenced from any YAML, XML, properties, JSON, or Gradle file. (The `schemaorg-docs` hits in `api-svc/datamodel` are LinkML documentation-generation output paths, unrelated to this Java package.)
- **The alignment path did not adopt it (AC1).** RAID-783 was sequenced after [RAID-781](https://ardc.atlassian.net/browse/RAID-781) to avoid removing anything the alignment work reused. RAID-781 resolved to **retire** the API's `application/ld+json` content negotiation rather than align it (rationale: RDA harvests the static-site JSON-LD via sitemap crawling, not the API endpoint — HELP-2753). Its implementation (PR #599) **removed `RaidJsonLdConverter` entirely**; the endpoint now returns `406 Not Acceptable`. Nothing from `model/schemaorg/*` was reused. `RaidJsonLdConverter` no longer exists on `main`.
- **Build passes (AC2).** After deletion, `./gradlew build -x intTest` is `BUILD SUCCESSFUL`, with the `iam`, `db`, and `raid-api` unit-test suites passing and the `intTest` sources still compiling.

## Status of the generated `idl.raidv2.model.ResearchProject*` classes (AC3)

This ticket removes **only** the hand-written POJOs above. It does **not** touch the generated classes under `api-svc/idl-raid-v2/src/main/generated/au/org/raid/idl/raidv2/model/`. Their status, which is deliberately distinct, is recorded here:

These classes are produced by `openApiGenerate` from the committed OpenAPI specifications (`raid-openapi-3.1.yaml` and `raid-openapi-strict-3.1.yaml`). Unlike the deleted POJOs, they are **generated artefacts, not hand-written source** — deleting the `.java` files would be futile because the build regenerates them on every run. There are two distinct cases:

1. **`ResearchProjectRole` — live, in use.** It is `$ref`'d by the spec graph (the research-project role field) and is part of the generated API model in active use. It is **not** dead code and is not a removal candidate.

2. **`ResearchProject` (and its nested `ResearchProjectLocation` and `ResearchProjectParentOrganization`) — orphan schema, retained deliberately.** `ResearchProject` is defined as a top-level schema in the committed spec but is not `$ref`'d by any path or operation; the two nested classes are generated from its `location` and `parentOrganization` sub-objects. Although no hand-written code references them, they are retained because:
   - They exist only as a byproduct of the committed OpenAPI spec, which **CI assembles and consumes without Docker or LinkML available**. The committed spec (and its upstream LinkML source `datamodel/src/v2/researchproject.yaml`) is the source of truth for what the generator emits.
   - Genuinely removing them would require editing the OpenAPI spec and regenerating it through the Dockerised LinkML chain that CI cannot run — out of scope for this ticket.

In short: the deleted `model/schemaorg/*` POJOs were plain hand-written source referenced by nothing and safe to delete outright; the generated `idl.raidv2.model.ResearchProject*` classes are spec-derived, partly still in use (`ResearchProjectRole`), and any future removal of the orphan `ResearchProject` schema is a separate, spec-and-LinkML change, not a source deletion.

## Testing

- `./gradlew :api-svc:raid-api:compileJava :api-svc:raid-api:compileTestJava` — success
- `./gradlew build -x intTest` — `BUILD SUCCESSFUL`; `iam:test`, `api-svc:db:test`, `api-svc:raid-api:test` all pass; `intTest` sources compile (execution excluded as it requires Docker)

No new tests were added: the change is a pure deletion of unreferenced code, and the existing unit-test suites passing is the quality gate confirming nothing depended on the removed package.
