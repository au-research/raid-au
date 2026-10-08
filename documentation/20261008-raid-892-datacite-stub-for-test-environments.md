# RAID-892: Stop test environments creating DataCite repositories and minting DOIs

- Ticket: [RAID-892](https://ardc.atlassian.net/browse/RAID-892) (parent story)
- Sub-tasks:
  - [RAID-943](https://ardc.atlassian.net/browse/RAID-943): in-memory DataCite stub (raid-au)
  - [RAID-945](https://ardc.atlassian.net/browse/RAID-945): enable the stub in the test environment (raido-v2-aws-private)
  - [RAID-948](https://ardc.atlassian.net/browse/RAID-948): enable the stub in branch environments (raido-v2-aws-private)
- PRs:
  - https://github.com/au-research/raid-au/pull/709 (RAID-943)
  - https://github.com/au-research/raido-v2-aws-private/pull/62 (RAID-945)
  - https://github.com/au-research/raido-v2-aws-private/pull/63 (RAID-948)
  - The two story PRs that merge `feature/RAID-892` to `main` in each repository

## What changed and why

DataCite reported that ARDC's test account (ATHH) was filling up with
repositories created by RAiD's test environments. Every repository
permanently reserves a prefix from DataCite's shared pool. A repository can
only be deleted if it holds no DOIs, and DOIs can't be deleted at all.

An audit of the live DataCite test API on 2026-10-08 found:

- 613 repositories in ATHH, 555 of them created by branch environments
  across 103 branches.
- Each branch pipeline run creates 2 repositories, one per service point
  that `Configure-ServicePoints` POSTs. Redeploys leak more because the API
  creates the repository before the service point's unique-name check fails.
- The "RAiD AU (branch-...)" repositories receive the DOIs minted by the
  branch's intTest and e2e runs, so they can't be deleted afterwards.

Cleaning up at teardown can't work while tests mint into the environment's
own repository. As a temporary measure until the RAID-812 mock server, test
environments now stop calling DataCite altogether:

- `raid.stub.datacite.enabled` (raid-au, default `false`) swaps
  `DataciteService` and `DataciteRepositoryClient` for in-memory stubs,
  following the existing stub pattern in `ExternalPidService`.
  - The DOI stub still builds the DataCite request, so payload-building
    failures (for example ROR lookups) still surface, but it never sends
    the request.
  - The repository stub returns the requested repository with the
    configured `raid.stub.datacite.prefix` (default `10.5072`).
  - The API refuses to start with the stub enabled when
    `raid.environment=prod`. Unlike the read-only validator stubs, this
    stub silently drops writes.
- The test environment (`config/environment-properties.ts`) and branch
  environments (`branch-api-stack.ts`) set it to `true`. Demo, stage and
  prod set it explicitly to `false`.

## Verification

- raid-au: unit tests (1,020) and intTests (256) green locally.
- raido-v2-aws-private: jest (256) and `tsc --noEmit` green.
- `BranchApi-raid-943` was hand-deployed from `feature/RAID-892` with the
  flag on. The startup log showed both stubs active. intTest
  (`BUILD SUCCESSFUL`) and e2e (46 passed) ran against it. During the runs
  the API log showed 99 mints and 24 updates skipped by the stub and no
  calls to `api.test.datacite.org`, and ATHH gained no repositories or DOIs.

## Not covered

- `LegacyRaidService` and `RaidUpgradeService` still POST to DataCite
  directly, outside `DataciteService`. Branch tests don't use them.
- Remaining RAID-892 work:
  - Deploy to the shared test environment and verify, once both story PRs
    reach `main`.
  - One-off clean-up of existing orphaned repositories in ATHH: about 430
    empty branch repositories can be deleted. About 126 that hold DOIs
    can't be, and are recorded as known exceptions.
  - Document the convention of not minting DOIs in disposable test
    repositories.
