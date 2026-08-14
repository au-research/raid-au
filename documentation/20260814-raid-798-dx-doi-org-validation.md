# RAID-798: Accept dx.doi.org-formatted DOIs in relatedObject validation

- **JIRA:** [RAID-798](https://ardc.atlassian.net/browse/RAID-798) (Bug)
- **PR:** [au-research/raid-au#613](https://github.com/au-research/raid-au/pull/613)
- **Branch:** `feature/RAID-798`
- **Date:** 2026-08-14

## What changed and why

The RAiD API rejected DOI `relatedObject` ids submitted through the `dx.doi.org`
proxy host, accepting only the bare `doi.org` host. This surfaced in the RAiD US
pilot. Both `doi.org` and `dx.doi.org` are valid DOI proxy services maintained by
the DOI Foundation; `dx.doi.org` is no longer preferred but remains functional.

`DoiService.regex` was broadened from:

```
^https?://(doi\.org/10\..+|web\.archive\.org/.*)
```

to:

```
^https?://((dx\.)?doi\.org/10\..+|web\.archive\.org/.*)
```

The submitted form is stored **verbatim, without normalisation**, consistent with
the store-as-submitted decisions for Handle (RAID-786) and ARK (RAID-793). DataCite
handling is unaffected because it keys off `schemaUri`, not the submitted id. The
in-memory `DoiServiceStub` reuses `getRegex()`, so the change flows through the
stubbed test path automatically.

No database migration and no backfill were required (no existing RAiD could carry
this format, since it was previously rejected at validation).

## Tests

- **`DoiServiceTest`** (new, mirrors `RridServiceTest`): accepts both proxy hosts
  over `http`/`https`; asserts the resolver HEAD target is the submitted URL (proving
  no normalisation); resolver-404 maps to "uri not found"; regex regression guard
  rejects `dx.doi.org/not-a-doi`, an unsupported scheme, and an unrecognised host.
- **`RelatedObjectIntegrationTest`**: a `dx.doi.org` DOI mints and stores the id
  unchanged; a malformed `dx.doi.org/not-a-doi` is rejected with HTTP 400.
- **`TestConstants`** (testFixtures): added `DOI_SCHEMA_URI` and `VALID_DX_DOI`.

## Verification

- `./gradlew build` (compile + unit tests across modules): green.
- Full `:api-svc:raid-api:intTest`: green.
- **Live resolver check** against the real `dx.doi.org` proxy (the ticket's
  non-functional requirement): a known-good DOI (`10.1038/nphys1170`) returned
  `302 -> 200`; a known-bad DOI returned `404`. Behaviour is identical to `doi.org`,
  so the app validator behaves correctly against the live proxy.

## Follow-up

- Companion documentation task (Matthias): update metadata.raid.org to state that
  both `doi.org` and `dx.doi.org` are accepted, with `doi.org` preferred. Neither
  raid-skos nor metadata.raid.org currently documents the accepted proxy hosts.
