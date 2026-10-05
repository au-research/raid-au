# RAID-885: Web archive links checked with a HEAD, not the availability API

- JIRA: [RAID-885](https://ardc.atlassian.net/browse/RAID-885) (Bug, from HELP-3215)
- PR: [#699](https://github.com/au-research/raid-au/pull/699)
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

## Still to do

- Merge, release, and confirm the change on the prod image before closing the ticket.
- Add a changelog entry in the next release.
- Tell the HELP-3215 reporter once the change is in prod.
