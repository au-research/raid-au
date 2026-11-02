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
>
> **2.20.0** turned the ORCID integration off by default. Trap 1 and
> [ORCID integration](#orcid-integration) describe both behaviours.

The sections below cover what a deployment has to decide. Every property the API
reads is listed with its default in [All properties](#all-properties) at the end
of this page, followed by the names that look like settings but have no effect.
>
> **2.20.0** added `raid.orcid-integration.enabled`, which defaults to `false`.
> A deployment that uses the ORCID integration must set it to `true` before
> upgrading, or the integration is switched off. See
> [ORCID integration](#orcid-integration).

## How configuration is supplied

The API is a Spring Boot application, so every property below can be supplied as
an environment variable, a JVM system property, or an entry in a configuration
file. Spring's
[relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html)
rules apply, so `raid.db.host` and `RAID_DB_HOST` are equivalent.

The defaults live in
[`api-svc/raid-api/src/main/resources/application.yaml`](../../api-svc/raid-api/src/main/resources/application.yaml).

> **`application-dev.yaml` is the local development profile, not a deployment
> template.** It points the API at the mock server in this repository's Docker
> Compose setup, which is what makes `./gradlew bootRunLocal` work out of the box
> on a developer's machine. Use it for that, and set the properties below
> explicitly for a deployed environment rather than starting from it.

## Three traps to know about first

As noted above, several defaults carry RAiD AU values rather than neutral ones.
Three groups of them cause problems in any other deployment, and two fail quietly
rather than loudly.

### 1. On 2.19.0 and earlier, one default points at a mock server

On 2.19.0 and earlier, `raid.orcid-integration.host` defaults to
`http://localhost:1080`, the local development mock server. Nothing listens
there in a deployed environment, so the call fails with `Connection refused`.

The call happens after the DataCite mint but inside the same database
transaction, so a failure mints a real DOI and then rolls the database change
back. On these releases, set this property before minting anything.

From 2.20.0 the integration is off unless `raid.orcid-integration.enabled` is
`true`, and `host` has no default, so a deployment that sets neither makes no
call. The transaction behaviour is unchanged when the integration is enabled:
an unreachable host still fails the save after the DataCite mint. See
[ORCID integration](#orcid-integration).

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
should name the environment it is actually running as. `dev` is for running the
API locally, and `test` and `stage` cover RAiD AU's own internal pipeline, so
none of the three belongs on another agency's deployed environment.

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

### `dev` is for running the API locally

`dev` is the environment for a developer's own machine, and it is worth using
for that. Everything it needs is in this repository: following
[`README.md`](../../README.md), `./gradlew bootRunLocal` starts PostgreSQL,
Keycloak and MockServer through Docker Compose and then launches the API on
`http://localhost:8080` with the `dev` profile. That is the whole setup, and it
is the quickest way to get a working instance to explore before deploying one.

What makes it work locally is exactly what makes it wrong for a deployed
environment. `db/env/dev` is the only environment folder that changes an empty
database: `V40.1` inserts a ready-made service point so there is something to
mint against immediately. That service point carries RAiD AU's ROR and
placeholder DataCite credentials that only authenticate against the local mock
server, which is ideal on a laptop and unusable anywhere else.

An agency that sets `raid.environment=dev` on a deployed environment therefore
starts with a service point that looks usable, attributes RAiDs to RAiD AU, and
cannot authenticate to DataCite. Use `demo` or `prod` there instead. `test` and
`stage` cover RAiD AU's own internal pipeline and are similarly not meant for
another agency's deployment.

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
| `raid.orcid-integration.enabled` | `true` to use the integration. From 2.20.0 it defaults to `false`. Not present on 2.19.0 and earlier, where the integration is always on. |
| `raid.orcid-integration.host` | The raid.org ORCID integration host for the relevant environment. Required when `enabled` is `true`. |
| `raid.orcid-integration.api-key` | The API key issued by raid.org for this deployment. Set it once raid.org has issued one. |
| `raid.contributor-validation.orcid.url-prefix` | `https://orcid.org/`, or `https://sandbox.orcid.org/` for sandbox. |
| `raid.contributor-validation.orcid.schema-uri` | Must match the url-prefix above. |

The two contributor-validation properties apply whether or not the integration
is enabled. They control which ORCID iDs a RAiD can contain.

**With the integration enabled**, the API looks up each contributor's status
from the integration host and sends each saved RAiD to it. If the host is
unreachable, the mint or update fails.

**With the integration disabled**, the API makes no request to any integration
host. Mints and updates work without it. Each contributor keeps the status
already stored for that ORCID iD, and a contributor new to the database is given
`UNAUTHENTICATED`. Contributors are shared across RAiDs, so a contributor who
authenticated through an earlier integration keeps that status.

**At startup** the API logs one of these lines at `INFO`:

```
ORCID integration is enabled: host https://orcid.demo.raid.org
ORCID integration is disabled: contributor statuses are not looked up and RAiDs are not sent to an ORCID integration host
```

If `enabled` is `true` and `host` is blank, the API refuses to start with
`raid.orcid-integration.enabled is true but raid.orcid-integration.host is not set`.

A new agency can launch with the integration disabled and enable it later.
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

The stub switches exist for local development and integration tests. There are
eleven: `apids`, `datacite`, `doi`, `geo-names`, `handle`, `isni`,
`open-street-map`, `orcid`, `ror`, `rrid` and `web-archive`. Spring's relaxed
binding ignores the hyphens, so `raid.stub.geonames.enabled` and
`raid.stub.openstreetmap.enabled` work too. They all default to `false`, so a
deployment need not set them.

`raid.stub.datacite.enabled` is different from the others: it stops the API
sending anything to DataCite at all, and the API refuses to start with it set to
`true` when `raid.environment` is `prod`. That guard only works if
`raid.environment` is set, which is one more reason to set it.

There is no `ark` stub. `raid.stub.ark.enabled` is accepted and ignored.

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
| `raid.repository-client.username` | DataCite repository administration account. |
| `raid.repository-client.password` | Password for the above. |
| `raid.repository-client.email` | Contact email for the above. |
| `spring.security.oauth2.client.registration.keycloak.client-secret` | API OIDC client secret. |
| `raid.raid-permissions.client-secret` | Permissions admin client secret. |
| `raid.orcid-client.access-token` | ORCID API access token. |
| `raid.validation.geonames.username` | GeoNames account used for spatial coverage validation. |
| `raid.orcid-integration.api-key` | Sent as `X-API-Key` to the ORCID integration host, once raid.org has issued one. |

DataCite minting credentials are not in this list because they are not
configuration: each service point carries its own, in the `service_point` table,
encrypted with `raid.db.encryption-key`. RAiD AU's deployment also injects
`datacite.user` and `datacite.password`, but nothing in the API reads them.

## Worked example: the RAiD AU demo environment

This is included as one agency's working configuration, not as a template to
copy. It is defined in
[`registration-authority/cdk/config/environment-properties.ts`](https://github.com/au-research/raido-v2-aws-private/blob/main/registration-authority/cdk/config/environment-properties.ts),
with the secret wiring in
[`api-service.ts`](https://github.com/au-research/raido-v2-aws-private/blob/main/registration-authority/cdk/lib/raid/construct/ecs/api-service.ts).
That repository is private; request access if the detail is useful. The
values below are the ones the running container actually has, read from its task
definition on 3 November 2026, with the database hostname omitted. It is running
a build later than 2.20.0, so it sits on the far side of both release boundaries
noted at the top of this page.

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

raid.orcid-integration.enabled                 = true
raid.orcid-integration.host                    = https://orcid.demo.raid.org
raid.contributor-validation.orcid.url-prefix   = https://sandbox.orcid.org/
raid.contributor-validation.orcid.schema-uri   = https://sandbox.orcid.org/

raid.datacite.resync.enabled                   = true
raid.history.baseline-interval                 = 500
logging.level.root                             = ERROR
logging.level.au.org.raid                      = debug
logging.level.org.springframework.security.web.FilterChainProxy = error
logging.level.org.springframework.security.web.authentication.AnonymousAuthenticationFilter = error

raid.stub.ark.enabled = raid.stub.datacite.enabled = raid.stub.doi.enabled
  = raid.stub.geonames.enabled = raid.stub.handle.enabled
  = raid.stub.isni.enabled = raid.stub.openstreetmap.enabled
  = raid.stub.orcid.enabled = raid.stub.ror.enabled
  = raid.stub.rrid.enabled = raid.stub.web-archive.enabled = false
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
- Secrets are injected separately from AWS Secrets Manager and are not present
  in this list.
- `raid.stub.ark.enabled` is one of the names with no effect, listed under
  [Names that have no effect](#names-that-have-no-effect). Setting it is
  harmless.

## Checking what a deployment actually loaded

When behaviour is unexpected, three things resolve most questions quickly:

- The startup line reporting the profile, either `No active profile set` or
  `The following 1 profile is active`.
- From 2.20.0, the startup line reporting whether the ORCID integration is
  enabled. It is logged at `INFO`, so it is missing if `logging.level.root` is
  `ERROR` and `logging.level.au.org.raid` is not lowered to `info` or `debug`.
- The effective configuration, with secrets masked.

Include all three when raising an issue.

## All properties

Every property the API reads, from
[`application.yaml`](../../api-svc/raid-api/src/main/resources/application.yaml)
and the `@ConfigurationProperties` classes under
`api-svc/raid-api/src/main/java/au/org/raid/api/config/`. The **Set it?** column
says what a deployed environment should do:

- **Required:** the API will not work correctly without a deployment value.
- **Secret:** required, and supplied from a secret store.
- **Check:** the default suits some environments and not others.
- **Leave:** keep the default.

Properties marked as Spring's own are standard Spring Boot settings. The
[Spring Boot reference](https://docs.spring.io/spring-boot/appendix/application-properties/index.html)
describes them in full; the description here says how the RAiD API uses them.

### Environment and database

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `raid.environment` | `not-set` | Required | Names the environment the instance is running as. It picks the folder of environment-specific migrations (`db/env/<value>`), fills the RAiD AU hostnames in the default `raid.iam.realm-uri` and `raid.identifier.landing-prefix`, and switches on the check that stops the DataCite stub running when the value is `prod`. Use `demo` or `prod`. See trap 3. |
| `raid.db.host` | none | Required | Hostname of the PostgreSQL server. Combined with the port and name into `spring.datasource.url`. |
| `raid.db.port` | none | Required | Port of the PostgreSQL server, normally `5432`. |
| `raid.db.name` | `raido` | Leave | Name of the PostgreSQL database. The API's tables live in its `api_svc` schema. |
| `raid.db.user` | none | Required | Database user the API connects as for normal reads and writes. |
| `raid.db.password` | none | Secret | Password for `raid.db.user`. |
| `raid.db.encryption-key` | none | Secret | Key used to encrypt each service point's DataCite repository password before it is stored, and to decrypt it when the API mints. Changing it on an existing database makes every stored password unreadable, so no service point can mint until each password is re-entered. |
| `raid.db.repair-token` | `RAID-878-v47` | Leave | Triggers a one-off Flyway `repair`, which rewrites stored migration checksums, before migrations run. Each token value runs once per database and is recorded in `db_repair_log`, so leaving a used token in place is harmless. A new value is only needed when a release deliberately ships a new repair. |
| `spring.flyway.locations` | `classpath:db/env/api_user,classpath:db/migration,classpath:db/env/${raid.environment}` | Leave | Spring's own. The folders Flyway reads migrations from at startup: the shared schema migrations plus the environment folder chosen by `raid.environment`. |
| `spring.flyway.user`, `spring.flyway.password` | the datasource user | Secret | Spring's own. Database account Flyway uses to run migrations at startup. Usually more privileged than `raid.db.user`, because migrations create and alter tables. |
| `spring.flyway.default-schema` | `api_svc` | Leave | Spring's own. Schema Flyway migrates. It is also appended to the datasource URL as `currentSchema`, which is how unqualified table names in the API's queries find their tables. |
| `spring.datasource.url` | built from `raid.db.*` | Leave | Spring's own. JDBC URL for the database. If overridden by hand, keep `?currentSchema=api_svc` or every query fails with `relation "raid" does not exist`. |

### Identity

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `raid.identifier.registration-agency-identifier` | none (from 2.17.0) | Required | ROR of the registration agency running the instance. It is published as `identifier.registrationAgency.id` on every RAiD minted, and at startup the API uses it to look up its Service Point ID block in `registration-agencies.yaml`. The API refuses to start if it is unset or has no block. |
| `raid.identifier.landing-prefix` | `https://static.${raid.environment}.raid.org.au/raids/` | Required | Start of each RAiD's landing page URL; the RAiD's handle is appended. The result is published as `identifier.raidAgencyUrl` and registered with DataCite as the URL the DOI resolves to. The default is a RAiD AU hostname. |
| `raid.identifier.name-prefix` | `https://raid.org/` | Leave | Start of each RAiD's identifier; the handle is appended to form `identifier.id`, for example `https://raid.org/10.xxxx/yyyy`. It is the same for every agency in the federation. |
| `raid.identifier.schema-uri` | `https://raid.org/` | Leave | Published as `identifier.schemaUri`. Must be one of the values the RAiD metadata schema allows, so leave it as it is. |
| `raid.identifier.license` | `Creative Commons CC-0` | Leave | Licence published as `identifier.license` on each new RAiD, which states the licence the RAiD's metadata is released under. |
| `datacite.registration-agency-name` | `Australian Research Data Commons` | Required | Name of the registration agency, sent to DataCite as a contributor on every record. The default is RAiD AU's name. |

### Authentication and CORS

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `raid.iam.realm-uri` | `https://iam.${raid.environment}.raid.org.au/realms/raid` | Required | URI of the Keycloak realm that issues tokens for the deployment. Both Spring issuer URIs below default to it, so this one setting tells the API whose tokens to trust. The default is a RAiD AU hostname. |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `${raid.iam.realm-uri}` | Leave | Spring's own. Issuer the API checks on every bearer token it receives. Spring fetches the realm's signing keys from it. |
| `spring.security.oauth2.client.provider.keycloak.issuer-uri` | `${raid.iam.realm-uri}` | Leave | Spring's own. Issuer for the browser sign-in described in the next row. |
| `spring.security.oauth2.client.registration.keycloak.client-id` | `raid-api-2` | Check | Spring's own. Keycloak client the API uses when a person signs in to the API directly in a browser, for example to try requests in the Swagger UI. The realm must contain a client with this ID. |
| `spring.security.oauth2.client.registration.keycloak.client-secret` | none | Secret | Spring's own. Secret for the client above. |
| `raid.raid-permissions.client-id` | none | Required | Confidential Keycloak client the API authenticates as, using the client credentials grant, to read a user's RAiD permissions and to give the user who mints a RAiD admin rights over it. RAiD AU uses `raid-permissions-admin`. |
| `raid.raid-permissions.client-secret` | none | Secret | Secret for the client above. |
| `raid.cors.origins` | `http://localhost:7080` | Required | Browser origins allowed to call the API, comma-separated. Normally the agency app's URL, for example `https://app.demo.example.org`. A browser on any other origin is refused. The default is the local development app. |

### DataCite

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `datacite.endpoint` | `https://api.test.datacite.org/dois` | Check | DataCite DOI API the API mints and updates DOIs through. The default is DataCite's test system, whose DOIs do not resolve publicly; production uses `https://api.datacite.org/dois`. |
| `raid.repository-client.url` | `https://api.test.datacite.org/repositories` | Check | DataCite repositories API. When a service point is created, the API creates a DataCite repository for it here, and the service point mints into that repository. Production uses `https://api.datacite.org/repositories`. |
| `raid.repository-client.username` | `not-set` | Secret | DataCite account, with permission to create repositories, that the API uses to create one for each new service point. |
| `raid.repository-client.password` | `not-set` | Secret | Password for the account above. |
| `raid.repository-client.email` | `not-set` | Secret | Contact email recorded as the system email on each repository the API creates. |
| `raid.datacite.resync.enabled` | `false` | Check | Turns on a background worker that re-sends RAiDs flagged as needing a DataCite update, sending each one's full record again. Flags are set by migrations that change how records are sent to DataCite, so an environment with this off keeps the old DataCite metadata for existing RAiDs. |
| `raid.datacite.resync.batch-size` | `50` | Leave | Number of flagged RAiDs the worker takes in each pass. |
| `raid.datacite.resync.throttle-millis` | `1000` | Leave | Pause between RAiDs within a pass, in milliseconds, so the worker stays well inside DataCite's rate limits. |
| `raid.datacite.resync.poll-delay-millis` | `60000` | Leave | Pause between passes, in milliseconds. |

### ORCID

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `raid.orcid-integration.enabled` | `false` (from 2.20.0) | Check | Whether the API uses raid.org's ORCID integration. When on, the API looks up each contributor's status there, and sends each saved RAiD so it can be written to contributors' ORCID records. When off, neither happens and stored statuses are kept. See trap 1. |
| `raid.orcid-integration.host` | empty | Check | Base URL of the ORCID integration for the environment, issued by raid.org. Required when the integration is enabled. |
| `raid.orcid-integration.api-key` | `not-set` | Secret | Key sent as the `X-API-Key` header on every call to the ORCID integration. Issued by raid.org. |
| `raid.orcid-integration.raid-listener.uri` | `${raid.orcid-integration.host}/raid-update` | Leave | Endpoint each saved RAiD is posted to. |
| `raid.orcid-integration.contributor-id-lookup.uri` | `${raid.orcid-integration.host}/find-contributor-by-orcid` | Leave | Endpoint used to look up a contributor's status by ORCID iD. |
| `raid.orcid-client.base-url` | `https://api.sandbox.orcid.org/v3.0/` | Check | ORCID public API used to check that a contributor's ORCID iD exists and to read the person's public details. The default is the ORCID sandbox; production uses `https://api.orcid.org/v3.0/`. Must match the ORCID system in `raid.contributor-validation.orcid.url-prefix`. |
| `raid.orcid-client.access-token` | `not-set` | Secret | Bearer token for the ORCID API above, from an ORCID public API client. |
| `raid.contributor-validation.orcid.url-prefix` | `https://orcid.org/` | Check | Prefix every contributor's ORCID iD must start with. A RAiD whose contributor iD has a different prefix fails validation. Set `https://sandbox.orcid.org/` for an environment that uses the ORCID sandbox. |
| `raid.contributor-validation.orcid.schema-uri` | `https://orcid.org/` | Check | Schema URI that ORCID contributors must declare. Must match the prefix above, and must also exist as an active row in the `contributor_schema` table. See [Sandbox ORCID contributors](#sandbox-orcid-contributors). |
| `raid.contributor-validation.isni.url-prefix` | `https://isni.org/` | Leave | Prefix every contributor's ISNI must start with. |
| `raid.contributor-validation.isni.schema-uri` | `https://isni.org/` | Leave | Schema URI that ISNI contributors must declare. |

### Other external services

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `raid.ror-client.base-url` | `https://api.ror.org/v2/` | Leave | ROR API used to check that each organisation's ROR exists. |
| `raid.ror-client.client-id` | a RAiD AU client ID | Check | Sent as the `Client-Id` header on ROR requests, which identifies the caller to ROR. Register a client ID with ROR for the agency rather than using RAiD AU's. |
| `raid.isni-client.url-format` | the OCLC SRU query URL | Leave | URL template for looking up an ISNI in the OCLC ISNI database. `__ISNI__` is replaced with the iD being checked. |
| `raid.validation.geonames.username` | `geonames` | Secret | GeoNames account the API uses to check that a GeoNames spatial coverage URI exists. GeoNames requires a registered account, so set the agency's own. |
| `raid.spatial-coverage.schema-uri.geonames` | `https://www.geonames.org/` | Leave | Spatial coverage schema URI that marks a place as a GeoNames place, so it is checked against GeoNames. |
| `raid.spatial-coverage.schema-uri.openstreetmap` | `https://www.openstreetmap.org/` | Leave | Spatial coverage schema URI that marks a place as an OpenStreetMap place, so it is checked against OpenStreetMap. |
| `raid.uri-validation.connect-timeout` | `5s` | Leave | How long the API waits to connect when checking that a related object's URI resolves, for example a DOI or Handle. A slow resolver then produces a validation error instead of holding the request open. |
| `raid.uri-validation.read-timeout` | `10s` | Leave | How long the API waits for a response from the same checks. |
| `raid.uri-validation.success-cache.expire-after-write` | `30m` | Leave | How long a URI that resolved successfully is remembered, so saving the same RAiD again does not check it again. Failures are never remembered. `0s` turns the cache off. |
| `raid.uri-validation.success-cache.maximum-size` | `10000` | Leave | Most URIs the cache above holds per API instance. `0` turns it off. |

### Stubs, history and logging

| Property | Default | Set it? | What it does |
| --- | --- | --- | --- |
| `raid.stub.<name>.enabled` | `false` | Leave | Replaces one external service with an in-memory stand-in that answers without a network call, for local development and automated tests. See [Optional](#optional) for the names. Leave them all off in a deployed environment. |
| `raid.stub.<name>.delay` | `150` (`0` for `datacite`) | Leave | Milliseconds the stub waits before answering, to imitate network latency. |
| `raid.stub.datacite.prefix` | `10.5072` | Leave | DOI prefix the DataCite stub gives the repositories it pretends to create. Nothing reaches DataCite, so it only needs to look like a prefix. |
| `raid.history.baseline-interval` | `50` | Leave | RAiD history stores each change as a difference from the previous version. Every this many versions, it stores the whole record instead, so rebuilding an old version never needs more than this many differences. |
| `logging.level.root`, `logging.level.<package>` | `info` | Check | Spring's own. How much the API logs, overall or for one package. The API's own code is under `au.org.raid`. The ORCID integration startup line is logged at `info`, so it disappears if `au.org.raid` is set higher. |

### Fixed by the application

These are set in `application.yaml` and are not expected to vary:
`server.forward-headers-strategy` (`native`), `spring.cache.type` (`simple`),
`spring.jackson.*`, `spring.jooq.sql-dialect`, `spring.flyway.create-schemas`,
`spring.flyway.baseline-version` (`25`),
`spring.security.oauth2.client.registration.keycloak.*` other than the client ID
and secret, `springdoc.swagger-ui.enabled` and `springdoc.api-docs.path`
(`/docs`).

### Names that have no effect

These appear in RAiD AU's deployment or in the code, and look like settings, but
nothing reads them. Setting them is harmless, and so is removing them.

| Name | Why |
| --- | --- |
| `raid.stub.ark.enabled` | There is no ARK stub. |
| `raid.identifier.handle-url-prefix` | Bound but never used. |
| `raid.orcid-integration.orcid-url-prefix` | Bound but never used. Contributor validation uses `raid.contributor-validation.orcid.url-prefix`. |
| `raid.contributor-validation.pattern` | Bound but never used. |
| `datacite.user`, `datacite.password` | Not read. DataCite credentials come from each service point. |
| `ENVIRONMENT_NAME`, `EnvironmentConfig.envName`, `EnvironmentConfig.isProd` | Left over from an earlier version of the API. Use `raid.environment`. |
| `orcid.in-memory-stub`, `ror.in-memory-stub`, `doi.in-memory-stub`, `geonames.in-memory-stub`, `openstreetmap.in-memory-stub` | Earlier stub switch names. Use `raid.stub.<name>.enabled`. |
