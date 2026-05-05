# RAID-556: Fix HTTP 500 when ORCID profile has null family name

## What changed and why

When updating a RAiD that included a contributor whose ORCID profile had no family name (e.g., sandbox ORCID `0009-0009-5506-7601`), the API returned an HTTP 500 error. The root cause was a `NullPointerException` in `OrcidClient.getName()` which called `.getValue()` directly on nullable `familyName` and `givenNames` fields from the ORCID API response.

### Changes

- **`OrcidClient.getName()`** — Replaced direct `.getValue()` calls with `Optional.ofNullable()` null-safe access. Returns only the non-null name parts joined by a space, or an empty string if both are null.
- **`OrcidClientTest`** — Added 3 unit tests covering null family name, null given names, and both null.
- **MockServer expectations** — Added HEAD and GET expectations for ORCID `0009-0009-5506-7601` with a null `family-name` response.
- **`OrcidNullFamilyNameIntegrationTest`** — New integration test that mints a RAiD with the null-family-name ORCID, reads it back, and updates it, verifying the full create/read/update cycle succeeds.

## JIRA tickets

- [RAID-556](https://ardc.atlassian.net/browse/RAID-556) — Receiving HTTP 500 error when trying to save demo RAiD with a particular sandbox ORCID

## Pull request

- [PR #378](https://github.com/au-research/raid-au/pull/378) — RAID-556: Add null-safe handling for ORCID profile names
