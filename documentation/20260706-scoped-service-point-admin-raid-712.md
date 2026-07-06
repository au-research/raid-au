# RAID-712: Scope Service Point Admin role to a single service point

**Date:** 2026-07-06
**Epic:** [RAID-711](https://ardc.atlassian.net/browse/RAID-711) — RAiD permissions and RBAC improvements
**Story:** [RAID-712](https://ardc.atlassian.net/browse/RAID-712)

## What changed and why

The flat `group-admin` Keycloak realm role made a user an admin of every
service point (Keycloak group) they belonged to. This work scopes that
authority to a single service point via dynamic realm roles named
`service-point-admin:<groupId>`, with full backward compatibility while the
RAiD app still reads the flat role. See the ADR:
[`doc/adr/2026-07-06_scoped-service-point-admin-roles.md`](../doc/adr/2026-07-06_scoped-service-point-admin-roles.md).

Backend only (Keycloak SPI + Spring API vocabulary). The app-side switch to
scoped roles, disabling the flat fallback, and pruning migration over-grants
are follow-up tickets.

## Sub-tasks and PRs

| Ticket | Change | PR |
|---|---|---|
| [RAID-719](https://ardc.atlassian.net/browse/RAID-719) | SPI: create + grant scoped role on group creation (dual-write) | [#551](https://github.com/au-research/raid-au/pull/551) |
| [RAID-722](https://ardc.atlassian.net/browse/RAID-722) | Dev realm JSON: seed scoped roles for the two dev groups; role-permissions docs | [#552](https://github.com/au-research/raid-au/pull/552) |
| [RAID-720](https://ardc.atlassian.net/browse/RAID-720) | SPI: per-group enforcement via `isGroupAdminOf`; `RAID_FLAT_GROUP_ADMIN_FALLBACK` flag (default true); dual-write/dual-revoke on group-admin PUT/DELETE | [#553](https://github.com/au-research/raid-au/pull/553) |
| [RAID-723](https://ardc.atlassian.net/browse/RAID-723) | API: `SERVICE_POINT_ADMIN_ROLE_PREFIX`, `getAdministeredGroupIds`, `isServicePointAdmin()` AuthorizationManager (additive, unwired) | [#554](https://github.com/au-research/raid-au/pull/554) |
| [RAID-721](https://ardc.atlassian.net/browse/RAID-721) | SPI: operator-only idempotent `POST /group/migrate-service-point-admins` backfill | [#556](https://github.com/au-research/raid-au/pull/556) |
| [RAID-724](https://ardc.atlassian.net/browse/RAID-724) | Integration tests: cross-SP admin matrix, new-group access, migration gating/idempotency, dual-write/dual-revoke | [#557](https://github.com/au-research/raid-au/pull/557) |

Final PR (`feature/RAID-712` → `main`): [#558](https://github.com/au-research/raid-au/pull/558).

## Deployment notes

- In test/stage/prod, run `POST /realms/raid/group/migrate-service-point-admins`
  as an operator after deploying, to backfill scoped roles. Idempotent — safe
  to re-run; a second run reports `grantsAdded: 0, rolesCreated: 0`.
- The flat fallback stays enabled (`RAID_FLAT_GROUP_ADMIN_FALLBACK` unset or
  `true`) until the app consumes scoped roles.
- Local dev: the realm JSON seeds scoped roles on first boot only. When
  testing IAM SPI changes locally, rebuild the `raid-iam` image —
  `./gradlew dockerComposeUp` can silently run a stale image.

## Testing

- iam unit suite: 100+ tests including the scoped/fallback authorisation
  matrix and the `addRole(id, name)` overload regression guard.
- raid-api unit suite: 765+ tests including scoped-authority parsing.
- Integration: `GroupServicePointAdminIntegrationTest` (11 tests) against the
  live stack; full `intTest` suite green (159 executed, 0 failures).

## Follow-up tickets

- [RAID-727](https://ardc.atlassian.net/browse/RAID-727) — app-side
  consumption of scoped roles, then disable fallback, remove flat
  `group-admin`, prune migration over-grants.
- [RAID-728](https://ardc.atlassian.net/browse/RAID-728) —
  `GroupController.createGroup` is not transactional; a failure after group
  creation can leave partial state.
- [RAID-729](https://ardc.atlassian.net/browse/RAID-729) —
  `ServicePointService.create` (API) does not create the corresponding
  Keycloak group (pre-existing gap).
