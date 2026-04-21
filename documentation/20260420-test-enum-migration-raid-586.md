# RAID-586: Update unit tests for schema URI enum changes

## What changed and why

Commit `534ecb14` (RAID-461) replaced String `schemaUri` fields with LinkML-generated enum types across ~50+ production files but left test code using String constants, resulting in 100 compilation errors.

This change updates all test code and remaining production code to use the new enum types:

- **12 production files** updated to use enum comparisons instead of String-based comparisons:
  - `AccessValidator.java` — removed hardcoded String constants, uses `AccessTypeIdEnum`
  - `DataciteRelatedIdentifierFactory.java` — typed maps using `RelatedObjectSchemaUriEnum`, `RelatedObjectTypeIdEnum`, `RelatedObjectCategoryIdEnum`, `RelatedRaidTypeIdEnum`
  - `RaidService.java` — enum-based access type comparison
  - `RaidAuthorizationService.java`, `SchemaValues.java`, `DescriptionValidator.java`, `SubjectValidator.java`, `TitleValidator.java`, Datacite factories

- **~80 test files** updated to use enum values instead of String literals

- **3 JSON test fixtures** updated with correct enum-compatible values

## Verification

- `./gradlew compileTestJava` — BUILD SUCCESSFUL
- `./gradlew :api-svc:raid-api:test` — BUILD SUCCESSFUL (all unit tests pass)
- `./gradlew compileIntTestJava` — BUILD SUCCESSFUL

## JIRA tickets

- Parent: [RAID-461](https://ardc.atlassian.net/browse/RAID-461) — Replace string schemaUri fields with LinkML-generated enums
- Subtask: [RAID-586](https://ardc.atlassian.net/browse/RAID-586) — Update unit tests for schema URI enum changes

## PR

PR not yet created — changes committed on `feature/RAID-461` branch.
