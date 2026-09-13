# RAID-793: ARK (arks.org) validator for relatedObject.schemaUri

- **JIRA (Story):** [RAID-793](https://ardc.atlassian.net/browse/RAID-793)
- **JIRA (parent epic):** [RAID-789](https://ardc.atlassian.net/browse/RAID-789)
- **PR (raid-au):** [#616](https://github.com/au-research/raid-au/pull/616)
- **PR (CDK companion, raido-v2-aws-private):** [#38](https://github.com/au-research/raido-v2-aws-private/pull/38) (merged)
- **Date:** 2026-08-18

## What changed

Added validation for ARK related-object identifiers (`relatedObject.schemaUri = https://arks.org/`). ARKs were previously rejected because `https://arks.org/`, although already seeded in the `related_object_schema` vocabulary (V29) and present as `SchemaValues.ARKS_SCHEMA`, was not registered in the `RelatedObjectValidator` dispatch map.

New and changed code (raid-au):

- **`service/ark/ArkService.java`** (new) — the validator. Implements `UriValidator` directly (following the `WebArchiveService` precedent) because its resolver semantics do not fit the `AbstractUriValidator` HEAD/404 model.
- **`service/ark/ArkServiceStub.java`** (new) and stub wiring — `raid.stub.ark.enabled`, `StubProperties.Ark`, `InMemoryStubTestData` constants, `ExternalPidService` bean.
- **`Api.java`** — new `arkResolverRestTemplate` bean with redirect-following disabled (needed to inspect the `Location` header), same bounded timeouts as the shared resolver template.
- **`validator/RelatedObjectValidator.java`** — registered `https://arks.org/` in the dispatch map.
- **Tests** — `ArkServiceTest` (unit) and `ArkRelatedObjectIntegrationTest` (intTest).

No Flyway migration was required. DataCite mapping already emits `relatedIdentifierType = "ARK"` and is regression-covered.

## Why this approach (and why not the ticket's original design)

The ticket originally specified a dual-resolver model: try the published ARK domain first, fall back to n2t.net, accept if either resolves. Live-resolver verification (recorded as a comment on RAID-793) showed that model is no longer valid:

- The ARK Alliance migrated the global resolver from **n2t.net to arks.org**. n2t.net now blindly 302-redirects **every** syntactically-valid ARK, real or fabricated, to arks.org. It gives no existence signal, so an n2t.net fallback would accept invalid ARKs.
- Published Name Mapping Authority (NMA) domains (for example `ark.bnf.fr`, `gallica.bnf.fr`) are bot-protected and return 403 to non-browser clients, so a server-side HEAD/GET cannot rely on a 200.

The implemented design is a single-resolver **NAAN-registry check** against arks.org:

1. Format check: fully-qualified `https://arks.org/ark:[/]NAAN/name`, NAAN 5 to 9 digits, no bare ARKs (regex anchored with `\z`).
2. No-follow GET to arks.org and inspect the redirect `Location` (resolved against the request URI):
   - redirects **off-host** to a real NMA (host is not `arks.org` and not `*.arks.org`) -> registered NAAN -> **accept**;
   - **self-loops** back to the arks.org registrable domain (or a null/relative host after resolution) -> unregistered NAAN -> **reject** (400).
3. Fail-closed per RAID-809: client errors -> 400; downstream/resolver failures (timeout, DNS, 5xx, unparseable or missing `Location`, or a template-breaking URI) -> `ResolverUnavailableException` (503). The validator never falsely rejects on a transient outage.

This validates NAAN registration, not exact-object existence (object-level checks are infeasible because the NMAs are bot-protected). The submitted URL is stored as-is and never rewritten.

## Deployment dependency

The new `raid.stub.ark.enabled` flag required a companion in the CDK (raido-v2-aws-private): `branch-api-stack.ts` and `config/environment-properties.ts`, set to `true` for test/branch and `false` for demo/stage/prod, mirroring the RAID-816 web-archive-stub companion. Without it, the branch and test environments run the ARK intTests against the real arks.org resolver and fail deterministically (the stub-only 503 path).

The companion (PR #38) was merged to `main`, then the branch stack (`BranchApi-raid-793`) was deployed manually with `cdk deploy` (merging to main does not self-deploy). The Branch-Build-Deploy pipeline for `feature/RAID-793` is green (execution `6ce2510b`) with the ARK intTest passing and `raid.stub.ark.enabled=true` confirmed live on the branch task definition.

## Testing

- Full unit test suite green, including `ArkServiceTest` (registered/unregistered NAAN, relative and protocol-relative and subdomain self-loops, off-host redirect with `/.info/` in path, format failures, and the 503 fail-closed paths).
- Full intTest suite green locally: 202 tests, 0 failures.
- Branch pipeline intTests green after the CDK stub companion was deployed.
- Live-resolver verification (NFR) performed and documented on RAID-793.

## Follow-ups (not in this PR)

- Update the SKOS definition for arks.org and the public docs to say ARK is now accepted and validated via arks.org NAAN registration (not via resolving the published URL).
- The `metadata.raid.org` ARK entry is Matthias's action.
