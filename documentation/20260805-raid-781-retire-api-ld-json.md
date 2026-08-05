# RAID-781: Retire the API's application/ld+json content negotiation

- **JIRA:** [RAID-781](https://ardc.atlassian.net/browse/RAID-781) (parent epic [RAID-574](https://ardc.atlassian.net/browse/RAID-574))
- **Depends on:** [RAID-780](https://ardc.atlassian.net/browse/RAID-780) (spike), follow-on from [RAID-759](https://ardc.atlassian.net/browse/RAID-759)
- **PR:** [au-research/raid-au#599](https://github.com/au-research/raid-au/pull/599)
- **Date:** 2026-08-05

## Decision

Retire, not align.

The `application/ld+json` content negotiation on `GET /raid/{prefix}/{suffix}` has been removed
rather than rewritten to emit canonical schema.org output.

## Why

The `RaidJsonLdConverter` emitted non-standard vocabulary (invented `@type`s such as "Name" and
"Description", invented properties like `roleOccupation` and `leadOrSupervisor`, incorrect
sponsor/funder handling, and a double-write bug on identifier). Aligning it to the canonical
LinkML/schema.org structure would have been a significant rewrite.

Investigation showed no consumer of the API's ld+json output. The only known/suspected integrator
is RDA (Research Data Australia), and HELP-2753 confirms RDA harvests the **static site** JSON-LD via
sitemap crawling (`https://static.prod.raid.org.au/sitemap-0.xml` to landing pages such as
`https://static.prod.raid.org.au/raids/10.71821/23fcbc6f`), not the API endpoint. On 21 May 2026,
Melanie Barlow (RDA) confirmed the static-site sitemap plus JSON-LD gives them the behaviour they
need. There is no evidence in HELP-2753 of any client requesting the API endpoint with
`Accept: application/ld+json`.

With no consumer and non-standard output, retiring the negotiation is lower risk and lower cost than
a rewrite. The static-site `json-ld.ts` remains the canonical JSON-LD source for external consumers.

## What changed

- **Deleted** `RaidJsonLdConverter.java` (the `@Component HttpMessageConverter<RaidDto>`) and its unit
  test `RaidJsonLdConverterTest.java`. The converter was registered solely by Spring Boot
  auto-detection, so deleting the class removes it from the converter chain.
- **`WebConfig.java`** - removed the loop in `extendMessageConverters(...)` that appended
  `application/ld+json` to every `MappingJackson2HttpMessageConverter`, plus the now-unused imports.
  Without this, an `Accept: application/ld+json` request would still have been served plain JSON. The
  three RDF converter registrations (Turtle, N-Triples, RDF/XML) are unchanged.
- **OpenAPI spec sources** - removed the `application/ld+json` 200-response entry from the codegen
  template `template-raid-openapi-3.1.yaml` and from the human-facing `raido-openapi-3.0.yaml`
  (satisfies acceptance criterion 3). Regenerated `raid-openapi-3.1.yaml` and
  `raid-openapi-strict-3.1.yaml`; the generated `RaidApi.java` `produces` list and 200/403
  `@ApiResponse` content no longer include `application/ld+json`.
- **Tests** - added `testJsonLdFormatNotAcceptable` to the live
  `ContentNegotiationIntegrationTest` (intTest source set) asserting a 406 for
  `Accept: application/ld+json`. The `@Disabled` `ContentNegotiationIntTest` carries a matching case
  for documentation.

## Behaviour change

`GET /raid/{prefix}/{suffix}` with `Accept: application/ld+json` now returns **406 Not Acceptable**.
The `application/json`, `text/turtle`, `application/n-triples`, and `application/rdf+xml`
representations are unchanged.

## Notes and follow-ups

- The required spec regeneration also swept up an unrelated `Organization`/`name` description change
  that originates from already-merged RAID-779 (the LinkML source on `main` was ahead of the last
  spec regeneration). This is pre-existing drift, not RAID-781 scope.
- The LinkML `researchproject.yaml` field description still names `RaidJsonLdConverter` as a live
  emitter, which is now stale. Correcting the prose needs a Docker-based regeneration and is worth a
  small follow-up ticket.

## Acceptance criteria

1. Documented decision made given RAID-780's findings - **met** (retire; recorded on RAID-781 and here).
2. If aligning, converter rewritten to canonical structure - **n/a** (retire chosen).
3. If retiring, content negotiation removed and human-facing 3.0 spec updated - **met**
   (`raido-openapi-3.0.yaml` updated).
