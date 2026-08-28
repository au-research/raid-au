# RAID-827: Self-service scoped client credentials

- Story: [RAID-827](https://ardc.atlassian.net/browse/RAID-827) (epic
  [RAID-711](https://ardc.atlassian.net/browse/RAID-711))
- Sub-tasks: [RAID-845](https://ardc.atlassian.net/browse/RAID-845),
  [RAID-846](https://ardc.atlassian.net/browse/RAID-846),
  [RAID-847](https://ardc.atlassian.net/browse/RAID-847),
  [RAID-848](https://ardc.atlassian.net/browse/RAID-848),
  [RAID-849](https://ardc.atlassian.net/browse/RAID-849)
- Follow-ups raised: [RAID-851](https://ardc.atlassian.net/browse/RAID-851),
  [RAID-853](https://ardc.atlassian.net/browse/RAID-853)
  (RAID-850 was created then descoped)
- Source investigation: [RAID-767](https://ardc.atlassian.net/browse/RAID-767)
- PRs (all into the `feature/RAID-827` story branch): #625, #626, #629, #630, #631
- ADRs: `doc/adr/2026-08-26_scoped-client-credential-quota.md`,
  `doc/adr/2026-08-27_in-process-spi-credential-lifecycle.md`

## What changed and why

RAID-827 lets a Service Point Admin self-serve their own service point's API
client credentials (create, list, rotate, revoke, get-secret) instead of
asking ARDC staff. The work spanned five sub-tasks on a single story branch,
each with its own PR into that branch; this documents the whole story.

### The design turned over twice under scrutiny

Both turns are worth recording, because the original ticket assumptions did not
survive contact with the code.

1. **No broker (RAID-845).** RAID-827 as written specified a "brokered scoped
   service account": a confidential client holding realm-wide `manage-clients`
   that would act on the user's behalf, on the premise that a Service Point
   Admin's token lacks the `realm-management` permissions to create a client.
   That premise only constrains callers *outside* Keycloak. These endpoints are
   a Keycloak SPI, which holds a `KeycloakSession` and reaches the model layer
   directly, in-process — Keycloak enforces `manage-clients` in its Admin REST
   endpoints, which an SPI never passes through. `GroupController` already
   creates groups, creates realm roles on demand and grants role mappings with
   no service account, proving the point. So the broker was dropped: no broker
   client, no `manage-clients` grant anywhere. This also eliminated the
   realm-wide-credential residual risk the story had accepted, rather than
   merely bounding it, and removed all provisioning work.

2. **Rate limiting deferred, cap shipped (RAID-849).** The story's open
   "decide rate-limiting behaviour" task resolved to: defer per-request rate
   limiting until demonstrated need (it is expensive to build in the SPI and
   the need is unproven), and instead cap active credentials per service point,
   which is far cheaper and portable across every deployment. RAID-850 (an
   AWS-only WAF rule) was raised then descoped on the same reasoning, since most
   registration agencies are not on AWS.

### What shipped

- **RAID-846** — `ClientCredentialController` at `/realms/raid/client-credential`
  (create/list/rotate/get-secret/revoke), in-process via the Keycloak model
  layer. Authorisation is a uniform Operator short-circuit applied independently
  of a scoped `service-point-admin:<groupId>` check; the flat legacy
  `group-admin` role is deliberately not honoured. Each credential is a
  confidential client whose service account holds only
  `service-point-user:<groupId>`, never an admin role. Ownership, label and
  timestamps are stored as client attributes so isolation and listing filter on
  stored data, and a `raid.credential.managed` marker keeps the realm's own
  clients out of reach. The per-service-point cap of 10 active credentials
  returns 409.
- **RAID-847** — `Cache-Control: no-store` on every secret-returning response,
  and an audit trail (`au.org.raid.iam.audit`, dedicated logger name) recording
  actor, timestamp, credential and service point but structurally incapable of
  recording a secret. Also fixed a pre-existing leak: `Cors` logged the whole
  JAX-RS `Response` at debug and Jackson serialises `getEntity()`, so every SPI
  response body — including credential secrets — was reaching the application
  log.
- **RAID-848** — an integration suite proving the RAID-827 acceptance criteria
  end to end through real calls to the SPI, the Keycloak token endpoint and the
  Admin API. See the defects it found, below.

### Three runtime defects RAID-846 had, found by RAID-848

RAID-846's unit tests passed and `javap` confirmed the Keycloak APIs existed,
but the feature did not work at all in a running Keycloak. Exercising a real
credential exposed three defects, none reachable by unit tests because each
lives in Keycloak behaviour the tests mock:

1. `new ClientManager()` (no-arg) leaves `realmManager` null;
   `enableServiceAccount` dereferenced it — **500 on every create**. Fixed to
   `new ClientManager(new RealmManager(session))`.
2. `session.clients().addClient()` does not apply the realm's default client
   scopes (the Admin API path does), so the client had no `roles` scope and
   granted realm roles never reached the token. Fixed by attaching the realm's
   own defaults.
3. `addClient` also leaves `fullScopeAllowed=false`, so Keycloak filtered the
   scoped role out of the token even once the `roles` scope was present. Fixed
   to `setFullScopeAllowed(true)`.

Each now has a unit test that fails when the defect is reintroduced.

## Verification

- iam unit tests: 183, 0 failures. Deny paths and the leak guard were
  mutation-tested (reintroducing the defect fails the test).
- Full API intTest suite: 43 classes, 221 tests, 0 failures, run against a
  stack rebuilt from the story branch.
- **Live on iam.test.** The story build was deployed to the shared test
  Keycloak (`scripts/deploy-iam-to-test.sh`) and a credential was minted
  through the SPI as a scoped Service Point Admin, then used: the
  `client_credentials` grant succeeded and the token carried the correct
  `service_point_group_id` and `service-point-user:<groupId>` role, with no
  admin role. All test residue was deleted afterwards.

The iam.test deploy also answered two questions the local realm could not: the
test realm does carry the `service_point_group_id` client scope, and it lists
`roles` among its default client scopes (which is why defect 2 above was masked
on test but not locally — the fix matters because the code, not the realm, must
be correct in every deployment).

## Revocation semantics

Revoke disables the client (`setEnabled(false)`), not deletes it — chosen for
idempotency, an audit trail, and to reject rotation of a revoked credential.
This satisfies the AC ("disabled or deleted"). Verified on iam.test that revoke
is reversible: a realm admin re-enabling the client restores the original
secret. So "revoked" currently means "dormant, secret intact" rather than
"secret destroyed". This is recorded on RAID-827 as a decision point, with
deletion and forced-rotation-on-re-enable flagged as possible future
requirements.

## Notable decisions and constraints

- **Deployment portability.** Most registration agencies are not on AWS, so
  only application code and the shipped realm config reach every deployment.
  This drove the cap over a WAF (RAID-849) and shaped how follow-ups were
  framed.
- **No new infrastructure, no migrations.** Because the broker was dropped,
  there is nothing to provision in any realm.

## Follow-ups not in this story

- [RAID-851](https://ardc.atlassian.net/browse/RAID-851) (High) — enable
  Keycloak realm brute-force protection; the token endpoint is currently
  unthrottled for these very credentials.
- [RAID-853](https://ardc.atlassian.net/browse/RAID-853) —
  `service_point.enabled` / `app_writes_enabled` appear unenforced in the API
  (found while implementing RAID-846, which is why its "reject creation against
  a disabled service point" AC was dropped: the SPI has no view of the API
  database).
- Deletion and rotation-on-re-enable semantics for revoked credentials
  (recorded on RAID-827).
- No log routing is configured for the `au.org.raid.iam.audit` logger in any
  environment yet.
- Denied actions are not audited (only successful ones).
- Nothing yet consumes `service-point-user:<groupId>` on the API side, so a
  minted credential is correctly scoped but cannot yet authorise against the
  RAiD API.
- `KeycloakClient` in the intTest harness logs at Feign `FULL`, so credential
  secrets appear in intTest logs (test-only).
