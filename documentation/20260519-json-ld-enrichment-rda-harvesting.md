# JSON-LD Enrichment for RDA Harvesting

**Date:** 2026-05-19

## What Changed

Enriched the JSON-LD embedded in raid detail pages on the Astro static site (`raid-agency-app-static`) to output full schema.org `ResearchProject` markup for RDA harvester consumption. Previously the JSON-LD was a minimal stub; now it maps contributors, organisations, funders, subjects, descriptions, and dates to their schema.org equivalents.

Also filtered the sitemap to exclude `.json/` and `.download/` URLs that aren't HTML pages.

Added vitest as a test framework to `raid-agency-app-static` (which previously had no tests) with 13 unit tests covering the JSON-LD mapping.

## Why

RAID-619 / RAID-639: The RDA harvester needs rich structured data to index RAiD records. The JSON-LD output mirrors the existing Java `ResearchProjectFactory` implementation in the API backend.

## JIRA Tickets

- Parent: [RAID-619](https://ardc.atlassian.net/browse/RAID-619)
- Related: [RAID-639](https://ardc.atlassian.net/browse/RAID-639)

## Pull Request

- [PR #454](https://github.com/au-research/raid-au/pull/454)

## Key Files

- `raid-agency-app-static/src/utils/json-ld.ts` — core mapping function
- `raid-agency-app-static/src/utils/json-ld.test.ts` — 13 unit tests
- `raid-agency-app-static/src/pages/raids/[prefix]/[suffix].astro` — wired JSON-LD into template
- `raid-agency-app-static/astro.config.mjs` — sitemap filter
- `raid-agency-app-static/vitest.config.ts` — new test config
