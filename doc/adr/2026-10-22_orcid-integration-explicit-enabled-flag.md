### Switch the ORCID integration on with an explicit `enabled` flag, off by default

* Status: final
* Who: decided by RL (2026-10-22)
* When: 2026-10-22
* Related: RAID-993 (story), RAID-995 (API), RAID-996 (ARDC CDK),
  RAID-997 (RAiD US), RAID-976 (per-RA ORCID updater), RAID-886
  (deployment configuration reference)

# Context

The base `application.yaml` pointed `raid.orcid-integration.host` at the
local mock server (`http://localhost:1080`). A deployment that did not
override it could not mint or update: `RaidService` calls
`OrcidIntegrationService.updateOrcidRecord` inside the save transaction,
and a failed POST to `/raid-update` rolls the save back. EOSC RAiD (SURF)
needs to launch without an ORCID updater, and the central updater is to
be switched off.

Two mechanisms were considered:

* Treat a blank `host` as "off". No new property, and existing ARDC
  deployments need no change, but "off" is implied by a missing value.
* An explicit `raid.orcid-integration.enabled` flag.

# Decision

`raid.orcid-integration.enabled` is written as `false` in the base
`application.yaml`, and the Java field also defaults to `false`. `host`
defaults to an empty string so the listener URIs still resolve.

When `enabled` is false, `OrcidIntegrationService`:

* makes no HTTP call to any ORCID integration host, neither the status
  lookup nor the `raid-update` POST;
* sets each contributor's status to the status already stored for that
  contributor, or `UNAUTHENTICATED` when there is none. The contributor
  row is shared across RAiDs, so taking the request's status would
  downgrade a contributor who authenticated earlier.

At startup the API logs whether the integration is enabled. When
`enabled` is true and `host` is blank, the API refuses to start.

The `dev` profile sets `enabled: true` and `host: http://localhost:1080`.

# Consequences

Good:

* A new registration agency can run with no ORCID integration settings.
* Whether the integration is on is visible in configuration and in the
  startup log.
* A deployment that asks for the integration without a host fails at
  startup, not on every mint.

Costs and risks:

* Every deployment that uses the integration must now set
  `enabled=true`: ARDC test, demo, stage, prod and branch environments
  (RAID-996) and RAiD US demo and prod (RAID-997). The setting must be
  deployed before the API release that contains this change, otherwise
  ORCID is silently switched off. Older API versions ignore the property,
  so deploying it first is safe.
* SURF's configuration sets `host` explicitly. With `enabled` unset, the
  host is ignored and SURF needs no change.
