# RAID-584: Fix Integration Tests for LinkML Enum Refactoring

## What changed

Fixed 5 failing integration tests caused by the LinkML enum refactoring on `feature/RAID-461`. The refactoring changed OpenAPI-generated model fields from `String` to typed enums (e.g., `ContributorSchemaUriEnum`, `TitleTypeIdEnum`, `RelatedObjectSchemaUriEnum`).

## Root causes and fixes

1. **ContributorValidator.java** — `isOrcid()` and `isIsni()` compared enum `.getSchemaUri()` directly with a String via `.equals()`, which always returned `false`. Fixed by adding `.getValue()` before `.equals()`.

2. **RaidHistoryIntegrationTest.java** — `getPrimaryTitle()` compared `TitleTypeIdEnum` with a String constant. Fixed by adding `.getValue()`.

3. **ContributorsIntegrationTest.java** — `nullPositions` test expected a custom validation message, but Jakarta `@NotNull` on the `position` field now triggers before the custom validator. Updated test expectations to match Jakarta's response format.

4. **RelatedObjectIntegrationTest.java** — `unsupportedSchemaUri` test expected a `schemaUri` validation failure, but with schemaUri now an enum, unsupported values can't be passed. Updated test to use a valid enum value with an invalid `id`, expecting an `id` validation failure instead.

5. **DataciteRelatedIdentifierFactoryTest.java** — Enum constant renamed from `HTTPS_ARCHIVE_ORG_` to `HTTPS_WEB_ARCHIVE_ORG_`.

## New unit tests

- `ContributorValidatorTest.isOrcidMatchesOnSchemaUri()` — verifies ORCID routing when schemaUri enum is set but id doesn't have the ORCID prefix
- `ContributorValidatorTest.isIsniMatchesOnSchemaUri()` — verifies ISNI routing when schemaUri enum is set but id doesn't have the ISNI prefix

## Files changed

- `api-svc/raid-api/src/main/java/au/org/raid/api/validator/ContributorValidator.java` — bug fix
- `api-svc/raid-api/src/intTest/java/au/org/raid/inttest/ContributorsIntegrationTest.java` — test fix
- `api-svc/raid-api/src/intTest/java/au/org/raid/inttest/RaidHistoryIntegrationTest.java` — test fix
- `api-svc/raid-api/src/intTest/java/au/org/raid/inttest/RelatedObjectIntegrationTest.java` — test fix
- `api-svc/raid-api/src/test/java/au/org/raid/api/factory/datacite/DataciteRelatedIdentifierFactoryTest.java` — enum rename
- `api-svc/raid-api/src/test/java/au/org/raid/api/validator/ContributorValidatorTest.java` — 2 new tests

## Links

- JIRA: [RAID-584](https://ardc.atlassian.net/browse/RAID-584) (sub-task of [RAID-461](https://ardc.atlassian.net/browse/RAID-461))
