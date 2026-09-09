# RAID-877: Fix scoped service-point-user credential authorization

- Ticket: [RAID-877](https://ardc.atlassian.net/browse/RAID-877)
- Related: [RAID-827](https://ardc.atlassian.net/browse/RAID-827) (mints the
  scoped `service-point-user:<groupId>` role this bug affects),
  [RAID-712](https://ardc.atlassian.net/browse/RAID-712) (prior art for
  scoped `service-point-admin:<groupId>` roles)
- PR: _to be added once opened_
- ADR: `doc/adr/2026-09-09_claim-anchored-scoped-service-point-user-authorization.md`
- Reference doc updated:
  `doc/reference/service-point-client-credentials.md` (new "What a credential
  can and cannot do" section)

## What changed and why

Client-credential tokens minted with a scoped `service-point-user:<groupId>`
realm role (the role RAID-827 mints for a service point's self-serve API
credentials) were rejected with `403 insufficient_scope` on every RAiD data
API endpoint, even though the token was minted correctly with the right
`service_point_group_id` claim and role. This made every credential created
via RAID-827 unusable against the actual RAiD API.

The root cause was three separate, independent role-check mechanisms in the
RAiD API, each doing exact string matching against the *flat*
`service-point-user` role/authority, none of which recognised a *scoped*
role:

1. `SecurityConfig.extractAuthorities(Jwt)` mapped every realm role verbatim,
   so a scoped role became the authority `ROLE_service-point-user:<groupId>`
   — never the flat `ROLE_service-point-user` that `hasAnyRole(...)` checks.
2. `RaidAuthorizationService.hasRole()` did exact authority-string equality
   against `"ROLE_" + role`.
3. `TokenUtil.hasRole(String)` read the raw `realm_access.roles` claim
   directly, bypassing Spring Security's authorities. This one was also a
   data-correctness bug, not just an authorization bug: it feeds
   `RaidIngestService.findAllByServicePointIdOrHandleIn`'s `isServicePointUser`
   flag, which controls whether `GET /raid/` truncates closed/embargoed
   records to open-access-only — so even a credential that somehow passed
   the other two checks would have had its own non-open-access records
   incorrectly hidden from itself.

All three had to be fixed together to close the bug without leaving a caller
authorised for the endpoint but still data-truncated, or vice versa.

### Code review follow-up (2026-09-09)

An "approve with comments" review raised one duplicate-authority edge case and
several test-quality suggestions, all addressed before merge:

- `extractAuthorities` could add the flat `ROLE_service-point-user` authority
  twice when a token legitimately carries both the flat role and a matching
  scoped role (once from the verbatim per-role mapping, once from the
  synthesis). Guarded with a `contains()` check before adding.
- The `WARN` log for a scoped-but-non-matching role was demoted to `debug` —
  nothing in the stack rate-limits requests, so a misconfigured credential
  polling the API would otherwise spam WARN-level logs indefinitely.
- `SERVICE_POINT_USER_ROLE_PREFIX` is now defined as
  `SERVICE_POINT_USER_ROLE + ":"` instead of repeating the `"service-point-user"`
  string literal, so the two constants can't drift apart.
- `RaidAuthorizationService.anyServicePointUserUnlessEmbargoed` was widened
  from `private` to package-private so its dedicated test could call it
  directly instead of via reflection.

### The fix: claim-anchored authority normalisation

`SecurityConfig.extractAuthorities(Jwt)` — the one place with access to both
the raw roles and the token's other claims — now synthesises the flat
`ROLE_service-point-user` authority in addition to the verbatim per-role
authorities, but only when a `service-point-user:<suffix>` role's non-blank
suffix exactly equals the token's own `service_point_group_id` claim
(fail-closed: no claim, blank claim, or a mismatched suffix all mean no flat
authority is synthesised, and a `WARN` is logged naming the role but not the
full token). `TokenUtil.hasRole(String)` was changed to read
`SecurityContextHolder`'s authorities instead of the raw claim, so it
observes the same synthesised authority. `RaidAuthorizationService.hasRole()`
needed no functional change, since it already checks the flat authority that
now exists for a matching scoped credential; its Javadoc was updated to
explain why it now works for scoped credentials.

Full design rationale and alternatives considered are in the ADR.

### Verification that a service-account caller doesn't break `getRaidPermissions`

Before writing any code, per the ticket's blocking requirement, verified
that `RaidIngestService.findAllByServicePointIdOrHandleIn` calling
`keycloakService.getRaidPermissions(TokenUtil.getUserId())` with a
service-account user id does not throw or 500 in the IAM SPI: Keycloak
creates a dedicated `service-account-<clientId>` user for every
client-credentials-enabled client, so `session.users().getUserById(realm,
userId)` resolves it like any other user, and
`user.getAttributeStream(...)` for the admin/user RAiDs attributes returns
an empty stream (not null, not throwing) when the attribute is unset. No fix
was needed for this path.

## Tests

- `SecurityConfigTest` (12 tests) covering `extractAuthorities`: flat-only
  roles unchanged, scoped role with matching/mismatched/absent claim, blank
  suffix never matches, multiple scoped roles, a flat role alongside a
  non-matching scoped role, a flat role alongside a *matching* scoped role
  (the duplicate-authority regression from the review — asserts exactly one
  `ROLE_service-point-user` authority, not two), unrelated roles pass
  through, and empty/absent `realm_access.roles`.
- `TokenUtilTest` proving `hasRole` reads authorities, not the raw claim,
  using a deliberately-disagreeing authority/claim fixture.
- Extended `RaidAuthorizationServiceTest` with a parallel
  `ScopedServicePointUserTests` nested class: read/write/patch on own service
  point, denial for a different service point, embargoed-read denial via
  `anyServicePointUserUnlessEmbargoed` (called directly now that it's
  package-private, no reflection), allow via `servicePointOwner` for the
  credential's own embargoed record, and — replacing three tests the review
  flagged as byte-for-byte duplicates of pre-existing `shouldAllowServicePointOwner`
  cases — a fail-closed test asserting a token holding *only* the scoped role
  `service-point-user:<groupId>`, with no flat authority, is denied by all
  three access managers. That's the regression guard for a future change that
  "helpfully" adds prefix-matching to `RaidAuthorizationService.hasRole`.
- Extended `RaidIngestServiceTest` with the regression guard the ticket
  specifically called out: `isServicePointUser=true` is correctly derived
  from the normalised credential authority (and `false` without it).
- Fixed two collateral `UnnecessaryStubbingException`s under strict Mockito
  stubbing caused by the `TokenUtil.hasRole` rewrite (`RaidServiceTest`'s
  `update()`/`noUpdateWhenNoDiff()` no longer need to stub
  `token.getClaims()`) and one in the new
  `RaidAuthorizationServiceTest.ScopedServicePointUserTests` (an unused
  `servicePointService.findByGroupId` stub, since the embargo check short-
  circuits before that call).
- Extended `ClientCredentialIntegrationTest` (intTest) with a new
  `DataApiAccess` nested class calling the actual RAiD data API with
  credential tokens against real, pre-existing fixture service point groups
  (dynamically-created SPI groups lack a backing `service_point` row needed
  to mint): `GET /raid/` succeeds, minting lands ownership in the
  credential's own service point, read/update/patch succeed on an
  owned record, access to another service point's record is denied
  (asserted as exactly `403`, not via the shared 401-or-403 `assertDenied`
  helper, since a 401 there would mean the token was rejected outright — a
  different bug from the tenancy-isolation failure this test targets; the
  test is commented to note it's paired with the owned-record test above to
  isolate the tenancy signal specifically), and — the most important new
  assertion — a **non-open-access** raid owned by the credential's own
  service point still appears in `GET /raid/` results (regression guard for
  the `isServicePointUser` truncation bug). The v2 `AccessTypeIdEnum` has no
  `CLOSED` value, so this test uses the default `EMBARGOED` access type
  instead, with an explicit assertion confirming the minted type is not in
  the open-access allow-list.

Full `./gradlew intTest` (all modules, not just the new/changed test
classes) and `./gradlew build` both actually ran to completion (not just
compiled) and were green locally after all changes, including the review
follow-up. The `DataApiAccess` intTest class mints and revokes client
credentials against the shared, persistent `raid-au` fixture group
(`RAID_AU_GROUP_ID`), which is capped at 10 active credentials — its
`@AfterEach` cleanup revokes what it creates but swallows exceptions
silently, so as part of this review follow-up the active-credential count
against that group was checked directly against Keycloak's database
(`client_attributes` rows for `raid.credential.group-id` joined to `client`,
filtered to `enabled = true`) before and after a full `intTest` run: 0 active
before, 0 active after (4 new credentials created and fully revoked, on top
of 8 pre-existing already-revoked ones from prior runs). Cleanup is reliable
for a normal run; the silent-swallow in `@AfterEach` remains a latent risk if
an assertion failure ever prevents a credential from being revoked
mid-test, but that's a pre-existing pattern in this test class, not something
introduced by RAID-877.

## Explicitly out of scope (left untouched, flagged for separate tickets)

Per the ticket's instruction, the following pre-existing issues discovered
during investigation were **not** fixed here:

- A pre-existing role-check bug in `RaidService.getPermissions`.
- `GET /raid/count` has no ownership check on its `servicePointId` query
  parameter.
- `addHandleToAdminRaids` grows unbounded for service accounts.
- `SERVICE_POINT_GROUP_ID_CLAIM` is declared independently in three separate
  files.
