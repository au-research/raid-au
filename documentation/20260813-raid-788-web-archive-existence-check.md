# RAID-788: live Wayback existence check for web.archive.org relatedObjects

Date: 2026-08-13

## What changed and why

`RelatedObjectValidator` only validated the *format* of `web.archive.org`
relatedObject URLs (a 14-digit timestamp regex), never confirming the snapshot
existed. Real bad data slipped through: RAiD 10.71613/18ae7426 (NESP RL Project
2.2) used timestamp `14062026010101` (a DD-MM-YYYY date written as digits, not
YYYYMMDDHHMMSS). It passes the 14-digit regex but no Wayback snapshot exists, and
sixteen other Resilient Landscapes RAiDs shared the same malformed pattern.

### The change

- **New `WebArchiveService`** (implements `UriValidator` directly, not
  `AbstractUriValidator`). The Wayback availability API is GET+JSON and returns
  HTTP 200 even when nothing is archived, so the base class's HEAD/404 machinery
  does not apply; a class Javadoc records this rationale. Validation order:
  1. **Format check** (unchanged regex + message, so existing behaviour/tests hold).
  2. **Year sanity check** (protected helper): reject year < 1996 or > current year
     (from an injected `Clock`) as a clean `invalidValue` failure **before any
     network call**. This alone catches `14062026010101` (year 1406) with zero
     external traffic.
  3. **Availability call:** `GET https://archive.org/wayback/available?url=<enc>&timestamp=<ts>`,
     parsed as `JsonNode`; a snapshot exists iff `archived_snapshots.closest.available == true`
     and `closest.status` is 2xx/3xx. Otherwise a clean `uri not found` (400) failure.
  4. **Fail-closed (RAID-802/809):** timeouts / DNS / connection-refused / 5xx /
     non-404 4xx are surfaced as `ResolverUnavailableException` (HTTP 503 +
     `Retry-After`), never a validation failure.
- **In-memory `WebArchiveServiceStub`** (gated on `raid.stub.web-archive.enabled`)
  reuses the inherited format + year checks, then dispatches on sentinels; the
  server-error sentinel throws `ResolverUnavailableException` to exercise the real
  503 contract (unlike the older DOI/Handle/RRID stubs that predate RAID-809).
- **Wiring:** `RelatedObjectValidator` dispatch map now routes web-archive to
  `webArchiveService::validate`; `ExternalPidService` builds the `@Bean @Primary`
  (real vs stub) on the timeout-bounded `uriValidatorRestTemplate`; `StubProperties`
  gains a `webArchive` block. Config: `application.yaml` stub disabled by default +
  `raid.uri-validation.web-archive.availability-url`; `application-dev.yaml` and the
  intTest profile enable the stub.

## Scope and follow-ups

- **CDK companion [RAID-816]** (`raido-v2-aws-private` #37, merged): adds
  `raid.stub.web-archive.enabled` to the branch/test task definition (test = `true`,
  demo/stage/prod = `false`). Without it, branch/test deployments validated against
  the real archive.org and the stub-dependent intTests (esp. the 503 case) failed
  deterministically. The branch pipeline pulls this CDK from `main` (InfraSource),
  so #37 had to merge before the branch/test run could go green.
- **Out of scope:** fixing the existing bad data in the Resilient Landscapes hub
  (Matthias is handling that with their contact). Validation only runs on
  mint/update, so existing minted RAiDs are not retroactively rejected on read; an
  update that re-submits a bad web-archive relatedObject will now be rejected.
- **Behaviour note:** stage/prod validate against real archive.org. The check runs
  inside the concurrent I/O-bound validator set, adding one bounded (5s/10s) call.

## Testing

- Unit: new `WebArchiveServiceTest` (15 — year boundaries, no-snapshot, 2xx/3xx/4xx
  statuses, null body, URL-encoding, timeout/5xx/non-404 -> 503) and
  `WebArchiveServiceStubTest` (5); updated `RelatedObjectValidatorTest` adds the
  previously-missing web-archive dispatch coverage. `./gradlew :api-svc:raid-api:test`
  green.
- Integration: new `RelatedObjectIntegrationTest` cases — valid snapshot,
  non-existent snapshot (400), implausible-year (400, no external call, the AC3
  regression guard), resolver-unavailable (503). Full `:api-svc:raid-api:intTest`
  green locally.
- Branch/test pipeline (after #37 merged): all web-archive cases pass in the test
  environment, including the 503 case (which only passes with the stub active,
  confirming RAID-816 took effect). The Api-Integration-Test stage is currently red
  only on 3 pre-existing, branch-wide IAM group-SPI 401 failures ("Group Service
  Point Admin" / "Migration endpoint"), which fail identically on unrelated branches
  (bug/RAiD-790) and are unrelated to this change.

## Reviews

- api-code-reviewer: one blocker (missing `@Value` default on the availability-url,
  which would fail the intTest context) fixed with an inline default; URL-encoding
  and null-body test suggestions folded in. No remaining blocking issues.

## Links

- Parent story: [RAID-788](https://ardc.atlassian.net/browse/RAID-788) (epic
  [RAID-789](https://ardc.atlassian.net/browse/RAID-789), resolver availability hardening)
- Sub-task: [RAID-816](https://ardc.atlassian.net/browse/RAID-816) (branch/test CDK stub)
- PR (api): https://github.com/au-research/raid-au/pull/612
- PR (CDK): https://github.com/au-research/raido-v2-aws-private/pull/37
