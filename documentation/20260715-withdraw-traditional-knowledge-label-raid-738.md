# Withdraw traditionalKnowledgeLabel pending co-design (RAID-738)

* Date: 2026-07-15
* JIRA: [RAID-738](https://ardc.atlassian.net/browse/RAID-738)
* PR: [au-research/raid-au#568](https://github.com/au-research/raid-au/pull/568)
* ADR: [doc/adr/2026-07-15_withdraw-traditional-knowledge-label.md](../doc/adr/2026-07-15_withdraw-traditional-knowledge-label.md)

## What changed and why

`traditionalKnowledgeLabel` was accepted by the API and defined in the schema,
but it was never a deliberate release. It was exposed as a side effect of LinkML
schema generation, was never rendered in the app, and never actually persisted
(there was no write path; the read path returned `null`). The real design for
Traditional Knowledge / Biocultural labelling is still being co-developed with
Indigenous communities and researchers, so a live field that silently accepts
data is a governance risk. It has been withdrawn until the co-design concludes.

Raised via US RAiD demo feedback (Erin Robinson, Metadata Game Changers, July
2026); confirmed by Matthias as accidentally live rather than a deliberate
partial rollout.

### Schema (AC2)
- Removed the `traditionalKnowledgeLabel` slot, the `TraditionalKnowledgeLabel`
  class, and the `TraditionalKnowledgeLabelSchemaUriEnum` enum from the LinkML
  source (`raid-core.yaml`, `raid-extended.yaml`, `extended-enums.yaml`,
  `researchproject.yaml`) and the `buildSrc/extended-enum2table.yaml` mapping.
- Regenerated the JSON Schema, OpenAPI 3.1 (incl. strict) specs, and Java/TS
  models. Hand-edited the legacy, hand-maintained 3.0 spec to match.
- Deleted the now-orphaned service/factory/repository/exception classes and
  their unit tests.

### API (AC1) — reject, not ignore
Removing the field alone would make Spring's Jackson config silently drop it
(unknown properties are ignored by default), which is the same governance risk
in a new form. Instead, `TraditionalKnowledgeLabelRejectionAdvice` (a
`@ControllerAdvice` `RequestBodyAdvice`) inspects the raw body of mint/update
requests for a top-level `traditionalKnowledgeLabel` key and throws the existing
`ValidationException`, rendered by the existing `RaidExceptionHandler` as a
standard `ValidationFailureResponse` (**HTTP 400**,
`fieldId=traditionalKnowledgeLabel`). No new error shape was introduced.

Scope note: the type-based `supports()` also covers `POST /raid/post-to-datacite`
(operator endpoint, also takes `RaidUpdateRequest`). Rejecting the withdrawn
field there too is consistent and harmless — an intentional side effect.

### Database — preserved
Flyway migrations and the `traditional_knowledge_label`,
`traditional_knowledge_label_schema`, `raid_traditional_knowledge_label`
(and `traditional_knowledge_notice*`) tables are intentionally left untouched.
No data was dropped.

## Testing
- Unit tests for the advice: reject, pass-through when absent, malformed JSON
  pass-through, nested-key not rejected, empty body pass-through.
- Integration tests: mint returns 400, update returns 400, positive control
  mint without the field succeeds.
- Full `intTest` suite green (180 tests, 0 failures).

## AC3 — existing records with data in this field

A read-only check was run against a full local data dump (schema `api_svc`):

| Source | Rows checked | With `traditionalKnowledgeLabel` data |
|---|---|---|
| `raid_traditional_knowledge_label` (normalised join) | 0 | 0 |
| `raid.metadata` JSONB (incl. versioned rows) | 598 | 0 |
| `raid_archive.metadata` JSONB | 0 | 0 |
| `traditional_knowledge_label` (reference catalogue) | 30 | n/a — seeded reference data, not submissions |

No submitter has populated the field in this dataset. **This is a local dump,
not an authoritative query against live environments.** The same check must be
run against **test** and **prod** for sign-off. Resolving what to do with any
data found is explicitly out of scope for this ticket (flag for a decision).

### SQL to run against test / prod (schema `api_svc`)

```sql
-- 1. Normalised store: any raid linked to a TK label?
SELECT count(*) AS raids_with_tk_label
FROM api_svc.raid_traditional_knowledge_label;

-- 2. Detail, if the count above is > 0
SELECT rtkl.handle, l.uri AS label_uri, s.uri AS schema_uri
FROM api_svc.raid_traditional_knowledge_label rtkl
JOIN api_svc.traditional_knowledge_label l         ON l.id = rtkl.traditional_knowledge_label_id
JOIN api_svc.traditional_knowledge_label_schema s  ON s.id = rtkl.traditional_knowledge_label_schema_id
ORDER BY rtkl.handle;

-- 3. Legacy / raw JSONB metadata (covers pre-normalisation data)
SELECT count(*) FILTER (WHERE metadata::text ILIKE '%traditionalknowledge%') AS raid_json_hits
FROM api_svc.raid;
SELECT count(*) FILTER (WHERE metadata::text ILIKE '%traditionalknowledge%') AS archive_json_hits
FROM api_svc.raid_archive;
```

## Reinstating the field later
Restore the LinkML edits, regenerate, and delete the single advice class. No
controller or service changes are needed. See the ADR.

## Follow-ups (out of scope for this ticket)
- Regenerate the downstream generated TS types in `raid-agency-app`,
  `raid-aws-private`, and `raido-v2-aws-private`.
- Decide whether the published `raido-metadata-scheme-v1.json` document should
  also drop the field (it uses legacy key spellings and is not read by any test).
- Add pretty-printing to `buildSrc` `Utils.writeJsonTree` so strict-schema
  regeneration stops producing minified single-line diffs.
- Consider removing the stale `raido-metadata-scheme-v1.json` fixture.
