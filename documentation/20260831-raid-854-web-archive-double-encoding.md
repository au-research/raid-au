# RAID-854: Web Archive validation rejected every genuinely archived URL

**Date:** 2026-08-31
**JIRA:** [RAID-854](https://ardc.atlassian.net/browse/RAID-854) (Bug, epic [RAID-789](https://ardc.atlassian.net/browse/RAID-789))
**PR:** https://github.com/au-research/raid-au/pull/634
**Source:** HELP-3170, reported by Aline Andrade (UWA, NESP Resilient Landscapes Hub); reproduced on demo by Matthias 2026-08-27

## What was wrong

Submitting a `relatedObject` whose id is a `web.archive.org` URL failed with:

```json
{"fieldId": "relatedObject[0].id", "errorType": "invalidValue", "message": "uri not found"}
```

even for snapshots that genuinely exist.

`WebArchiveService` (added in RAID-788) percent-encodes the archived url so it survives as a
single `url` query-parameter value, then passed the finished request as a **String** to
`RestTemplate.exchange`. `RestTemplate`'s String overloads treat their argument as a URI
*template* and encode it a second time, so `%3A%2F%2F` went out on the wire as `%253A%252F%252F`:

```
actual:   ...?url=https%253A%252F%252Fhealthycountryai.org%252Ffiles%252F...pdf&timestamp=...
expected: ...?url=https%3A%2F%2Fhealthycountryai.org%2Ffiles%2F...pdf&timestamp=...
```

The Wayback availability API has no snapshot of a url spelled like that, so it answered
`200 {"archived_snapshots": {}}` — which this validator reads as "no snapshot" — instead of
returning the real capture. Verified directly against the live service: the correctly encoded
request returns the snapshot, the double-encoded one returns an empty `archived_snapshots`.

This affected **every** web.archive.org related object, not just this one URL. It was invisible to
the existing tests because the unit tests mock `RestTemplate` (so no template expansion happens)
and the integration tests use `WebArchiveServiceStub` (so no HTTP happens at all).

## What changed

- `WebArchiveService.buildAvailabilityUrl` now returns a `java.net.URI` built with
  `URI.create(...)` instead of a String. Handing `RestTemplate` a `URI` skips template expansion
  entirely, so the encoding built here is sent verbatim. This also removes a latent crash on any
  archived url containing `{` or `}`, which the String overload would have read as a template
  variable.
- Regression test `availabilityRequestIsNotDoubleEncoded` binds a real `RestTemplate` to
  `MockRestServiceServer` and asserts on the URI that actually reaches the transport, using the
  exact URL from the ticket. A mocked `RestTemplate` cannot catch this class of bug; the mock
  server can, because it sits below the URI template handler.
- The existing tests were updated from `anyString()`/`ArgumentCaptor<String>` to the `URI`
  overload.

## Not changed (deliberately)

The validator accepts the *closest* snapshot the availability API returns rather than requiring an
exact timestamp match. For the reported URL the closest capture is `20250330052536`, not the
requested `20260827054756`. That leniency is correct here — Wayback itself serves the nearest
capture for a `/web/<timestamp>/` URL, so requiring an exact match would reject URLs that resolve
perfectly well in a browser. Flagged here only so the behaviour is a recorded decision rather than
an accident.

## Verification

- `./gradlew :api-svc:raid-api:test` — green.
- `./gradlew :api-svc:raid-api:intTest` — full suite green against a locally running API.
- Manual: live `https://archive.org/wayback/available` queries confirming the correctly encoded
  request returns the snapshot and the double-encoded one does not.
