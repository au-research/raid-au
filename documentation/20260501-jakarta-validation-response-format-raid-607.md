# RAID-607: Fix Jakarta Bean Validation Response Format

## What changed

Overrode `handleMethodArgumentNotValid` in `RaidExceptionHandler` so that Jakarta Bean Validation errors return `ValidationFailureResponse` (with a `failures` list) instead of Spring's default `ProblemDetail` format.

## Why

OpenAPI-generated model classes have Jakarta validation annotations (`@NotNull`, `@Pattern`, `@Valid`). When these annotations trigger validation errors before the custom `ValidationService`, Spring was returning its default `ProblemDetail` response — which has no `failures` field. Clients deserializing `RaidApiValidationException.getFailures()` got `null`, breaking the API contract.

## Key decisions

- **Standardized messages**: Always use `"field must be set"` for `notSet` and `"field has an invalid value"` for `invalidValue`, ignoring Jakarta's default messages (e.g., `"must not be null"`)
- **Blank value semantics**: A `@Pattern` failure on a blank/null value maps to `notSet`, not `invalidValue` — a blank value is semantically "not set"
- **Constraint code mapping**: `NotNull`, `NotBlank`, `NotEmpty` → `notSet`; everything else → `invalidValue`
- **ObjectError handling**: Non-field errors use `objectName` as `fieldId` and default to `invalidValue`

## Files changed

- `api-svc/raid-api/src/main/java/au/org/raid/api/endpoint/raidv2/RaidExceptionHandler.java` — added override and two helper methods
- `api-svc/raid-api/src/test/java/au/org/raid/api/endpoint/raidv2/RaidExceptionHandlerTest.java` — 10 unit tests

## Links

- JIRA: [RAID-607](https://ardc.atlassian.net/browse/RAID-607) (sub-task of [RAID-461](https://ardc.atlassian.net/browse/RAID-461))
- PR: [#444](https://github.com/au-research/raid-au/pull/444) (target: `feature/RAID-461`)
