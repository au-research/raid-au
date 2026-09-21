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
| `raid.identifier.registration-agency-identifier` | RAiD AU's ROR, `https://ror.org/038sjwq14` | Stamped into every minted RAiD, and used to route ORCID contributor updates back to the originating deployment. A wrong value attributes RAiDs to RAiD AU. |
| `datacite.registration-agency-name` | `Australian Research Data Commons` | Sent to DataCite with each record. |
| `raid.iam.realm-uri` | `https://iam.${raid.environment}.raid.org.au/realms/raid` | A RAiD AU hostname. |
| `raid.identifier.landing-prefix` | `https://static.${raid.environment}.raid.org.au/raids/` | A RAiD AU hostname. |

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

The folders `dev`, `test`, `demo`, `stage` and `prod` each hold data specific to
one of RAiD AU's own environments, so none of them suits another deployment.
`db/env/dev` in particular rewrites service point rows with mock server DataCite
credentials and RAiD AU's ROR.

Rather than choosing one, set the locations explicitly and leave the environment
folder out:

```
spring.flyway.locations = classpath:db/migration,classpath:db/env/api_user
```

One caveat. Accepting ORCID sandbox contributors also requires a
`contributor_schema` row for `https://sandbox.orcid.org/`, which today is
delivered only by the `dev`, `test` and `demo` folders (`V42.1`). Without it,
saving a sandbox ORCID contributor fails. Insert that row directly until the
migration moves somewhere environment-neutral.

## Required properties

Set all of these. Anything not listed keeps its shipped default.

### Identity

| Property | Description |
| --- | --- |
| `raid.identifier.registration-agency-identifier` | The registration agency's ROR. See trap 2. |
| `datacite.registration-agency-name` | The registration agency's name, as sent to DataCite. |
| `raid.identifier.landing-prefix` | Prefix for the RAiD landing page URLs the deployment serves. |
| `raid.identifier.name-prefix` | Prefix used when building identifier names. |

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
| `raid.repository-client.url` | Repositories endpoint, for managing repository accounts. |

Per-service-point DataCite credentials live in the `service_point` table, not in
configuration.

### ORCID integration

The ORCID integration is a shared service operated by raid.org on behalf of all
registration agencies. Agencies do not deploy their own copy, and do not need
their own ORCID member credentials for it.

| Property | Description |
| --- | --- |
| `raid.orcid-integration.host` | The raid.org ORCID integration host for the relevant environment. |
| `raid.orcid-integration.api-key` | The API key issued by raid.org. |
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
| `raid.orcid-client.base-url` | ORCID API. Defaults to the sandbox. |
| `raid.ror-client.base-url` | ROR API. |
| `raid.ror-client.client-id` | ROR API client ID. A RAiD AU value ships as the default; obtain a separate one. |
| `raid.isni-client.url-format` | ISNI SRU query URL. |

### Optional

| Property | Default | Description |
| --- | --- | --- |
| `raid.datacite.resync.enabled` | `false` | Background re-push of records flagged for DataCite re-sync. |
| `raid.history.baseline-interval` | `50` | How often a full history baseline is written. |
| `logging.level.root` | | Root log level. |
| `raid.stub.*.enabled` | `false` | In-memory stubs for external resolvers. Leave off in a deployed environment. |

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
Those repositories are private; request access if the detail is useful.

Note that it sets `spring.flyway.locations` explicitly and overrides every
property that would otherwise interpolate `raid.environment`, rather than setting
`raid.environment` itself.

## Checking what a deployment actually loaded

When behaviour is unexpected, two things resolve most questions quickly:

- The startup line reporting the profile, either `No active profile set` or
  `The following 1 profile is active`.
- The effective configuration, with secrets masked.

Include both when raising an issue.
