# RAID-583: SPARQL retry + cache fallback for schema generation

## What changed

`./gradlew generateAllV2` was intermittently failing with a
`NullPointerException` in `Utils.parseQueryResults` because the ARDC demo
SPARQL endpoint (fronted by Cloudflare) returned HTTP 429 after ~30
consecutive requests. The rate-limit response body was plain text, not the
expected JSON envelope.

Three fixes, all in `buildSrc`:

1. **`Utils.queryValues` now throttles, retries, and falls back to cache.**
   - 500 ms minimum spacing between SPARQL calls (global `AtomicLong`).
   - Up to 5 attempts per call. 429 honours the `Retry-After` header up to
     60 s; 5xx/network errors use exponential backoff up to 30 s.
   - On success, writes the raw SPARQL JSON envelope to
     `api-svc/datamodel/sparql-cache/<enumID>.json`.
   - On total failure, reads the cache. Throws a descriptive `IOException`
     if both fail.

2. **`parseQueryResults` no longer NPEs on malformed responses.** It throws
   `IOException` with a truncated body preview when `results` or
   `results.bindings` is missing, so rate-limit errors produce an actionable
   message instead of a stack trace.

3. **Cache format switched to raw SPARQL envelope.** Previously
   `sparql-cache/` held bare URI lists (committed in `283643a6`, never wired
   up). Storing the raw envelope means one cache entry serves both
   `includePrefLabel=true` (examples generator) and `=false` (SQL/JSON Schema
   generators). Legacy bare-list files are rejected at read time.

## Why

Keeps the build pointed at `demo.vocabs.ardc.edu.au` — the active v2
vocabulary curation endpoint — while surviving Cloudflare rate-limiting.
Retry + throttle handle transient rate-limits; cache fallback handles the
case where the endpoint is temporarily unavailable after a previous healthy
run.

## Affected files

- `buildSrc/src/main/java/au/raid/org/api/Utils.java` — rewrote
  `queryValues`, extracted `buildQuery`, `postSparqlQuery`, `readCache`,
  `writeCache`, `throttle`, `exponentialBackoffMs`. Added
  `RateLimitedException`. `parseQueryResults` now throws `IOException` on
  malformed input.
- `buildSrc/src/main/java/au/raid/org/api/GenerateReferenceDataTask.java` —
  added `cacheDir` input; passes it plus `enumID` to `queryValues`.
- `buildSrc/src/main/java/au/raid/org/api/AddStaticEnums.java` — same.
- `api-svc/datamodel/build.gradle` — wires `cacheDir = file("sparql-cache")`
  into `generateReferenceDataV2`, `generateExtendedReferenceDataV2`, and
  `generateStrictJsonSchemaV2`.
- `buildSrc/build.gradle` — added JUnit 5 test deps and
  `test { useJUnitPlatform() }`.
- `buildSrc/src/test/java/au/raid/org/api/UtilsTest.java` — new, 11 tests
  covering parsing, query building, cache read, legacy format rejection.
- `api-svc/datamodel/sparql-cache/*.json` — all 27 files re-seeded with raw
  SPARQL JSON envelope.

## Verification

- `./gradlew :buildSrc:test` — 11 tests pass.
- `./gradlew :api-svc:datamodel:generateReferenceDataV2 :api-svc:datamodel:generateExtendedReferenceDataV2`
  — both tasks succeed in ~27 s total, no rate-limit errors, 27 cache files
  written in raw SPARQL envelope format.

## Links

- JIRA parent: https://ardc.atlassian.net/browse/RAID-461
- JIRA sub-task: https://ardc.atlassian.net/browse/RAID-583
- ADR: [`doc/adr/2026-04-17_sparql-cache-fallback.md`](../doc/adr/2026-04-17_sparql-cache-fallback.md)
- PR: _pending_
