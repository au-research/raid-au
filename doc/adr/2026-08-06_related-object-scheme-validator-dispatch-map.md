### Related object schemaUri validation via a scheme-to-validator dispatch map

* Status: final
* Who: proposed and finalised by RL
* When: 2026-08-06
* Related: RAID-789 (parent), RAID-786 (Handle validator story), RAID-699/RPDB-62 (relatedObject schemaUri follow-up)


# Decision

`RelatedObjectValidator` previously dispatched on `relatedObject.schemaUri`
through a hard-coded if/else with a two-item allow-list
(`https://doi.org/`, `https://web.archive.org/`). Adding Handle support
(`https://hdl.handle.net/`) would have made it a three-way if/else, and the
enum and OpenAPI specs already carry further schemes (`arks.org`,
`scicrunch.org`, `isbn-international.org`) that the validator actively rejects
today. Rather than grow the conditional again, RAID-786 refactors the dispatch
into a **scheme-to-validator map**.

1. **Dispatch map keyed by schemaUri value.** A
   `Map<String, BiFunction<String, String, List<ValidationFailure>>>` maps each
   supported `schemaUri` string to a `(id, fieldId) -> failures` function.
   Entries: `doi.org -> doiService::validate`,
   `hdl.handle.net -> handleService::validate`, and
   `web.archive.org -> ` a local lambda running the inline
   `WEB_ARCHIVE_URL_PATTERN` regex (web-archive has no resolver call, so it
   keeps its regex-only behaviour). DOI and Handle are resolver-backed
   validators extending `AbstractUriValidator`.

2. **Built in the constructor, not as a Spring bean.** The map is assembled in
   the `RelatedObjectValidator` constructor from the injected `DoiService` and
   `HandleService` as a `LinkedHashMap` wrapped with
   `Collections.unmodifiableMap` (insertion order preserved so the
   "unsupported schemaUri" error lists schemes deterministically). It is not
   exposed as a bean like `spatialCoverageUriValidatorMap` because the
   web-archive entry is a local regex lambda with no injectable collaborator;
   making it a bean would split a single-owner concern across
   `RelatedObjectValidator` and `ExternalPidService` for no benefit.

3. **Allow-list derived from the map's key set.** The set of supported schemes
   and the "unsupported schemaUri" error message are both generated from
   `map.keySet()`, so adding a scheme is a one-line map entry rather than
   touching a separate constant and an error string.

4. **No cross-scheme rerouting.** A DOI-shaped id (`10.xxxx/...`) declared under
   `schemaUri = https://hdl.handle.net/` is dispatched to `handleService`, never
   to `doiService`. DOIs are Handles; when the caller declares the Handle scheme
   we validate against the Handle resolver as declared. This is enforced by a
   unit test asserting `doiService.validate(...)` is never invoked in that case.

# Alternatives considered

* **Keep the if/else, add one more branch.** Rejected — the conditional was
  already carrying two schemes plus a separate allow-list constant and a
  hand-maintained error string; each added scheme touches three places and the
  divergence risk grows.
* **Expose the map as a Spring `@Bean`** (mirroring `spatialCoverageUriValidatorMap`).
  Rejected for this validator — see point 2; the web-archive regex lambda has no
  injectable collaborator, so a bean would fragment ownership without benefit.
* **Special-case DOI-shaped Handles and route them to `DoiService`.** Rejected —
  contradicts the "validate as declared" principle and the ticket's explicit
  no-rerouting requirement (per Matthias's 2026-07-27 decision).

# Consequences

* Adding a new resolver-backed related-object scheme is now: implement the
  validator (extending `AbstractUriValidator`), register its bean in
  `ExternalPidService`, and add one entry to the dispatch map. The allow-list
  and error message update automatically.
* Behaviour is otherwise unchanged and backward-compatible: blank id ->
  `NOT_SET`, null schemaUri -> `NOT_SET`, previously-supported DOI and
  web-archive paths behave exactly as before, and adding Handle is purely
  additive (input previously rejected is now accepted).
* The remaining spec-declared schemes (`arks.org`, `scicrunch.org`,
  `isbn-international.org`) are still not wired in; they now slot into the map
  as follow-up work rather than another if/else branch.
* Resolver-failure handling (timeouts, DNS, 5xx) is inherited from the
  RAID-802-hardened `AbstractUriValidator`; the map does not change that path.
