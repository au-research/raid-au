# RAID-885: Web archive links checked with a HEAD, not the availability API

- JIRA: [RAID-885](https://ardc.atlassian.net/browse/RAID-885) (Bug, from HELP-3215)
- PR: [#699](https://github.com/au-research/raid-au/pull/699)
- Sub-task: [RAID-935](https://ardc.atlassian.net/browse/RAID-935), only re-validate new or changed related objects on update, plus a success cache (branch `feature/RAID-935`, PR into `feature/RAID-885`)
- Follow-up: [RAID-933](https://ardc.atlassian.net/browse/RAID-933), other URI validators double-encode percent-escaped identifiers
- Related: RAID-788 (original check), RAID-854 (encoding and timestamp fixes), RAID-809 (503 for unavailable resolvers)

## Problem

Users intermittently got `relatedObject[n].id: uri not found` when saving genuinely archived
`web.archive.org` links. Failures included captures years old, and links that had saved
successfully before failed again on an unrelated edit, because every save re-validates every
related object.

Prod had been running 2.17.0, which includes the RAID-854 fix, since 11 September 2026. The
reported failures on 15 September therefore came from a separate defect.

## Root cause

The Wayback availability API (`archive.org/wayback/available`) answers
`HTTP 200 {"archived_snapshots":{}}` when its own internal CDX lookup times out. That response is
identical to its answer for a page that was never archived, so the validator returned a 400
rather than a 503. This was reproduced live with the HELP-3215 URL: a normal request returned the
11 September capture, and a request with `timeout=0.001` returned the empty response.

## What changed

- `WebArchiveService` extends `AbstractUriValidator` and sends a HEAD to the user's own
  `web.archive.org/web/<timestamp>/<url>` link:
  - 2xx and 3xx mean valid. An inexact timestamp gets a 302 to the closest capture, and HEAD
    redirects are not followed.
  - 404 means `uri not found`.
  - 429, other 4xx, 5xx and timeouts mean 503.
- Links are percent-encoded before sending, and existing `%XX` escapes are kept:
  - Non-ASCII characters were otherwise sent as raw UTF-8, which the archive rejects with 400.
  - Spaces, `|` and `{}` are encoded rather than rejected, so existing records stay updatable.
- `AbstractUriValidator` has a new `headRequest` hook, so this validator can send a
  `java.net.URI`. The other validators are unchanged (see RAID-933).
- `WebArchiveServiceStub` applies the same format rules as the real service.
- The `raid.uri-validation.web-archive.availability-url` property was removed. CDK never set it.
- The ADR `doc/adr/2026-08-06_related-object-scheme-validator-dispatch-map.md` now has a note
  saying the web archive check is resolver-backed.

## Evidence

Live probes, 5 October 2026, using the HELP-3215 URL and a never-archived control:

| Check | Archived | Never archived |
|---|---|---|
| Availability API, forced timeout | 200, empty | 200, empty |
| CDX API | correct, but 19 to 51 s, one 503 | correct |
| HEAD on the playback link (4 runs each) | 200, under 1.2 s | 404, under 0.7 s |
| Non-ASCII link (`.../wiki/München`) | raw UTF-8: 400, encoded: 302 | |

## Testing

- Unit tests: 957 tests, 0 failed, 17 skipped.
- intTest in stub mode: 253 tests, 0 failed, 16 skipped.
- intTest `RelatedObjectIntegrationTest` with the stub disabled, against the live archive:
  6 of 7 web archive tests pass. The one failure is the stub-only `server-error` sentinel, which
  the real archive correctly answers with 404.

## RAID-935: skip unchanged links and cache successful checks

The HEAD check alone still left two parts of the HELP-3215 report unsolved:
- Every update re-checked every saved link, so an unrelated edit could fail because of a link
  that was already accepted.
- A rate-limited save was blocked with a 503.

A bounded probe on 5 October 2026 (sequential HEADs from one office IP) found that
web.archive.org throttles by refusing TCP connections, not with a 429. It tripped after about 20
requests at roughly 2.7 s spacing, and after about 14 back-to-back requests. The block cleared in
under a minute.

What changed:
- On update, the external check is skipped for each related object whose exact
  `(schemaUri, id)` is in the version being edited. That version is read with
  `RaidHistoryService.findByHandleAndVersion`, the same call `RaidService.update` uses. The local
  checks (format, timestamp year, type, category, allow-list) still run on every item. If the
  stored version can't be read, everything is checked, as before.
- A per-instance Caffeine cache records successful checks, so a save retried after a 503 skips
  links already confirmed. Failures and 503s are never cached. The defaults (30 minutes,
  10,000 entries) were chosen, not measured. The cache is off under the `dev` profile, so
  intTests prove the stored-version skip end to end.
- `UriValidator` has a new `validateLocally` method, and the related-object dispatch map now
  holds validator objects.
- Decisions: links saved before checks existed are skipped like any other stored link, and the
  change is recorded in `doc/adr/2026-10-05_verify-related-object-links-on-entry.md`.
- RAID-793 (ARK, PR #616) must implement `validateLocally` when the two meet; this is noted on
  that ticket.

Testing (RAID-935, local):
- Unit tests: 1001 tests, 0 failed, 17 skipped.
- intTest (dev profile, cache off): 256 tests, 0 failed, 16 skipped. The new update tests use a
  stub link that passes on its first check and returns 503 on any later one, so re-checking an
  unchanged link would fail them. They are written to pass with the cache on as well, which is
  how branch environments run.

## Still to do

- Merge, release, and confirm the change on the prod image before closing the ticket.
- Add a changelog entry in the next release.
- Tell the HELP-3215 reporter once the change is in prod.
