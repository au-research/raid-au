### Scoped service point admin roles

* Status: final
* Who: proposed and finalised by RL
* When: 2026-07-06
* Related: RAID-711 (epic), RAID-712 (story), RAID-719/720/721/722/723/724 (implementation)


# Decision

The flat `group-admin` realm role historically made a user an admin of *every*
Keycloak group (service point) they belonged to. RAID-712 scopes service point
admin authority to a single service point using **dynamic realm roles** named
`service-point-admin:<groupId>`, where `<groupId>` is the Keycloak group UUID —
the same value as the group's `groupId` attribute, the `service_point_group_id`
JWT claim, and the `service_point.group_id` database column.

1. **Dynamic scoped realm roles.** One realm role per service point group,
   created on demand (create-if-absent) by the IAM group SPI. Realm roles flow
   into `realm_access.roles` in the access token via the built-in `roles`
   client scope, so the Spring API's existing `extractAuthorities` picks them
   up as `ROLE_service-point-admin:<groupId>` authorities with no mapper or
   claim changes.

2. **Dual-write during transition.** Group creation and the group-admin
   PUT endpoint grant both the flat `group-admin` role and the scoped role;
   the group-admin DELETE endpoint revokes both. This keeps the legacy app
   (which reads the flat role) working while consumers migrate.

3. **Scoped enforcement with a flat fallback.** The SPI authorises group-admin
   operations per group via `isGroupAdminOf(user, groupId)`: true if the user
   holds the scoped role, or — while the `RAID_FLAT_GROUP_ADMIN_FALLBACK`
   environment flag is enabled (default `true`) — if the user holds the flat
   role *and* is a member of the group. Operators bypass. Once the app
   consumes scoped roles, the fallback is disabled and the flat role removed.

4. **Idempotent migration endpoint.** Deployed realms (test/stage/prod) are
   backfilled with the operator-only endpoint
   `POST /realms/raid/group/migrate-service-point-admins`, which grants
   `service-point-admin:<groupId>` for every group each flat `group-admin`
   user belongs to. This deliberately **over-grants** to preserve current
   effective access (a flat admin was an admin of all their groups); pruning
   to a minimal grant set is a deferred cleanup. Local dev realms are seeded
   via `iam/realms/raid-realm.json` instead.

# Alternatives considered

* **Keycloak subgroups** (e.g. an `admins` child group per service point):
  rejected — group membership is already overloaded to mean service point
  association, subgroup membership does not surface in tokens without extra
  mappers, and the SPI/API changes would be larger.
* **A custom token claim listing administered group IDs** (like `admin_raids`):
  rejected — the `admin_raids` claim caused the >8KB JWT header incident.
  Scoped realm roles are bounded (one per group an admin belongs to, typically
  a handful) and reuse the standard `realm_access.roles` pathway.
* **API-side database authorisation only**: rejected — group administration
  happens through the IAM SPI inside Keycloak, which has no view of the API
  database; the authority must live in Keycloak.

# Consequences

* The API gained vocabulary only (RAID-723: `SERVICE_POINT_ADMIN_ROLE_PREFIX`,
  `getAdministeredGroupIds`, an `isServicePointAdmin()` AuthorizationManager);
  nothing is wired to endpoints until the app migrates.
* Token size impact is bounded and small; no new mappers were added.
* Follow-ups: app-side consumption of scoped roles, then disabling the
  fallback, removing the flat role, and pruning migration over-grants.
