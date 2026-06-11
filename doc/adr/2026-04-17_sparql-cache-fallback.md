### Schema generation resilience via SPARQL retry + read-through cache

* Status: final
* Who: proposed and finalised by RL
* When: 2026-04-17
* Related: RAID-461 (parent), RAID-583 (implementation)


# Decision

`./gradlew generateAllV2` calls the ARDC demo SPARQL endpoint 30+ times to hydrate
dynamic enums used by reference-data SQL, JSON Schema, and example generation.
The endpoint is fronted by Cloudflare and starts returning HTTP 429
(`error code: 1015`) after roughly 30 consecutive requests, which previously
surfaced as an opaque NPE in `Utils.parseQueryResults` when the rate-limit
response body was fed into the parser.

Three changes have been made to `buildSrc/.../Utils.queryValues`:

1. **Global throttle.** All SPARQL calls go through a single static mutex that
   enforces a minimum 500 ms gap between requests. With 27 enums this adds
   ~13 s total, but in practice avoids the 429 wall entirely.

2. **Retry with backoff.** Up to 5 attempts per call. HTTP 429 honours the
   `Retry-After` header (seconds) up to 60 s; 5xx and network errors use
   exponential backoff (2 s, 4 s, 8 s, 16 s, capped at 30 s).

3. **Read-through cache fallback.** On success, the raw SPARQL JSON envelope
   is written to `api-svc/datamodel/sparql-cache/<enumID>.json`. If all retry
   attempts fail, the cached envelope is parsed instead. Storing the raw
   envelope means one cache entry serves both `includePrefLabel=true` (examples)
   and `includePrefLabel=false` (SQL/JSON Schema) consumers.

Non-2xx and non-envelope responses no longer cause a NullPointerException —
`parseQueryResults` explicitly rejects missing `results`/`bindings` with a
descriptive `IOException`, so failures are actionable.


# Context

The endpoint `demo.vocabs.ardc.edu.au/.../raid_raid-controlled-lists-v2_1-7-0`
holds the current working copy of the RAiD controlled-lists vocabulary.
Moving to a more permissive endpoint was considered but rejected — the demo
instance is where the vocabulary is actively curated for v2, so the schema
generator has to keep reading from it.

A `sparql-cache/` directory was committed earlier (commit `283643a6`, part of
RAID-461) but never wired up to a consumer, and held bare URI lists rather
than the raw SPARQL envelope. Those files have been replaced with raw
envelopes; legacy bare-list files are rejected at read time.


# Consequences

- Builds are resilient to transient Cloudflare rate-limiting: if the network
  is flaky but a previous successful run seeded the cache, the build still
  produces correct output.
- Cache files are source-controlled. They must be re-seeded when the upstream
  vocabulary changes — running `generateReferenceDataV2` +
  `generateExtendedReferenceDataV2` against a healthy endpoint overwrites all
  27 files in ~30 seconds.
- The 500 ms throttle adds ~13 s to a cold build run. Acceptable cost.
- Build failures now propagate as `IOException` with the enum ID and cache
  file path, making rate-limit vs genuine outage distinguishable from the
  error message.


# Links

- Parent ticket: https://ardc.atlassian.net/browse/RAID-461
- Implementation ticket: https://ardc.atlassian.net/browse/RAID-583
- Cloudflare 1015 error reference: https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1015/
