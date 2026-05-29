# RAID-652: Decouple JOOQ Schema for Branch Deployments

## What Changed

Three changes enable schema-per-branch database isolation for the branch pipeline (RAID-647):

1. **JOOQ codegen** (`api-svc/db/build.gradle`): Added `outputSchemaToDefault = true` so generated SQL omits the `api_svc` schema qualifier and relies on the JDBC connection's `search_path`.

2. **Datasource URL** (`api-svc/raid-api/src/main/resources/application.yaml`): Appended `?currentSchema=${spring.flyway.default-schema}` to set the connection's search_path at connect time.

3. **Generated code** (`ApiSvc.java`): Schema name changed from `"api_svc"` to `""` (empty string), matching `outputSchemaToDefault` behavior.

## Why

Branch pipeline deployments (RAID-647) need each feature branch to use its own Postgres schema in the shared test RDS instance. Previously, JOOQ hardcoded `api_svc` in all generated SQL, making schema isolation impossible without code changes.

## Backward Compatibility

Existing deployments are unaffected: `spring.flyway.default-schema` defaults to `api_svc` (unchanged). Branch deployments override this to a branch-specific schema name (e.g., `raid_123`).

## JIRA Tickets

- Parent: [RAID-647](https://ardc.atlassian.net/browse/RAID-647) — Branch Pipeline CDK construct
- This task: [RAID-652](https://ardc.atlassian.net/browse/RAID-652) — JOOQ outputSchemaToDefault
- Related: [RAID-653](https://ardc.atlassian.net/browse/RAID-653) — currentSchema in datasource URL

## PR

- https://github.com/au-research/raid-au/pull/458
