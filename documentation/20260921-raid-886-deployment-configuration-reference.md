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
   `test` and `stage` cover RAiD AU's internal pipeline only. The shared folders
   are not portable: `db/env/demo` holds one agency-neutral migration (`V42.1`,
   the sandbox ORCID contributor schema row) and six RAiD AU data repairs, and
   `db/env/prod` holds six repairs and nothing portable. `V36.1`, in both,
   rewrites `raid_history` to `static.<env>.raid.org.au` URLs and would corrupt
   another agency's history. So the two folders every agency is expected to use
   are exactly the two that will damage a non-RAiD AU deployment. The page
   recommends omitting the env folder as a temporary measure, as the RAiD AU demo
   deployment already does, and flags making `demo` and `prod` agency-neutral as
   the fix.

## Framing

The page uses neutral terminology throughout. raid.org is the registration
authority operating shared services; RAiD AU is one registration agency among
several with no special status. The RAiD AU values in `application.yaml` are
described as an artefact of the codebase having originated at RAiD AU, not as a
reference configuration. RAiD AU's demo environment appears as a worked example
of one agency's configuration rather than a template to copy.

## Follow-up work identified

Raised on RAID-886 for separate tickets, not addressed here:

- `registration-agency-identifier` and `registration-agency-name` should not
  default to RAiD AU values; consider failing startup when unset
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
- make `db/env/demo` and `db/env/prod` agency-neutral, since every agency runs
  those two environments. Separate the environment-dependent migrations every
  agency needs from the RAiD AU data repairs that must never run elsewhere;
  `dev`, `test` and `stage` can stay RAiD AU-internal. Today `demo` carries one
  portable migration against six repairs and `prod` carries none against six,
  and `V36.1` in both hardcodes RAiD AU hostnames
- check whether any agency has already run with `raid.environment` set to
  `demo` or `prod`, since `V36.1` would have rewritten their `raid_history` to
  RAiD AU URLs
- fix the `raid.orcid-integration.host` default, and the non-transactional
  failure behaviour that leaves an orphaned DataCite DOI when the ORCID call
  fails mid-mint
- `raid.ror-client.client-id` is a credential committed in the base
  `application.yaml` of a public repository and is overridden by no environment

## Links

- JIRA: [RAID-886](https://ardc.atlassian.net/browse/RAID-886)
- PR: [au-research/raid-au#674](https://github.com/au-research/raid-au/pull/674)
