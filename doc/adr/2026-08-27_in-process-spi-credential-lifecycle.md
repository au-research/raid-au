### Manage client credentials in-process in the IAM SPI, with no broker service account

* Status: final
* Who: proposed and finalised by RL
* When: 2026-08-27
* Related: RAID-711 (epic), RAID-827 (story), RAID-845 (this ADR),
  RAID-846 (implementation), RAID-767 (source investigation)
* Supersedes: the brokered service account design recorded against RAID-827
  ("Decision (2026-08-17): brokered scoped service account (Option B)")


# Context

RAID-827 lets a Service Point Admin self-serve their service point's API client
credentials: create, list, rotate, revoke and get-secret.

Its stated problem is that "a Service Point Admin's app-level token carries none
of the `realm-management` permissions needed to create a Keycloak client or
assign it a role". From that, RAID-827 concluded a privileged intermediary was
required, and specified a confidential client holding `manage-clients` that
would perform the work on the user's behalf. Its acceptance criteria name this
broker twice, including a scenario in which "the broker service account cannot
authenticate to Keycloak's Admin API".

That premise is true only of callers **outside** Keycloak. It does not hold for
a Keycloak SPI.


# Decision

Implement the credential lifecycle **in-process in the IAM SPI**, in the same
way `GroupController` already implements group and role administration. Do not
create a broker client, and do not grant `manage-clients` to anything.

1. **Authorisation is the SPI's own role check.** Follow `GroupController`
   exactly: authenticate with `AppAuthManager.BearerTokenAuthenticator`, then
   `if (!isOperator(user) && !isGroupAdminOf(user, groupId)) throw new
   NotAuthorizedException(...)`, where `isGroupAdminOf` requires the scoped
   `service-point-admin:<groupId>` realm role. RAID-827 additionally requires
   the flat legacy `group-admin` role be rejected here, and the Operator check
   be a uniform short-circuit rather than an OR-ed clause.

2. **Mutations go through the model layer**, not the Admin REST API:
   `session.clients().addClient(...)`, `KeycloakModelUtils.generateSecret(...)`,
   `ClientManager.enableServiceAccount(...)`,
   `session.users().getServiceAccount(client)` to grant the scoped
   `service-point-user:<groupId>` role. Revoke is `setEnabled(false)` or
   `removeClient`. Rotate reissues the secret on the existing client.

3. **Ownership and labels are client attributes.** Store the owning groupId and
   the human-readable label via `ClientModel.setAttribute`, so list and
   isolation checks filter on the owning groupId rather than on naming
   convention.


# Why this works

Keycloak enforces `manage-clients` and the other `realm-management` roles in
its **Admin REST endpoints**. An SPI resource never passes through that layer:
it holds a `KeycloakSession` and reaches the model layer directly, in-process.
There is no Admin API call, therefore nothing to authenticate as, therefore no
service account and no role grant.

The broker exists to answer "how does something acquire Admin API permissions".
In-process, that question does not arise.

`GroupController` is the proof. It imports only `org.keycloak.models.*` and
`AppAuthManager`, holds no service account, and already performs mutations that
are more privileged than creating a client:

* `session.groups().createGroup` / `removeGroup`
* `realm.addRole(...)`, creating realm roles on demand
* `grantRole` / `deleteRoleMapping` against *other* users
* `session.users().getRoleMembersStream(...)`, enumerating every holder of a role

Creating a realm role and granting it to an arbitrary user mutates the
authorisation graph itself, which is a greater capability than minting a
client.

The required API surface was verified against the pinned Keycloak **26.5.6**
artefacts before this decision was taken: `ClientProvider.addClient`,
`ClientModel.setSecret` / `setServiceAccountsEnabled` / `setAttribute` /
`setEnabled`, `UserProvider.getServiceAccount`,
`ClientManager.enableServiceAccount` and `KeycloakModelUtils.generateSecret`
all exist. `ClientManager.createClient(KeycloakSession, RealmModel,
ClientRepresentation)` is static and is the same path the Admin REST API uses
internally, so prefer it over hand-rolled construction.


# Alternatives considered

* **Broker confidential client holding `manage-clients` (the original RAID-827
  design):** rejected. It solves a problem the SPI does not have, and it
  introduces a realm-wide credential that can read, modify or delete *any*
  client in the realm, including other service points' credentials and the RAiD
  app's own clients. RAID-827 accepted that as residual risk. Declining the
  broker removes the risk rather than accepting it.

* **Implementing the endpoints in `api-svc` (Spring) against the Admin REST
  API:** rejected. From outside Keycloak a broker genuinely would be required,
  reinstating the realm-wide credential. It would also split service point
  administration across two codebases, when group and role administration
  already lives in the SPI and this is the same kind of work on the same
  objects.

* **Granting Service Point Admins `manage-clients` directly:** rejected, and
  never seriously on the table. The role is realm-wide, so it would make every
  Service Point Admin an administrator of every client in the realm.


# Consequences

* **No provisioning work.** Nothing to create in the dev realm and nothing to
  propagate to test, stage or prod. This also removes the open question of how
  a non-AWS agency deployment would receive a broker client, since there is no
  broker to receive.
* **The residual risk RAID-827 accepted is eliminated, not merely bounded.** No
  realm-wide `manage-clients` credential will exist, so a defect in the
  isolation check cannot be escalated through an over-privileged account.
  Application code remains the isolation boundary, but there is no longer a
  standing credential behind it to abuse.
* **Two acceptance criteria scenarios become moot** and should be removed from
  RAID-827: the one requiring the operation be performed by the broker service
  account, and the broker-unavailable scenario.
* **Partial-state protection improves.** RAID-827 requires that a failure leave
  no half-created client. A broker issuing sequential Admin API calls has no
  transaction and genuinely can. In-process model mutations participate in the
  request's Keycloak transaction, which is how `GroupController#createGroup`
  safely performs five mutations in sequence. This should be asserted in the
  RAID-848 suite rather than assumed.
* **Dependency on internal Keycloak API deepens slightly.**
  `ClientManager` and `KeycloakModelUtils` are internal and can shift between
  minor versions, and the module already runs with a compile/runtime skew
  (built against 26.5.6, running on 26.6.2 per `iam/Dockerfile`). This is not
  new in kind, since `GroupController` already imports
  `org.keycloak.services.managers`, but it is more surface. Keycloak version
  bumps must re-verify this path.

# Note on provenance

The original broker decision is attributed in RAID-827 to "the ADR ticket",
which is referenced without a key and was not located when this ADR was
written. If that ticket records reasoning not addressed above, this decision
should be revisited against it.
