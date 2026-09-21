# Deployment configuration reference

This page lists the configuration the RAiD API needs when it is deployed, what
each property is for, and which defaults must be overridden.

It is written for any registration agency deploying the RAiD API, and covers the
API service (`api-svc`) only. Keycloak configuration is described in
[`iam/doc/keycloak-configuration.md`](../../iam/doc/keycloak-configuration.md).

Two things are referred to throughout:

- **raid.org** is the registration authority. It operates the services shared by
  all registration agencies, such as the ORCID integration.
- **RAiD AU** is the Australian registration agency. It is one agency among
  several, with no special status in the deployment model.

This codebase originated at RAiD AU, and a number of defaults in
`application.yaml` still carry RAiD AU values as a result. That is an artefact of
authorship rather than a statement that RAiD AU is a reference deployment, and
those defaults are being made neutral. Until they are, every agency other than
RAiD AU has to override them, so they are called out explicitly below. RAiD AU's
own configuration appears here as a worked example for the same reason: it is the
deployment the defaults were drawn from, so the contrast is the clearest way to
show what needs setting.

> **Which release this describes.** Service Point ID block allocation arrived in
> **2.17.0** and changed how the agency's ROR is configured. Where behaviour
> differs, this page describes 2.17.0 and later and says what 2.16.0 and earlier
> do instead. Check the release being deployed before following the ROR guidance,
> because the two behave in opposite ways when the property is unset.

## How configuration is supplied

The API is a Spring Boot application, so every property below can be supplied as
an environment variable, a JVM system property, or an entry in a configuration
file. Spring's
[relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html)
rules apply, so `raid.db.host` and `RAID_DB_HOST` are equivalent.

The defaults live in
[`api-svc/raid-api/src/main/resources/application.yaml`](../../api-svc/raid-api/src/main/resources/application.yaml).

> **Do not base a deployment on `application-dev.yaml`.** That profile points the
> API at the mock server in this repository's Docker Compose setup, which exists
> only for local development.

## Three traps to know about first

As noted above, several defaults carry RAiD AU values rather than neutral ones.
Three groups of them cause problems in any other deployment, and two fail quietly
rather than loudly.

### 1. One default points at a mock server

`raid.orcid-integration.host` defaults to `http://localhost:1080`, the local
development mock server. Nothing listens there in a deployed environment, so the
call fails with `Connection refused`.

The call currently happens after the DataCite mint but inside the same database
transaction, so a failure mints a real DOI and then rolls the database change
back. Set this property before minting anything. Both the default and the
transaction behaviour are being fixed.

### 2. Some defaults identify RAiD AU as the agency

These are the quiet ones. The API starts normally and minting succeeds, but the
records name RAiD AU as the registration agency rather than the one running the
deployment.

| Property | Ships as | Why it matters |
| --- | --- | --- |
| `datacite.registration-agency-name` | `Australian Research Data Commons` | Sent to DataCite with each record. |
| `raid.iam.realm-uri` | `https://iam.${raid.environment}.raid.org.au/realms/raid` | A RAiD AU hostname. |
| `raid.identifier.landing-prefix` | `https://static.${raid.environment}.raid.org.au/raids/` | A RAiD AU hostname. |

**On 2.16.0 and earlier, `raid.identifier.registration-agency-identifier` belongs
in this list too, and is the worst of them.** It defaults to RAiD AU's ROR, so an
instance that does not set it attributes every RAiD it mints to RAiD AU and
routes ORCID contributor updates to RAiD AU's API. Nothing reports this.

From 2.17.0 the property has no default and the failure is loud instead: see the
section below.

Service point rows carry an owning organisation as well. Check that
`identifier_owner` on each service point holds the correct ROR for the
deployment, rather than one inherited from a seeded row.

### 3. `raid.environment` is not a Spring profile

`raid.environment` selects a folder of environment-specific database migrations:

```
spring.flyway.locations = classpath:db/env/api_user,classpath:db/migration,classpath:db/env/${raid.environment}
```

It is a separate property from `spring.profiles.active`, and setting it does not
activate a Spring profile. Setting a Spring profile does not set it either,
except for the `dev` profile, which sets `raid.environment: dev` as a side
effect.

Every registration agency is expected to run a `demo` and a `prod` environment,
so those two names mean the same thing in every deployment and `raid.environment`
should name the environment it is actually running as. The `dev`, `test` and
`stage` folders cover RAiD AU's own internal pipeline and are not part of that
shared convention.

### Creating a new environment

Set `raid.environment` to `demo` or `prod` to match the environment being
created. Against an empty database that is safe.

Most of what those two folders contain is one-off repair of RAiD AU data, but
none of it does anything to a database with no rows in it. Every migration in
`db/env/prod` is an `UPDATE` that matches nothing. In `db/env/demo`, the two
migrations that would insert or delete real data sit below the baseline
(`baseline-version: 25`, applied from `B25__baseline.sql`) and so never run on a
fresh database, and the remainder match nothing. The one migration that does
take effect is `V42.1`, which inserts the sandbox ORCID contributor schema row
that a demo environment needs.

So a demo environment should use `db/env/demo` rather than skip it.

### Do not use `dev`

`db/env/dev` is a RAiD AU developer environment and is not part of the shared
convention. Unlike the others it does change an empty database: `V40.1` inserts
a service point carrying RAiD AU's ROR and placeholder DataCite credentials that
authenticate only against the local mock server.

An agency that sets `raid.environment=dev` therefore starts with a service point
that appears usable, attributes RAiDs to RAiD AU, and cannot authenticate to
DataCite. `test` and `stage` are likewise RAiD AU-internal.

### Two things to be careful of later

These do not affect creating an environment, but are worth knowing.

The repairs are written for RAiD AU's data, and some embed RAiD AU hostnames.
`V36.1`, present in both shared folders, rewrites `raid_history` entries to
`https://static.<env>.raid.org.au/raids/`. Harmless against no rows, but not
against a populated database, so take care if migrations are ever applied after
restoring a dump rather than before loading data.

The minor version numbers are positional rather than meaningful, and are reused
across folders for unrelated changes. `V40.1` is `delete_demo_raids` in `demo`
and `fix_raid_history_schema_uris` in `prod`, and `V36.1` has a different
checksum in each folder. `flyway_schema_history` records only the version and
checksum, so changing `raid.environment` after an environment exists will fail
validation with a checksum mismatch or `Detected resolved migration not
applied`. Choose the value when the environment is created and leave it alone.

### Sandbox ORCID contributors

Whether a contributor can be saved depends on two things agreeing.
`raid.contributor-validation.orcid.schema-uri` declares which ORCID namespace is
accepted, and `ContributorService` separately requires a matching row in the
`contributor_schema` table, refusing the save when there is none. The property is
ordinary configuration; the row arrives through a migration.

`V42.1` inserts the row for `https://sandbox.orcid.org/`. It is present in
`db/env/demo` and deliberately absent from `db/env/prod`, which matches the
convention that demo points at the ORCID sandbox and production at real ORCID.
Using the matching environment folder therefore gets this right automatically.

If the environment folder is skipped, or a production environment is pointed at
the sandbox, the row is missing and every sandbox contributor save fails with a
generic 500 rather than an error naming the missing schema. Insert it directly
in that case:

```sql
insert into contributor_schema (uri, status)
select 'https://sandbox.orcid.org/', 'active'::schema_status
where not exists (
    select 1 from contributor_schema where uri = 'https://sandbox.orcid.org/'
);
```

## The agency's ROR must be allocated a Service Point ID block

**From 2.17.0.** On 2.16.0 and earlier there is no register, no allocation is
needed, and `service_point.id` starts at a hardcoded `20000000` on every
instance, so Service Point IDs are not unique between agencies. Upgrading across
this boundary renumbers existing Service Points into the allocated block, so an
agency already running 2.16.0 or earlier should read this section before
upgrading rather than after.

`raid.identifier.registration-agency-identifier` has no default. It is the ROR of
the registration agency operating the instance, and the instance resolves its own
Service Point ID range from it at startup, against
[`registration-agencies.yaml`](../../api-svc/raid-api/src/main/resources/registration-agencies.yaml).

Each agency gets a distinct numeric block so that Service Point IDs are unique
across the federation, because the ID is published as
`identifier.owner.servicePoint` in RAiD metadata. A block's first ID is
`block * blockSize`, and there is nothing significant about the number of digits.

Two failures follow from this, both at startup:

- the property is unset, or
- the ROR is set but does not appear in `registration-agencies.yaml`

Allocations are made by the RAiD Registration Authority and recorded in each
agency's legal agreement. `registration-agencies.yaml` is the authoritative copy
in software, so onboarding an agency is a pull request against this repository
plus a release. It is **not** something an agency can set in its own deployment
configuration. Confirm the allocation is present in the release being deployed
before standing an environment up.

## Required properties

Set all of these. Anything not listed keeps its shipped default.

### Identity

| Property | Description |
| --- | --- |
| `raid.identifier.registration-agency-identifier` | The registration agency's ROR. From 2.17.0 it has no default and must be allocated a Service Point ID block; on 2.16.0 and earlier it silently defaults to RAiD AU's ROR. See above. |
| `datacite.registration-agency-name` | The registration agency's name, as sent to DataCite. Defaults to RAiD AU's name. |
| `raid.identifier.landing-prefix` | Prefix for the RAiD landing page URLs the deployment serves. Defaults to a RAiD AU hostname. |

`raid.identifier.name-prefix` and `raid.identifier.schema-uri` both default to
`https://raid.org/` and are federation-wide constants rather than per-agency
settings. Leave them alone.

### Database

| Property | Description |
| --- | --- |
| `raid.db.host` | PostgreSQL hostname. |
| `raid.db.port` | PostgreSQL port. |
| `raid.db.name` | Database name. Defaults to `raido`. |
| `raid.db.user` | Application database user. |
| `spring.flyway.locations` | See trap 3. |

The datasource URL is built automatically and appends `?currentSchema=api_svc`.
If `spring.datasource.url` is overridden by hand, keep that parameter or every
query fails with `relation "raid" does not exist`.

### Authentication

| Property | Description |
| --- | --- |
| `raid.iam.realm-uri` | The deployment's Keycloak realm URI. Also used as the OAuth2 issuer. |
| `spring.security.oauth2.client.registration.keycloak.client-id` | API client ID. RAiD AU uses `raid-api-2`. |
| `raid.raid-permissions.client-id` | Permissions admin client ID. RAiD AU uses `raid-permissions-admin`. |
| `raid.cors.origins` | Origins allowed to call the API, normally the agency app's URL. |

### DataCite

| Property | Description |
| --- | --- |
| `datacite.endpoint` | DOI minting endpoint. Test is `https://api.test.datacite.org/dois`. |

`raid.repository-client.url` is the repositories endpoint used to manage
repository accounts. It defaults to `https://api.test.datacite.org/repositories`
and only needs overriding for a production deployment.

Per-service-point DataCite credentials live in the `service_point` table, not in
configuration.

### ORCID integration

The ORCID integration is a shared service operated by raid.org on behalf of all
registration agencies. Agencies do not deploy their own copy, and do not need
their own ORCID member credentials for it.

| Property | Description |
| --- | --- |
| `raid.orcid-integration.host` | The raid.org ORCID integration host for the relevant environment. |
| `raid.orcid-integration.api-key` | The API key issued by raid.org for this deployment. Set it once raid.org has issued one. |
| `raid.contributor-validation.orcid.url-prefix` | `https://orcid.org/`, or `https://sandbox.orcid.org/` for sandbox. |
| `raid.contributor-validation.orcid.schema-uri` | Must match the url-prefix above. |

Contributor status flows back into RAiD records only once the deployment is
registered with raid.org. Registration requires:

- the agency's ROR, which must match the one its service points assert
- the RAiD API base URL, including the trailing slash
- the realm's token endpoint
- a confidential Keycloak client in the agency's realm with service accounts
  enabled, whose service account holds the realm role `contributor-writer`

raid.org uses the client credentials grant to obtain a token, then reads RAiDs by
contributor and patches contributor status. The `contributor-writer` role grants
exactly that and nothing wider. Send the client secret through a one-time link
rather than email.

### External resolvers

| Property | Description |
| --- | --- |
These all have workable defaults. Override them only where noted.

| Property | Description |
| --- | --- |
| `raid.orcid-client.base-url` | ORCID API. Defaults to the sandbox, so a production deployment must override it. |
| `raid.ror-client.base-url` | ROR API. The default is current. |
| `raid.ror-client.client-id` | ROR API client ID. A RAiD AU value ships as the default; obtain a separate one. |
| `raid.isni-client.url-format` | ISNI SRU query URL. The default is current. |

### Optional

| Property | Default | Description |
| --- | --- | --- |
| `raid.datacite.resync.enabled` | `false` | Background re-push of records flagged for DataCite re-sync. |
| `raid.history.baseline-interval` | `50` | How often a full history baseline is written. |
| `logging.level.root` | `info` | Root log level. `logging.level.<package>` can raise or lower individual loggers. |
| `raid.stub.<resolver>.enabled` | `false` | In-memory stub for one external resolver. Leave off in a deployed environment. |

The stub switches exist for local development and integration tests, and the
resolvers are `ark`, `doi`, `geonames`, `handle`, `isni`, `openstreetmap`,
`orcid`, `ror`, `rrid` and `web-archive`. They already default to `false`, so a
deployment need not set them; RAiD AU sets all ten explicitly so the value is
visible in the environment rather than implied.

## Secrets

Supply these through a secret store rather than plain configuration. The RAiD AU
deployment injects them into the container from AWS Secrets Manager, separately
from the non-secret environment.

| Property | Description |
| --- | --- |
| `raid.db.password` | Application database password. |
| `raid.db.encryption-key` | Encrypts service point DataCite passwords at rest. Changing it makes existing rows undecryptable. |
| `spring.flyway.user` | Database user that runs migrations. Often more privileged than the application user. |
| `spring.flyway.password` | Password for the migration user. |
| `datacite.user` | DataCite account used for minting. |
| `datacite.password` | Password for the above. |
| `raid.repository-client.username` | DataCite repository administration account. |
| `raid.repository-client.password` | Password for the above. |
| `raid.repository-client.email` | Contact email for the above. |
| `spring.security.oauth2.client.registration.keycloak.client-secret` | API OIDC client secret. |
| `raid.raid-permissions.client-secret` | Permissions admin client secret. |
| `raid.orcid-client.access-token` | ORCID API access token. |
| `raid.validation.geonames.username` | GeoNames account used for spatial coverage validation. |

## Worked example: the RAiD AU demo environment

This is included as one agency's working configuration, not as a template to
copy. It is defined in
[`registration-agency/cdk/config/environment-properties.ts`](https://github.com/au-research/raido-v2-aws-private/blob/main/registration-agency/cdk/config/environment-properties.ts),
with the secret wiring in
[`api-service.ts`](https://github.com/au-research/raido-v2-aws-private/blob/main/registration-agency/cdk/lib/raid/construct/ecs/api-service.ts).
Those repositories are private; request access if the detail is useful. The
values below are the ones the running container actually has, read from its task
definition, with the database hostname omitted. It is running a build later than
2.17.0, so it sits on the far side of the release boundary noted at the top of
this page.

```
raid.environment                               = demo
spring.flyway.locations                        = classpath:db/migration,classpath:db/env/api_user,classpath:db/env/demo

raid.identifier.registration-agency-identifier = https://ror.org/038sjwq14
raid.identifier.landing-prefix                 = https://static.demo.raid.org.au/raids/

raid.db.name                                   = raido
raid.db.port                                   = 5432
raid.db.user                                   = api_user

raid.iam.realm-uri                             = https://iam.demo.raid.org.au/realms/raid
raid.raid-permissions.client-id                = raid-permissions-admin
raid.cors.origins                              = https://app.demo.raid.org.au
spring.security.oauth2.client.registration.keycloak.client-id = raid-api-2

datacite.endpoint                              = https://api.test.datacite.org/dois

raid.orcid-integration.host                    = https://orcid.demo.raid.org
raid.contributor-validation.orcid.url-prefix   = https://sandbox.orcid.org/
raid.contributor-validation.orcid.schema-uri   = https://sandbox.orcid.org/

raid.datacite.resync.enabled                   = true
raid.history.baseline-interval                 = 500
logging.level.root                             = ERROR
logging.level.au.org.raid                      = debug

raid.stub.ark.enabled = raid.stub.doi.enabled = raid.stub.geonames.enabled
  = raid.stub.handle.enabled = raid.stub.isni.enabled
  = raid.stub.openstreetmap.enabled = raid.stub.orcid.enabled
  = raid.stub.ror.enabled = raid.stub.rrid.enabled
  = raid.stub.web-archive.enabled = false
```

Four points worth drawing out, because they are the ones an agency has to decide
for itself:

- It sets `raid.environment=demo` **and** `spring.flyway.locations` explicitly.
  The two agree, so the explicit locations are belt and braces rather than an
  override, and `db/env/demo` is applied as described above.
- It does not set `datacite.registration-agency-name`. RAiD AU is the agency the
  default names, so the default happens to be correct here. It will not be for
  anyone else.
- It does not set `raid.orcid-client.base-url` or `raid.repository-client.url`,
  because the shipped defaults already point at the ORCID sandbox and the
  DataCite test API, which is what a demo environment wants. A production
  environment overrides both.
- The thirteen secrets in the previous section are injected separately from AWS
  Secrets Manager and are not present in this list.

## Checking what a deployment actually loaded

When behaviour is unexpected, two things resolve most questions quickly:

- The startup line reporting the profile, either `No active profile set` or
  `The following 1 profile is active`.
- The effective configuration, with secrets masked.

Include both when raising an issue.
