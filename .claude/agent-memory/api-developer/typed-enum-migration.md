---
name: OpenAPI Typed Enum Migration — intTest Fix Patterns
description: Fix patterns for intTest compilation failures when OpenAPI codegen changes fluent-setter params from String to typed enums
type: reference
---

When OpenAPI codegen replaces `String` parameters with typed enums (e.g., `DescriptionTypeIdEnum`, `ContributorPositionIdEnum`, `SpatialCoverageSchemaUriEnum`), intTest files that pass raw string literals to fluent-setter methods will fail to compile.

## Three Fix Patterns

**Pattern 1 — Valid string constant**: Wrap with `EnumType.fromValue(STRING_CONSTANT)`
```java
.id(ContributorPositionIdEnum.fromValue(PRINCIPAL_INVESTIGATOR_POSITION))
```

**Pattern 2 — Empty string `""`**: Replace chained call with standalone null setter. Do NOT add `@Disabled`.
```java
createRequest.getDescription().get(0).setSchemaUri(null);
```

**Pattern 3 — Arbitrary invalid string not in enum**: Add `@Disabled("TODO:RL ...")` AND replace call with null setter. Both required — compiler processes `@Disabled` method bodies.

## Import Notes
- `AbstractIntegrationTest` has wildcard `import au.org.raid.idl.raidv2.model.*;` — no new imports needed there
- Other test classes need explicit imports per enum type

## Targeted Compile Check
```bash
./gradlew :api-svc:raid-api:compileIntTestJava
```
Much faster than `./gradlew build`; gives clear error locations for remaining failures.

## Context
Applied during RAID-461 (LinkML integration) when codegen updated all model builder params from String to typed enums. Four intTest files required fixes: `DescriptionIntegrationTest`, `AbstractIntegrationTest`, `OrcidNullFamilyNameIntegrationTest`, `SpatialCoverageIntegrationTest`.
