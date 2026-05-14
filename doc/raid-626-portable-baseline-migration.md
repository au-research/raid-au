# Remove hardcoded postgres ownership from baseline migration

## Problem

The baseline migration `B25__baseline.sql` in [raid-au](https://github.com/au-research/raid-au) hardcodes `postgres` as the owner of all database objects. This prevents deployment in environments where:

- The `postgres` role does not exist (e.g. a non-AWS managed database)
- The migration runner is a non-superuser who cannot execute `ALTER ... OWNER TO` for roles they don't belong to

A new registration agency needs to deploy the RAiD API on their own infrastructure, and their database admin user is not `postgres`.

## Proposed change

**File:** `api-svc/db/src/main/resources/db/migration/B25__baseline.sql`

Remove all hardcoded `postgres` references (281 lines deleted, 2 lines modified):

| Change | Count | Example |
|--------|-------|---------|
| Remove `ALTER ... OWNER TO postgres` statements | 47 | `ALTER TABLE api_svc.raid OWNER TO postgres;` |
| Remove `-- Name: ...; Owner: postgres` comments | 234 | `-- Name: raid; Type: TABLE; Schema: api_svc; Owner: postgres` |
| Fix `ALTER DEFAULT PRIVILEGES` statements | 2 | See below |

The two `ALTER DEFAULT PRIVILEGES` lines drop the `FOR ROLE postgres` clause so they apply to whatever user runs the migration:

**Before:**
```sql
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA api_svc GRANT USAGE ON SEQUENCES TO api_user;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA api_svc GRANT ALL ON TABLES TO api_user;
```

**After:**
```sql
ALTER DEFAULT PRIVILEGES IN SCHEMA api_svc GRANT USAGE ON SEQUENCES TO api_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA api_svc GRANT ALL ON TABLES TO api_user;
```

No application code changes. No new migrations. No schema changes.

## Security motivation

The `postgres` role is a PostgreSQL superuser — it bypasses all permission checks. Hardcoding it as the owner of application objects has two concerns:

- **Blast radius**: If the application were to connect as `postgres`, a SQL injection would grant an attacker superuser access — read any database, create roles, access the filesystem via `COPY`, or execute OS commands through extensions. Our deployment already mitigates this by using a restricted `api_user` role at runtime, but the baseline should not encode a dependency on a superuser.
- **Principle of least privilege**: Objects owned by a superuser cannot be modified by lesser roles without explicit grants, which couples schema management to the highest-privilege account. Removing the ownership statements allows a dedicated non-superuser migration role to own the application schema.

## Why this is safe

- The `OWNER TO` statements are cosmetic in practice — objects are already owned by whatever role created them during the original migration run
- Removing them means new deployments simply inherit ownership from the migration runner, which is the standard PostgreSQL behaviour
- The `-- Owner: postgres` lines are pg_dump metadata comments with no functional effect
- The `ALTER DEFAULT PRIVILEGES` change is equivalent when the current user *is* the user running migrations (which it always is in our deployments)

## Impact on existing environments

Modifying a baseline migration changes its Flyway checksum. Any environment that has already applied B25 will detect a checksum mismatch and Flyway will fail on next deploy.

**Resolution:** Run `flywayRepair` on each existing environment before merging. `flywayRepair` updates the stored checksum in `flyway_schema_history` without re-running any migrations — it is a metadata-only operation.

### Enabling flywayRepair

The Flyway Gradle plugin already provides a `flywayRepair` task, but it has no `doFirst` guard in `api-svc/db/build.gradle`. Add one to match the existing `flywayMigrate` and `flywayInfo` tasks:

```groovy
tasks.flywayRepair.doFirst {
  assert apiSvcPgPassword : "must set the apiSvcPgPassword"
  assert apiSvcRolePassword : "must set the apiSvcRolePassword"
  project.logger.lifecycle "flyway.locations:" + flyway.locations
}
```

### Deployment sequence

Flyway runs automatically at Spring Boot startup during deployment, so checksums must be repaired on all existing environments **before** deploying with the updated baseline.

1. **Before deploying** — run `flywayRepair` on every environment that has already applied B25. This updates the stored checksum in `flyway_schema_history` to match the modified baseline. It does not re-run any migrations or alter the database schema:
   ```
   ./gradlew flywayRepair
   ```
   Verify the repair was successful by checking that `flywayInfo` shows no checksum mismatches:
   ```
   ./gradlew flywayInfo
   ```
2. **Deploy** — Flyway runs `flywayMigrate` at startup. Because checksums were already repaired, Flyway recognises B25 as unchanged and the deploy succeeds.

### New environments

New environments that have never run B25 require no special handling. Flyway applies the baseline as-is — objects are owned by whatever database user runs the migration, with no dependency on a `postgres` role.

## Verification

- `./gradlew build` passes (no application code changes)
- `./gradlew flywayInfo` shows correct checksum for B25 after repair
- New deployments can run migrations without requiring a `postgres` role
