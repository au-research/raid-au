### Withdraw traditionalKnowledgeLabel pending Indigenous co-design

* Status: final
* Who: proposed and finalised by RL
* When: 2026-07-15
* Related: RAID-738

# Context

`traditionalKnowledgeLabel` was accepted by the API and defined in the schema,
but it was never an intentional release. The field was exposed as a side effect
of LinkML schema generation, it was never rendered in the app, and — as it turns
out — there was never a write path: mint and update accepted the field into the
generated request model but no service ever persisted it, and the read path
returned it as `null`.

The real design for Traditional Knowledge / Biocultural labelling in RAiD is
still being co-developed with Indigenous communities and researchers
(governance model, display, and alignment with the Local Contexts / DataCite
crosswalk). Leaving a live field that silently accepts data is a governance
risk: a submitter could believe a label they set carries protections or meaning
that have not yet been designed. The issue was surfaced via US RAiD demo
feedback (Erin Robinson, Metadata Game Changers, July 2026) and confirmed by
Matthias as accidentally live rather than a deliberate partial rollout.

# Decision

Remove `traditionalKnowledgeLabel` from the schema and API surface, and reject
any request that still carries it, rather than silently ignoring it. Preserve
the database so nothing is lost before the co-design concludes.

1. **Remove from the LinkML source and regenerate.** The slot, the
   `TraditionalKnowledgeLabel` class, and the
   `TraditionalKnowledgeLabelSchemaUriEnum` enum were removed from the LinkML
   source (`raid-core.yaml`, `raid-extended.yaml`, `extended-enums.yaml`,
   `researchproject.yaml`) and the `buildSrc/extended-enum2table.yaml` mapping.
   The JSON Schema, OpenAPI 3.1 (incl. strict) specs, and generated Java/TS
   models were regenerated; the legacy hand-maintained 3.0 spec was edited to
   match. The now-orphaned service/factory/repository/exception classes were
   deleted.

2. **Reject on submission with HTTP 400.** Because Spring's Jackson config
   ignores unknown properties by default, removing the field alone would make
   the API *silently drop* it — the same governance risk in a new form. A
   `RequestBodyAdvice` (`TraditionalKnowledgeLabelRejectionAdvice`,
   `@ControllerAdvice` extending `RequestBodyAdviceAdapter`) inspects the raw
   body of `RaidCreateRequest`/`RaidUpdateRequest` submissions for a top-level
   `traditionalKnowledgeLabel` key and throws the existing `ValidationException`,
   which the existing `RaidExceptionHandler` renders as a standard
   `ValidationFailureResponse` (HTTP 400, `fieldId=traditionalKnowledgeLabel`).
   No new error-response shape was introduced.

3. **Preserve the database.** The Flyway migrations and the
   `traditional_knowledge_label`, `traditional_knowledge_label_schema`, and
   `raid_traditional_knowledge_label` tables (and the `traditional_knowledge_notice*`
   tables) are left untouched. A data check across a full local dump found the
   reference catalogue seeded but zero submitter-populated records; the same
   check should be run against test and prod for authoritative sign-off
   (see RAID-738 / the completion doc).

# Alternatives considered

* **Silently ignore the field** (remove from schema only): rejected — silent
  acceptance is the exact governance risk called out. The submitter gets no
  signal the value was dropped.
* **Global `FAIL_ON_UNKNOWN_PROPERTIES=true`**: rejected — rejects *every*
  unknown property on *every* endpoint, breaking forward/backward compatibility
  for other clients. Too broad.
* **Drop the database tables**: rejected — existing data (if any) must be
  preserved for a decision once co-design concludes.
* **Value-level validation in the service layer**: impossible once the field is
  removed from the generated model — the typed request no longer carries it, so
  the check must happen on the raw body.

# Consequences

* Reinstating the field once co-design concludes is: restore the LinkML edits,
  regenerate, and delete the single advice class. No controller/service changes.
* The type-based `supports()` also covers `POST /raid/post-to-datacite`
  (operator endpoint, also takes `RaidUpdateRequest`); rejecting the withdrawn
  field there too is consistent and harmless.
* Follow-ups (out of scope): regenerate the downstream TS types in
  `raid-agency-app`, `raid-aws-private`, and `raido-v2-aws-private`; decide
  whether the published `raido-metadata-scheme-v1.json` document should also
  drop the field; and add pretty-printing to `buildSrc` `Utils.writeJsonTree`
  so strict-schema regen stops producing minified diffs.
