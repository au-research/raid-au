# RAID-704: Allow line breaks in Description text field

## What changed

The `Description.text` field in the RAiD API rejected values containing line breaks (`\n`). The root cause was the regex pattern `^\s*\S.*$` in the LinkML schema, where `.` does not match `\n` in ECMA 262 / Java regex. Changed the pattern to `^\s*\S[\s\S]*$` so that `[\s\S]` matches any character including newlines.

### Files modified

- `api-svc/datamodel/src/v2/raid-core.yaml` (source of truth)
- `api-svc/datamodel/generated/v2/raid-jsonschema.json` (generated, committed)
- `api-svc/datamodel/generated/v2/raid-strict-jsonschema.json` (generated, committed)
- `api-svc/idl-raid-v2/src/raid-openapi-3.1.yaml` (generated, committed)
- `api-svc/idl-raid-v2/src/raid-openapi-strict-3.1.yaml` (generated, committed)
- `api-svc/raid-api/src/intTest/java/au/org/raid/inttest/DescriptionIntegrationTest.java` (3 new tests)

### Why only Description.text?

13 fields in `raid-core.yaml` use the same `^\s*\S.*$` pattern. Only `Description.text` was changed because it is the only field where multi-line input is a valid use case. Other fields (titles, notes, identifiers) are single-line by design.

## JIRA

- [RAID-704](https://ardc.atlassian.net/browse/RAID-704)

## PR

- https://github.com/au-research/raid-au/pull/534
