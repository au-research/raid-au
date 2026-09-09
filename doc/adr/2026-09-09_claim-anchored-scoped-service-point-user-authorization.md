### Claim-anchored authorization for scoped service-point-user roles

* Status: final
* Who: proposed and finalised by RL
* When: 2026-09-09
* Related: RAID-877 (bug), RAID-827 (in-process credential SPI, mints the scoped role), RAID-712/[[2026-07-06_scoped-service-point-admin-roles.md]] (prior art for scoped admin roles)

# Context

Client-credential tokens minted by the RAID-827 in-process credential SPI
carry a scoped realm role `service-point-user:<groupId>` (mirroring the
`service-point-admin:<groupId>` pattern from RAID-712), plus a
`service_point_group_id` claim identifying the caller's own service point.
Every one of these tokens was rejected with `403 insufficient_scope` on
every RAiD data API endpoint, even though the token was minted correctly.

The root cause was that the RAiD API's authorization checks for the flat
`service-point-user` role are implemented in **three separate places**, each
doing exact string matching, and none of them recognised a scoped role:

1. `SecurityConfig.extractAuthorities(Jwt)` mapped every
   `realm_access.roles` entry verbatim into `ROLE_<value>`, so a scoped role
   became the authority `ROLE_service-point-user:<groupId>` — never the flat
   `ROLE_service-point-user` that `hasAnyRole(SERVICE_POINT_USER_ROLE)`
   checks against on `POST/GET /raid/**` and `GET /service-point/**`.
2. `RaidAuthorizationService.hasRole()` (used by the composed
   `AuthorizationManager`s for read/write/patch access) did exact authority
   equality against `"ROLE_" + role`.
3. `TokenUtil.hasRole(String)` read the raw `realm_access.roles` claim
   directly (bypassing Spring Security's authorities entirely), used by
   `RaidIngestService.findAllByServicePointIdOrHandleIn` to decide
   `isServicePointUser`, which controls whether `GET /raid/` truncates
   closed/embargoed records to open-access-only for the caller.

All three had to be fixed together, or a scoped credential would pass some
checks and silently fail others (for example: authorised to call the
endpoint, but still have its own embargoed records truncated out of the
result).

# Decision

**Claim-anchored authority normalisation.** In
`SecurityConfig.extractAuthorities(Jwt)` — the one place with access to both
the raw roles and the token's other claims — keep emitting the verbatim
`ROLE_<value>` authority for every realm role (unchanged, so nothing else
that reads a scoped authority directly is affected), and additionally
synthesise the flat `ROLE_service-point-user` authority, but **only** when:

* a role of the form `service-point-user:<suffix>` is present, **and**
* `<suffix>` is non-blank, **and**
* `<suffix>` exactly equals the token's own `service_point_group_id` claim.

If a scoped `service-point-user:*` role is present but no match is found
(missing/blank claim, or a suffix that disagrees with the claim), no flat
authority is synthesised and a `WARN` is logged (role name only, no token
contents) — every downstream denial in that case would otherwise surface as
an unhelpful `insufficient_scope` with no clue why.

Because the synthesis happens once, up front, in the JWT-to-authorities
conversion, the three downstream checks did not need parallel role-parsing
logic added to each of them:

* `RaidAuthorizationService.hasRole()` needed no change — it already checks
  for the flat `ROLE_service-point-user` authority, which now exists for a
  matching scoped credential.
* `TokenUtil.hasRole(String)` was changed to read
  `SecurityContextHolder`'s authorities (`"ROLE_" + role`) instead of the
  raw `realm_access.roles` claim, so it observes the same synthesised
  authority rather than bypassing it. This was a necessary companion fix,
  not an optional cleanup: it was the third and final place doing its own
  exact-match role check against the raw claim.

Implementation lives in `SecurityConfig.SecurityConstants` (a new
`SERVICE_POINT_USER_ROLE_PREFIX` constant) and
`SecurityConfig.extractAuthorities`/`hasMatchingScopedServicePointUserRole`.
Suffix parsing uses `startsWith`/`substring`, not `split(":")`, so a group id
containing a colon cannot corrupt parsing.

# Alternatives considered

* **Prefix-matching directly inside each of the three checks** (teach
  `hasRole`/`hasAnyRole`/`TokenUtil.hasRole` to recognise a
  `service-point-user:` prefix): rejected — this would triple the surface
  area for the same fail-closed claim comparison, and two of the three
  checks (`hasAnyRole` in `SecurityConfig`'s request matchers) are Spring
  Security built-ins that cannot be taught custom prefix logic without
  replacing them with `AuthorizationManager`s, which is a much larger change
  than the bug warrants.
* **A parallel `getServicePointUserGroupIds` helper**, mirroring
  `RaidAuthorizationService.getAdministeredGroupIds` from the RAID-712 admin
  work: rejected for this bug. That helper exists to let an admin
  administer *multiple* groups by group id; a service-point-user credential
  is scoped to exactly one service point (its own, per the
  `service_point_group_id` claim), so there is nothing to enumerate — a
  single boolean match at authority-extraction time is sufficient and
  simpler.
* **Unconditionally flattening any `service-point-user:*` role to the flat
  role, ignoring the claim**: rejected — this would grant flat
  service-point-user access to any caller holding a scoped role for *any*
  group, not just their own, defeating the purpose of scoping the role in
  the first place.

# Consequences

* A client-credential token scoped to `service-point-user:<groupId>` now
  passes `GET/POST/PUT/PATCH /raid/**` and `GET /service-point/**`
  authorization checks when, and only when, its `service_point_group_id`
  claim matches the role's suffix.
* `RaidIngestService.findAllByServicePointIdOrHandleIn` correctly derives
  `isServicePointUser=true` for such a credential via the now-authority-based
  `TokenUtil.hasRole`, so the credential's own closed/embargoed records are
  no longer incorrectly truncated out of `GET /raid/` results — this was
  previously not just an authorization bug but also a data-correctness bug
  for any credential that did pass authorization by other means.
* A scoped role that does not match the claim now logs a `WARN` identifying
  the misconfiguration, rather than only ever showing up as a bare 403 to
  the caller.
* No change to the flat `service-point-user` role's existing behaviour for
  human users; this only affects tokens carrying the scoped role.
* Left deliberately unfixed by this change (raised separately, out of scope
  for RAID-877): a pre-existing role-check bug in `RaidService.getPermissions`;
  `GET /raid/count` not checking ownership of its `servicePointId` query
  parameter; unbounded growth of `addHandleToAdminRaids` for service
  accounts; and the `SERVICE_POINT_GROUP_ID_CLAIM` constant being declared
  independently in three files.
