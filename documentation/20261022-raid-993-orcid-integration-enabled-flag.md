# RAID-993: Run the RAiD API without an ORCID integration

- JIRA: [RAID-993](https://ardc.atlassian.net/browse/RAID-993) (story)
- Sub-tasks:
  - [RAID-995](https://ardc.atlassian.net/browse/RAID-995) API flag: [au-research/raid-au#736](https://github.com/au-research/raid-au/pull/736) (merged into `feature/RAID-993` 2026-10-22, merge commit `1035b4a3`)
  - [RAID-996](https://ardc.atlassian.net/browse/RAID-996) ARDC environments: [au-research/raido-v2-aws-private#66](https://github.com/au-research/raido-v2-aws-private/pull/66) (merged 2026-10-22, `ef46715e`; deployed to test, demo, stage and prod 2026-10-22)
  - [RAID-997](https://ardc.atlassian.net/browse/RAID-997) RAiD US environments: [au-research/raido-v2-aws-private#67](https://github.com/au-research/raido-v2-aws-private/pull/67) (merged 2026-10-23, `d92d2e14`; takes effect when RAiD US upgrades to 2.20.0)
  - [RAID-998](https://ardc.atlassian.net/browse/RAID-998) deployment configuration reference: [au-research/raid-au#739](https://github.com/au-research/raid-au/pull/739) (merged 2026-10-23)
- Related: [RAID-994](https://ardc.atlassian.net/browse/RAID-994) (orphan DataCite DOI when a mint fails after DataCite, out of scope)
- ADR: `doc/adr/2026-10-22_orcid-integration-explicit-enabled-flag.md`
- PR: [au-research/raid-au#737](https://github.com/au-research/raid-au/pull/737) (`feature/RAID-993` to `main`, merged 2026-10-22, `14235d45`)
- Status: released to production in 2.20.0 on 2026-10-22; RAID-997 awaits the RAiD US upgrade

## What changed and why

EOSC RAiD (SURF) needs to launch without an ORCID updater, and the central
updater is being switched off. Before this change the base `application.yaml`
pointed `raid.orcid-integration.host` at the local mock server. A deployment
that did not override it could not mint or update, because
`updateOrcidRecord` runs inside the save transaction and a failed
`raid-update` POST rolls the save back.

- New property `raid.orcid-integration.enabled`, written as `false` in the
  base `application.yaml`. The Java field also defaults to `false`. `host`
  defaults to `""`.
- When disabled, `OrcidIntegrationService` makes no HTTP call to any ORCID
  integration host. Each contributor keeps the status already stored for that
  contributor, or gets `UNAUTHENTICATED` if there is none. The contributor row
  is shared across RAiDs, so taking the request's status would downgrade a
  contributor who authenticated earlier.
- The startup log states whether the integration is enabled. The API refuses
  to start when `enabled=true` and `host` is blank.
- The `dev` profile sets `enabled: true` and `host: http://localhost:1080`.
- No database migration.

Rob chose the explicit flag over "blank host means disabled" on 2026-10-22.
The cost is that every deployment using the integration must set
`enabled=true`, and must do so before the API release.

## Environment changes (RAID-996)

`raid.orcid-integration.enabled=true` was added wherever the host is set:
test, demo, stage, prod and branch APIs. All four `{ENV}_TAG` constants had
fallen behind the running images again, so they were realigned in the same PR
to stop the Api deploys downgrading the environments. Each environment was
deployed with `--exclusively` and checked: same image, flag present, rollout
complete, health 200.

## Tests

- `OrcidIntegrationServiceTest`: 9 new tests for the disabled path, stored
  status handling and the startup check.
- `OrcidIntegrationPropertiesTest`: binds the shipped `application.yaml` and
  `application-dev.yaml` (default disabled, dev profile on the mock).
- Full `raid-api` unit suite: 1060 tests, 0 failures, 18 skipped.
- Full local intTest (dev profile, integration enabled): 256 tests, 0
  failures, 16 skipped.
- Local check of the "no request" scenario, counting mock server requests
  after `RaidIntegrationTest.mintRaid` and `updateRaid`: 0 to `/raid-update`
  and 0 to `/find-contributor-by-orcid` with the integration disabled; 3 and 3
  with it enabled.

## Verification in the branch environment (2026-10-22)

`feature/RAID-993` at `2a5ffc0b` was deployed to `api-raid-993.test.raid.org.au`
by Branch-Build-Deploy with infrastructure `ef46715e` (RAID-996, so
`enabled=true`, host `https://orcid.demo.raid.org`). Deploy, Api-Integration-Test
and E2e-Test passed.

Each check minted a RAiD with two contributors and then updated it (a title
change): a newly generated ORCID iD and `0000-0002-1825-0097`. Delivery was
checked in the demo updater's logs (raid-aws-private, `raid-demo`, us-east-1).

| | Integration enabled | Integration disabled |
|---|---|---|
| Startup log | `ORCID integration is enabled: host https://orcid.demo.raid.org` | `ORCID integration is disabled: ...` |
| Mint / update | 201 / 200 (`10.5072/51a506e1`) | 201 / 200 (`10.5072/262ce7a1`) |
| New contributor status | `AWAITING_AUTHENTICATION` | `UNAUTHENTICATED` |
| `0000-0002-1825-0097` status | `AWAITING_AUTHENTICATION` | `AWAITING_AUTHENTICATION` (stored status kept) |
| `FindContributorByOrcid` log entries | 4 | 0 |
| `RaidListener` log entries | 2 (handle), 2 (ORCID iD) | 0 |

For the disabled check, `BranchApi-raid-993` was redeployed by hand with
`enabled=false` (same image), then restored to `enabled=true`.

The acceptance scenario expects `0000-0002-1825-0097` to be `UNAUTHENTICATED`.
That holds only when the iD is new to the database. Here it already existed from
the enabled run, so its stored status was kept, as decided on RAID-993 on
2026-10-22.

## Release

Shipped in 2.20.0 from `main` (pipeline run `a701175c`, commit `55343f81`),
released to production on 2026-10-22. SURF needs no configuration change:
leaving `enabled` unset disables the integration even though their
configuration sets `host`.

Each environment had `enabled=true` and the integration host before the image
changed. Checks on the 2.20.0 build:

| Environment | Check | Result |
|---|---|---|
| Test | Mint, update, read `10.82841/c20163ac` | 201, 200, 200; contributors `AWAITING_AUTHENTICATION` |
| Demo | Mint, update, read `10.83483/07b1e2ff` | 201, 200, 200; updater logs show the RAiD; DataCite `publicationYear` and `rightsList` correct |
| Stage | Mint, update, read `10.83746/16829911` | 201, 200, 200; updater logs show the RAiD; DataCite correct |
| Prod | Update `10.26261/11ce4e02` (title change) | 200, version 11; prod updater looked up the contributor and received the RAiD; DataCite updated |

Demo and stage check that ORCID iDs exist on `sandbox.orcid.org`, so those
runs used a known sandbox iD rather than a generated one. Prod logs nothing
at `INFO`, so the startup line could not be used there; the updater logs were
used instead.

RAiD US takes 2.20.0 by following
`documentation/20261023-upgrade-raid-2.20.0.md` in raido-v2-aws-private, which
deploys the flag before the new image.
