# RAID-886: Deployment configuration reference

## What changed

Added `doc/reference/deployment-configuration.md`, a reference page listing the
configuration the RAiD API needs when deployed by a registration agency, what
each property relates to, and which shipped defaults must be overridden.

The page covers:

- how configuration is supplied, and why `application-dev.yaml` is not a
  deployment template
- three traps that cost deploying agencies time, described below
- required properties grouped by concern: identity, database, authentication,
  DataCite, ORCID integration, external resolvers, optional tuning
- the thirteen values that must come from a secret store rather than plain
  configuration, taken from the RAiD AU ECS secret wiring
- what raid.org needs in order to register an agency's deployment for ORCID
  contributor write-back
- the two diagnostics worth sending when raising an issue

## Why

Registration agencies had no reference for which properties to set. Both SURF
and CRKN reverse-engineered `application.yaml`, and both deployed with
`raid.orcid-integration.host` still pointing at the local development mock
server, failing on mint with `Connection refused`.

The closest existing page, `doc/architecture/environment/deployment-environment.md`,
describes RAiD AU's own AWS setup and is out of date.

## The three traps documented

1. **A mock address in base configuration.** `raid.orcid-integration.host`
   defaults to `http://localhost:1080` in `application.yaml`, not in
   `application-dev.yaml`. DataCite does this correctly, defaulting to the real
   test API in base and overriding to the mock only in the dev profile. That
   asymmetry is why minting reaches DataCite while the ORCID call does not.

2. **Base defaults carrying RAiD AU identity.**
   `raid.identifier.registration-agency-identifier` ships as RAiD AU's ROR and
   `datacite.registration-agency-name` as RAiD AU's name. These fail silently:
   the API starts, minting succeeds, and records name the wrong registration
   agency. The same identifier routes ORCID contributor updates, so an
   unoverridden deployment is also unreachable for write-back.

3. **`raid.environment` is not a Spring profile.** It selects the Flyway env
   folder through `classpath:db/env/${raid.environment}`. Every agency runs a
   `demo` and a `prod` environment, so those two folders are shared; `dev`,
   `test` and `stage` are RAiD AU-internal.

   Against an empty database, `demo` and `prod` are safe to use and `demo` is
   necessary. Every `db/env/prod` migration is an `UPDATE` matching nothing. In
   `db/env/demo` the two migrations that insert or delete real data (`V8_1`,
   `V24.1`) fall below `baseline-version: 25` and never run on a fresh database,
   and the rest match nothing; only `V42.1` takes effect, supplying the sandbox
   ORCID contributor schema row a demo environment needs.

   `dev` is the harmful one and is not part of the convention. `V40.1` is above
   the baseline and inserts a service point carrying RAiD AU's ROR and mock
   DataCite credentials, so it does change an empty database. Both SURF and CRKN
   set `raid.environment=dev`, which is why both ended up with a service point
   that looked usable, attributed RAiDs to RAiD AU and could not authenticate to
   DataCite.

   The page also warns that the minor versions are positional and reused across
   folders (`V40.1` is a different migration in `dev`, `demo` and `prod`;
   `V36.1` has a different checksum in each), so `raid.environment` must not be
   changed once an environment exists.

## Verification against the running deployment

The property list was reconciled against the RAiD AU demo task definition read
from ECS, rather than against the CDK source. That found three things.

1. **The Service Point ID block allocation was missing from the page.**
   `raid.identifier.registration-agency-identifier` no longer defaults to RAiD
   AU's ROR; RAID-862 removed the default and made the instance resolve its
   Service Point ID range from
   `api-svc/raid-api/src/main/resources/registration-agencies.yaml` at startup,
   refusing to start when the ROR is unset or unallocated. This is a hard gate on
   any new deployment and is now documented in its own section. Note that DRAC
   and TIB have blocks reserved but no ROR recorded, so a deployment for either
   would not start on the current release.

2. **The worked example was wrong.** It claimed the demo environment overrides
   every property that would otherwise interpolate `raid.environment` rather than
   setting `raid.environment` itself. The running container sets
   `raid.environment=demo` and sets `spring.flyway.locations` to the matching
   value. The example is now the actual environment read from the task
   definition, with the database hostname omitted, and calls out the four
   decisions an agency has to make for itself.

3. **Smaller corrections.** `raid.identifier.name-prefix` was listed as
   required; it is a federation-wide constant (`https://raid.org/`) and the demo
   environment does not set it. `raid.repository-client.url` and
   `raid.orcid-client.base-url` were listed as required; both have defaults that
   suit a test or demo environment and only need overriding in production. The
   ten `raid.stub.<resolver>.enabled` switches are now named.

All thirteen secrets listed on the page match the thirteen the running container
receives from Secrets Manager, with no additions or omissions.

The CDK source in `raido-v2-aws-private` does not match the deployed task
definition on either branch checked, including `origin/main`, so the deployed
environment was the right thing to verify against. That drift is a separate
concern and is not addressed here.

## Framing

The page uses neutral terminology throughout. raid.org is the registration
authority operating shared services; RAiD AU is one registration agency among
several with no special status. The RAiD AU values in `application.yaml` are
described as an artefact of the codebase having originated at RAiD AU, not as a
reference configuration. RAiD AU's demo environment appears as a worked example
of one agency's configuration rather than a template to copy.

## Follow-up work identified

Raised on RAID-886 for separate tickets, not addressed here:

- `datacite.registration-agency-name` should not default to a RAiD AU value.
  `registration-agency-identifier` was fixed by RAID-862
- the CDK source in `raido-v2-aws-private` has drifted from the deployed demo
  task definition on `origin/main`; reconcile the two
- DRAC and TIB have Service Point ID blocks reserved in
  `registration-agencies.yaml` but no ROR recorded, so neither can start a
  deployment until their entries are completed
- `V42.1` inserts the sandbox ORCID `contributor_schema` row and is
  environment-neutral, but ships only in the environment folders (`dev`, `test`,
  `demo` and `stage` each carry a near-identical copy; `prod` deliberately omits
  it), so agencies omitting the env folder lose it. The row follows from
  `raid.contributor-validation.orcid.schema-uri` and belongs with that property.
  The `test` copy's comment claiming stage omits the row is also stale
- `ContributorSchemaNotFoundException` has no `@ExceptionHandler`, so a missing
  contributor schema surfaces as a generic 500 rather than an error naming the
  schema. This is the likely cause of the sandbox contributor save failures
  reported by CRKN
- `db/env/dev` inserts a RAiD AU service point (`V40.1`) on an empty database,
  and both SURF and CRKN used it. Either make `dev` safe for external use or
  make it obviously RAiD AU-internal, since agencies reach for it first
- the RAiD AU repairs in `db/env/demo` and `db/env/prod` are harmless on an
  empty database but not on a populated one, and some embed RAiD AU hostnames
  (`V36.1`). Consider separating them so the shared folders contain only
  migrations every agency should run
- minor versions are positional and reused across folders for unrelated changes,
  so `raid.environment` cannot be changed after an environment is created
  without a Flyway validation failure. Worth resolving before more agencies
  onboard
- fix the `raid.orcid-integration.host` default, and the non-transactional
  failure behaviour that leaves an orphaned DataCite DOI when the ORCID call
  fails mid-mint
- `raid.ror-client.client-id` is a credential committed in the base
  `application.yaml` of a public repository and is overridden by no environment

## Links

- JIRA: [RAID-886](https://ardc.atlassian.net/browse/RAID-886)
- PR: [au-research/raid-au#674](https://github.com/au-research/raid-au/pull/674)
