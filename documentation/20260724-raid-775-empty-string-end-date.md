# RAID-775: InvalidDateException when minting with a cleared end date

- **JIRA:** [RAID-775](https://ardc.atlassian.net/browse/RAID-775) (Bug, standalone — no sub-tasks)
- **PR:** [au-research/raid-au#582](https://github.com/au-research/raid-au/pull/582)
- **Date:** 2026-07-24

## Problem

Minting a RAiD failed with a `400 InvalidDateException` (`" is an invalid date or has an unsupported format."`, note the leading blank) when a user set a start date and then cleared the end date. The frontend clears an end date by submitting an empty string `""` rather than omitting the field or sending `null`. End dates are optional in the metadata schema, so this should mint successfully (consistent with the intent of RAID-595 and RAID-708).

## Root cause

Two independent defects, at different layers:

1. **Validator date guards (Java).** `DateUtil.parseDate("")` throws because an empty string matches none of its date regexes. Several validators guarded `parseDate` calls with `== null` (or no guard) instead of the blank-safe `StringUtil.isBlank` used elsewhere, so `""` was passed straight into `parseDate`. Affected: `DateValidator` (overall date — the path in the reported reproduction), `TitleValidator`, `ContributorPositionValidator`, `ContributorTypeValidator`, `ContributorValidator`.

2. **Stray `@Pattern` on `Title.endDate` (datamodel).** The generated `Title` model uniquely carried `@Pattern(regexp = "^\\s*\\S.*$")` on `endDate` (an accidental copy from `Title.startDate`/`text`); `Date`, `ContributorPosition` and `OrganisationRole` did not. This constraint runs at the controller `@Valid` boundary, before `TitleValidator`, and rejected `""` outright. This was surfaced by the integration test and is a separate, pre-existing issue from defect 1.

## What changed

- Replaced `== null` / unguarded date checks with `StringUtil.isBlank` across the five validators, so a blank date string (null, empty, or whitespace) is treated as absent. A blank *required* start date now correctly raises the existing NOT_SET failure rather than being silently accepted or throwing.
- Refactored `TitleValidator.validatePrimaryTitleDates` to resolve dates into `LocalDate` before sorting, removing an unrelated NPE risk on a raw nullable `startDate`.
- Removed the stray `pattern` from `Title.endDate` in the LinkML datamodel (`api-svc/datamodel/src/v2/raid-core.yaml`) and regenerated the JSON schemas (`raid-jsonschema.json`, `raid-strict-jsonschema.json`) and OpenAPI specs (`raid-openapi-3.1.yaml`, `raid-openapi-strict-3.1.yaml`). `Title.startDate` retains `@NotNull @Pattern`. Removing a pattern is a relaxation, so the API contract change is backward-compatible.

## Testing

- Unit tests added for empty-string dates across all affected validators; full `raid-api` unit suite passes.
- Integration test `RaidIntegrationTest.mintRaidWithEmptyStringEndDates` mints a RAiD with cleared (`""`) end dates on the overall date, title, contributor position and organisation role, and asserts success (Acceptance Criteria Scenario 1). Verified passing against the local Docker + Keycloak stack.

## Follow-up noted (not in scope)

`datamodel/build.gradle`'s `generateStrictJsonSchemaV2` (`AddStaticEnums`) can silently rewrite `raid-strict-jsonschema.json` to empty when re-triggered by a plain `./gradlew build`, because it depends on the sparql-cache to inject enum values. Worth its own ticket.
