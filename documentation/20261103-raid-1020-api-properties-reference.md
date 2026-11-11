# RAID-1020: API properties reference and per-environment values

## What changed

`doc/reference/deployment-configuration.md` now lists every property the API
reads, in a new **All properties** section. Each entry gives the property's
default and says whether a deployed environment should set it, treat it as a
secret, check it, or leave it alone. A **Names that have no effect** table lists
settings that look real but that nothing reads.

The page also corrects statements that had drifted from the code:

- The stub list named an `ark` stub that does not exist, and left out `apids`
  and `datacite`. The DataCite stub's prod guard is now described, along with
  its dependence on `raid.environment`.
- `datacite.user` and `datacite.password` were listed as secrets, but no API
  code reads them. `raid.orcid-integration.api-key` was missing from the list.
- The External resolvers section had a stray table header.
- The worked example linked the `registration-agency` CDK tree. RAiD AU demo
  is defined in `registration-authority`. Its values have been refreshed from
  the running task definition.

A companion page in the private CDK repository,
`raido-v2-aws-private/doc/api-environment-properties.md`, records the values
each RAiD AU environment, branch environment, the `registration-agency`
template and the sandbox actually receive. Each secret is listed by its
Secrets Manager name.

## Why

There was no complete list of the API's properties, and agency operators such
as CRKN had to ask for individual values. The existing reference had drifted
since RAID-892 and RAID-993.

## How it was checked

- Property list: taken from `application.yaml`, every `@ConfigurationProperties`
  class and every `@Value` on main. A property was marked "no effect" when its
  getter has no caller.
- Relaxed binding: `raid.stub.geonames.enabled` and
  `raid.stub.openstreetmap.enabled`, supplied as ECS environment variables,
  were confirmed to bind to the `geo-names` and `open-street-map` stubs using
  Spring Boot 3.4.11's `Binder`.
- Values: read from CDK on main, then compared with the running ECS task
  definitions for test, stage, demo, prod, a branch environment and the
  sandbox on 3 November 2026.

## Found, not changed

These are outside the ticket and are raised separately:

- `raid.orcid-integration.api-key` is not injected in any environment, so the
  API sends `X-API-Key: not-set`.
- `datacite.user` and `datacite.password` are injected everywhere but never
  read.
- The `registration-agency` template does not set `raid.environment`. It also
  sets `landing-prefix` and `name-prefix` the opposite way round to RAiD AU.
- Several `IdentifierProperties`, `OrcidIntegrationProperties` and
  `ContributorValidationProperties` fields are bound but unused.

## Links

- JIRA: [RAID-1020](https://ardc.atlassian.net/browse/RAID-1020)
- PR (public reference): https://github.com/au-research/raid-au/pull/763
- PR (per-environment values): https://github.com/au-research/raido-v2-aws-private/pull/71
- PR (descriptions follow-up): https://github.com/au-research/raid-au/pull/764 and https://github.com/au-research/raido-v2-aws-private/pull/72

## Follow-up: property descriptions

Both pages now describe what each property does. The public **All properties**
tables have a **What it does** column. The private per-environment page has a
short *What these do* list under each table and a **Used for** column on the
secrets table. Each description was checked against the code that reads the
property.
