# RAID-619: Sitemap for RDA Harvesting - Investigation & Proposal

## Context

Research Data Australia (RDA) needs to harvest RAiD metadata so the NESP Domain Data Portal can display records for the NESP program, its 4 hubs, and all NESP projects. RDA's harvester crawls a **sitemap** and extracts **JSON-LD** (schema.org) from each listed page, then maps it to RIF-CS XML. This same infrastructure enables Google indexing. See [HELP-2753](https://ardc.atlassian.net/browse/HELP-2753) for full discussion.

RDA supports two harvester methods (both require a sitemap pointing to pages with JSON-LD):
- **JSON-LD Harvester** - sitemap crawler that extracts JSON-LD content
- **Dynamically inserted JSON-LD Harvester** - same, for dynamically injected JSON-LD

## Current State

### Sitemap - exists and is deployed

The Astro static site (`raid-agency-app-static`) generates a sitemap via `@astrojs/sitemap`:
- **Index**: `https://static.prod.raid.org.au/sitemap-index.xml`
- **URLs**: `sitemap-0.xml` contains 1,557 URLs
- **Declared in HTML**: `<link rel="sitemap" href="/sitemap-index.xml" />`

### JSON-LD - exists but insufficient

Each landing page at `/raids/{prefix}/{suffix}/` embeds a `<script type="application/ld+json">` block, but it only contains:

```json
{
  "@context": "https://schema.org",
  "@type": "ResearchProject",
  "name": "...",
  "identifier": { "@type": "PropertyValue", "propertyID": "RAID", "value": "..." },
  "alternateName": "...",
  "url": "https://doi.org/...",
  "funder": { "@type": "Organization", "identifier": "..." }
}
```

This is too thin for RDA to map to RIF-CS. The agreed-upon schema.org mapping (implemented in `ResearchProjectFactory.java` at commit `21c0f6b3`) is significantly richer.

### JSON endpoints - exist

Full RaidDto data is available at `/raids/{prefix}/{suffix}.json` for each public RAiD.

## Gaps

### 1. JSON-LD missing fields

The agreed mapping in `ResearchProjectFactory` includes fields not present in the current Astro JSON-LD:

| Field | schema.org Type | Source |
|---|---|---|
| `@id` | URL | `identifier.id` |
| `identifier.propertyID` | URL | `https://registry.identifiers.org/registry/raid` |
| `parentOrganization` | Organization | Registration agency with ROR identifier |
| `description` | Text | Primary description (type schema/318) |
| `foundingDate` | Date | `date.startDate` |
| `dissolutionDate` | Date | `date.endDate` |
| `member` | Role[] | Contributors as Person (ORCID) + Organisations as Organization (ROR), each wrapped in Role with position/role name and dates |
| `funder` | Role[] | Organisations with funder role (schema/186), same Role structure |
| `knowsAbout` | DefinedTerm[] | Subjects with `inDefinedTermSet` |

### 2. Sitemap includes non-HTML URLs

The sitemap currently includes `.json` and `.download` URLs alongside HTML landing pages. RDA's harvester expects pages containing JSON-LD, so these add unnecessary noise (roughly 2/3 of the 1,557 URLs).

### 3. No NESP-specific filtering

There is no NESP-specific tagging in the RAiD system. The sitemap includes all ~500 public RAiDs. This is likely fine - RDA can harvest all public RAiDs and the NESP Domain Data Portal can filter on its side, or Melanie can configure the crosswalk to select NESP records.

## Proposal

### Phase 1: Pre-LinkML (immediate, once-off)

Enrich the JSON-LD in the Astro template and filter the sitemap. All required data is already available in the Astro page props.

#### 1a. Enrich JSON-LD in `[prefix]/[suffix].astro`

Update the `<script type="application/ld+json">` block to match the `ResearchProjectFactory` mapping:

```json
{
  "@context": "https://schema.org",
  "@type": "ResearchProject",
  "@id": "https://raid.org/10.26259/0d7f1865",
  "identifier": {
    "@type": "PropertyValue",
    "propertyID": "https://registry.identifiers.org/registry/raid",
    "name": "RAiD",
    "value": "https://raid.org/10.26259/0d7f1865"
  },
  "parentOrganization": {
    "@type": "Organization",
    "@id": "https://ror.org/038sjwq14",
    "identifier": {
      "@type": "PropertyValue",
      "propertyID": "https://registry.identifiers.org/registry/ror",
      "name": "ROR",
      "value": "https://ror.org/038sjwq14"
    }
  },
  "description": "The aim of the study is to...",
  "foundingDate": "2025-05-26",
  "dissolutionDate": "2026-05-26",
  "member": [
    {
      "@type": "Role",
      "@id": "https://vocabulary.raid.org/contributor.position.schema/307",
      "roleName": "https://vocabulary.raid.org/contributor.position.schema/307",
      "startDate": "2025-05-26",
      "endDate": "2026-05-26",
      "member": {
        "@type": "Person",
        "@id": "https://orcid.org/0000-0002-4582-7728",
        "identifier": {
          "@type": "PropertyValue",
          "propertyID": "https://registry.identifiers.org/registry/orcid",
          "name": "ORCID",
          "value": "https://orcid.org/0000-0002-4582-7728"
        }
      }
    }
  ],
  "funder": [],
  "knowsAbout": [
    {
      "@type": "DefinedTerm",
      "@id": "https://linked.data.gov.au/def/anzsrc-for/2020/420399",
      "inDefinedTermSet": "https://vocabs.ardc.edu.au/viewById/316"
    }
  ]
}
```

#### 1b. Filter sitemap

Configure `@astrojs/sitemap` in `astro.config.mjs` to exclude `.json` and `.download` URLs:

```js
sitemap({
  filter: (page) => !page.endsWith('.json/') && !page.endsWith('.download/')
})
```

This reduces the sitemap from ~1,557 to ~520 URLs (HTML landing pages only).

#### 1c. Provide sample URL to Melanie Barlow

Once deployed, send Melanie:
- Sitemap URL: `https://static.prod.raid.org.au/sitemap-index.xml`
- Sample landing page for her to test JSON-LD extraction and RIF-CS crosswalk

### Phase 2: Post-LinkML

Once LinkML is deployed:
- The API can serve JSON-LD via content negotiation (`Accept: application/ld+json`), using the `ResearchProject` model generated from LinkML schemas
- The Astro site can call the API's JSON-LD endpoint instead of manually constructing JSON-LD
- Sitemap generation continues as-is (Astro `@astrojs/sitemap` handles it automatically on each build)
- If dynamic sitemap generation is needed (without a static build), a `/sitemap.xml` endpoint could be added to the API

### Effort Estimate

- **Phase 1a** (enrich JSON-LD): ~2 hours - template changes + unit tests for the JSON-LD helper
- **Phase 1b** (filter sitemap): ~30 minutes - one-line config change
- **Phase 2**: Covered by existing LinkML work

## JIRA

- [RAID-619](https://ardc.atlassian.net/browse/RAID-619) - Investigate how we can create a sitemap file for RDA pre- and post-LinkML
- [HELP-2753](https://ardc.atlassian.net/browse/HELP-2753) - RAiD Metadata in RDA - Initially for NESP Domain Data Portal
