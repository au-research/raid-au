# Web Archive Related Object Integration Tests

**Date:** 2026-04-14

## What Changed

Added integration tests for the web archive related object validation feature introduced in Flyway migration V39. The `RelatedObjectValidator` supports `https://web.archive.org/` as a related object `schemaUri` and validates URLs against the pattern `https://web.archive.org/web/<14-digit-timestamp>/<url>`.

### Files Changed

- `api-svc/raid-api/src/intTest/java/au/org/raid/inttest/RelatedObjectIntegrationTest.java` — new integration test class with 4 scenarios
- `api-svc/testFixtures/src/testFixtures/java/au/org/raid/fixtures/TestConstants.java` — added constants for web archive and related object type/category vocabulary URIs

### Test Scenarios

| # | Test | Input | Expected |
|---|------|-------|----------|
| 1 | Valid web archive URL | `https://web.archive.org/web/20220101000000/https://example.com` | 201 - RAiD minted |
| 2 | Invalid web archive URL | `https://web.archive.org/foo/bar` | 400 - validation failure on `relatedObject[0].id` |
| 3 | Unsupported schemaUri | `https://example.com/` | 400 - validation failure on `relatedObject[0].schemaUri` |
| 4 | Missing inner URL | `https://web.archive.org/web/20220101000000/https://` | 400 - validation failure on `relatedObject[0].id` |

## Why

The web archive schema support was added without integration test coverage. These tests verify the full request lifecycle (API request -> validation -> response) and were manually confirmed against all three environments (local, test, demo).

## Links

- **PR:** https://github.com/au-research/raid-au/pull/429
- **Branch:** `defect/related-object-schemaURI-weborigin`
