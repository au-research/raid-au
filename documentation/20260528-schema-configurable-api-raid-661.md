# RAID-661: Make API Schema-Configurable for Branch Deployments

## What Changed and Why

The RAiD API had a hardcoded dependency on the `api_svc` Postgres schema. Branch deployments (RAID-647) require each branch to use its own isolated schema. This change makes the schema configurable at runtime.

### Changes

1. **Flyway migrations** (32 files): Removed all `api_svc.` schema qualifiers from SQL statements. Flyway's `default-schema` property now controls which schema migrations target.

2. **Datasource URL** (`application.yaml`): Added `?currentSchema=${spring.flyway.default-schema}` to the JDBC URL. This sets the Postgres `search_path` at the connection level, so all unqualified table references resolve to the correct schema.

3. **JOOQ runtime config** (`JooqConfig.java`): Added a `DefaultConfigurationCustomizer` bean that sets `renderSchema(false)`. This prevents JOOQ from emitting `api_svc.` prefixes in generated SQL, letting the connection's `currentSchema` control resolution instead.

4. **Baseline migration** (`B25__baseline.sql`): Removed `SET search_path TO DEFAULT` statement that would have reset the schema context after Flyway set it.

5. **Schema privilege grant** (`db/env/test/R__grant_api_user_schema_access.sql`): Added a Flyway repeatable migration that dynamically grants `api_user` access to the current schema. The existing `V2_1__api_user_permission.sql` hardcodes grants only for `api_svc`, so branch schemas never received the necessary privileges — causing 500 errors on all database queries. The repeatable migration grants on both existing objects (`GRANT ALL ON ALL TABLES`, `GRANT USAGE ON ALL SEQUENCES`) and future objects (`ALTER DEFAULT PRIVILEGES`). Placed in `db/env/test/` so it only runs in the test environment where branch deployments exist.

### Backward Compatibility

Existing deployments are unaffected: `spring.flyway.default-schema` defaults to `api_svc`. Branch deployments override this via environment variable.

## JIRA Tickets

- Parent: [RAID-647](https://ardc.atlassian.net/browse/RAID-647) — Branch Pipeline CDK Construct
- This task: [RAID-661](https://ardc.atlassian.net/browse/RAID-661) — Make API schema-configurable

## Pull Requests

- https://github.com/au-research/raid-au/pull/468 (schema-agnostic migrations, JOOQ, datasource URL)
- https://github.com/au-research/raid-au/pull/488 (api_user schema privilege grant for branch deployments)
