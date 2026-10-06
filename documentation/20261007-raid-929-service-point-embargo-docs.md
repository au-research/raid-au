# RAID-929: Service point users can read their own embargoed RAiDs

- JIRA: [RAID-929](https://ardc.atlassian.net/browse/RAID-929) (Bug, parent [RAID-711](https://ardc.atlassian.net/browse/RAID-711))
- PR: [#704](https://github.com/au-research/raid-au/pull/704)
- Related: RAID-521 (made the list endpoint include a service point's own embargoed RAiDs), RAID-877 (scoped `service-point-user:<groupId>` credentials)

## Problem

`iam/doc/role-permissions.md` said three times that a `service-point-user` cannot read embargoed
RAiDs, even from its own service point. The code allows it. The mismatch surfaced while drafting
an integrator tutorial, where it would either hide data that is visible or promise a protection
that does not exist.

## Decision

The code is correct and the documentation was wrong. Rob Leney confirmed this on 2026-10-07.

- RAID-521 (commit `d5d78710`, March 2026) deliberately set the list behaviour: a
  service-point-user sees all of its service point's RAiDs, open and embargoed.
- Owners must read their embargoed RAiDs to manage them during the embargo period, and the write
  path already lets them edit those RAiDs.
- The document was AI generated in July 2025 (commit `d8dca92c`), before RAID-521, and was
  internally inconsistent.

## Read paths checked

| Path | Implementation | service-point-user, own embargoed RAiD |
|------|----------------|----------------------------------------|
| `GET /raid` (list) | `RaidIngestService.findAllByServicePointIdOrHandleIn` → `RaidRepository.findAllViewable` | Included: no access-type filter when the caller holds `service-point-user` |
| `GET /raid/{handle}` and sub-paths | `RaidAuthorizationService.createReadAccessManager` (`anyOf`) | Granted by `servicePointOwner`, which does not check access type. `anyServicePointUserUnlessEmbargoed` denies it alone, but `anyOf` takes the grant |

The same review found that `raid-admin` and `raid-user` can also read embargoed RAiDs listed in
their `admin_raids` / `user_raids` claims, on both paths. The document had marked these
"non-embargoed".

## What changed

- `iam/doc/role-permissions.md`: corrected the role permission lists, the Embargo Protection
  section and the access control matrix.
- `RaidAuthorizationService`: corrected the Javadoc on `anyServicePointUserUnlessEmbargoed`.
- `RaidRepositoryTest`: two tests capture the `findAllViewable` WHERE condition and assert
  whether the access-type filter is present. The previous tests only checked `selectFrom(RAID)`.
  A mutation check (inverting the `isServicePointUser` branch) fails both.
- `RaidAuthorizationServiceTest`: a raid-admin can read an embargoed RAiD listed in `admin_raids`.

There is no change to runtime behaviour.

## Verification

- Unit tests pass. The full local intTest suite ran with 256 tests, 0 failures and 16 skipped.
- Live check in the test environment as `raid-test-user` (`service-point-user`):
  - `GET /raid/` returned 500 RAiDs, of which 462 were embargoed (500 is the
    `MAX_EXPERIMENTAL_RECORDS` cap).
  - `GET /raid/10.82841/67b6ac79`, an embargoed RAiD, returned HTTP 200.
