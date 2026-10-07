### Federation harvest client authenticates with private_key_jwt, provisioned at boot

* Status: proposed (for review with the product owner)
* Who: proposed by RL
* When: 2026-09-30
* Related: RAID-836 (spike), RAID-739 (Federation API epic), RAID-752 (federation
  ingest), RAID-884 and
  [2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md](./2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md)
  (the boot hook this builds on), RAID-827 (in-process client creation reference)


# Context

The Federation API harvests each Registration Agency's `GET /raid/all-public`
using a client-credentials token for a client holding the `raid-dumper` role. The
ingest handler (`raid-aws-private`,
`handler/federation-ingest/src/source/http-agency-source.ts`) authenticates with a
`client_secret` read from AWS Secrets Manager. Today every agency's client and
secret were created by hand by ARDC staff.

For production, the product owner has set three constraints (RAID-836):

1. No manual steps during or after deployment. The credential must come into
   existence automatically.
2. It must work on any deployment platform, with no central ARDC-hosted broker
   provisioning on an agency's behalf.
3. The only accepted manual step is the agency handing the credential to ARDC.

The first two are met by the verified `PostMigrationEvent` boot hook (RAID-884):
logic in `raid-iam.jar` creates the client on every boot if it is absent.

With `client_secret`, constraint 3 leaves a shared secret that must cross an
organisational boundary. That needs a secure intake channel, and the secret then
lives in two places, is valid until someone rotates it by hand, and must be
re-sent on every rotation.


# Decision

The boot hook creates a dedicated federation-harvest client that authenticates
with **signed JWT client assertions** (`private_key_jwt`,
[RFC 7523](https://datatracker.ietf.org/doc/html/rfc7523); Keycloak's `client-jwt`
authenticator), not a client secret.

* **Client.** A dedicated client, separate from any existing `raid-dumper` client
  an agency uses for Zenodo archiving, holding only the `raid-dumper` realm role.
  Its client ID is a fixed, well-known value compiled into the jar, so ARDC knows
  it without being told. The value is to be settled in the implementation ticket;
  `raid-federation-harvester` is used in the probe as a placeholder only.
* **Keys.** The client trusts a JWKS URL published by ARDC for the Federation
  API. The Federation API signs each assertion with a private key held in AWS KMS,
  which never leaves KMS. The JWKS URL is compiled in as the default and can be
  overridden by an environment variable, read with `System.getenv()` as the SPI's
  other config is.
* **Which realms.** Following the boot-hook ADR, no realm name is configured. The
  client is created in any realm that has a `raid-dumper` role; other realms,
  including `master`, are left alone.
* **Handback.** Nothing secret is handed back. The agency tells ARDC its base URL
  (and token URL, if it differs from the convention), which ARDC needs anyway to
  register the agency.

This was verified against `quay.io/keycloak/keycloak:26.6.2` on 2026-09-30 with
[`iam/probe/private-key-jwt/`](../../iam/probe/private-key-jwt/README.md). A
client-credentials token was issued from a JWKS-URL client and carried
`raid-dumper` and not an unrelated role. Replayed, expired, wrong-audience and
wrongly-signed assertions were rejected. So were `client_secret` and
unauthenticated requests. A key added to the JWKS was picked up without a restart,
and disabling the client stopped token issue immediately.


# Alternatives considered

* **`client_secret` plus a secure handback channel.** The baseline. It works, but
  it keeps a long-lived shared secret crossing an organisational boundary, depends
  on a secret-intake mechanism whose existence at ARDC is unconfirmed, and makes
  every rotation a manual, two-party exchange.
* **Keys embedded on the client instead of a JWKS URL.** Verified to work, both
  as a single certificate (probe T12b and T12c) and as a key set in the client's
  `jwks.string` attribute (T15). It removes the agency's outbound call to ARDC,
  but a rotation then depends on agencies deploying a new `raid-iam.jar`. Kept as
  the fallback for agencies whose Keycloak cannot make outbound HTTPS calls, using
  `jwks.string` so each release can carry the current and next keys and no
  certificate has to be generated.
* **Mutual TLS client authentication (`tls_client_auth`).** Not tested. Rejected
  on reasoning: it needs the client certificate to reach Keycloak intact, which
  depends on how each agency terminates TLS in front of it (load balancer, ingress,
  reverse proxy). That is exactly the per-agency deployment detail ARDC cannot
  control.


# Consequences

Good:

* No secret crosses an organisational boundary, so the secret-handback question
  largely falls away. The agency's one manual step is sending non-secret details.
* ARDC rotates keys by publishing a new key in the JWKS. No agency action and no
  restart are needed.
* Assertions are short-lived and single-use (`jti` replay is rejected), so an
  intercepted assertion is worth very little.
* An agency can cut off harvesting at any time by disabling the client.

Costs and risks:

* **Removing a key from the JWKS does not revoke it on its own.** Keycloak caches
  fetched keys, and a removed key was still accepted afterwards (probe T11). Two
  things bound this. The Registration Authority can force eviction itself: an
  assertion with an unknown `kid` makes Keycloak refetch, the refetch replaces the
  cached key set, and the retired key is then rejected (T11b to T11d). Without
  that, Keycloak 26.6.2 drops cached keys after an hour unused
  (`KEYS_CACHE_MAX_IDLE_SECONDS = 3600`). Rotation therefore ends with one
  eviction request per agency. Each Keycloak node keeps its own cache, so a
  multi-node agency needs the request to reach every node; this is unverified.
* **Issued tokens outlive revocation.** The RAiD API validates access tokens
  locally (Spring resource server with the realm's signing keys), so a token stays
  usable until it expires. The realm default is 24 hours in test. Keycloak honours
  a per-client `access.token.lifespan` (T16), so the harvest client gets a short
  one without changing the realm.
* **The fallback keys on `invalid_client`.** Every rejected key returned HTTP 400
  `invalid_client`; the description varies and is not a contract. The handler
  retries with the previous key only on that response, only during the grace
  period, and with a fresh assertion.
* **Agencies' Keycloak needs outbound HTTPS to the JWKS URL.** Where that is
  blocked, the embedded-certificate fallback applies.
* **The ingest handler changes.** It must build and sign assertions through KMS
  instead of reading a secret. This is ARDC-side work in `raid-aws-private`.
* **A dormant secret may exist.** The Admin REST API generated a client secret for
  every `client-jwt` client in the probe. The `client-jwt` authenticator does not
  accept it (probe T8), but it would become live if anyone switched the
  authenticator. The model-layer `addClient()` path used by RAID-827 generates a
  secret only when explicitly asked to, so the boot hook should create none; its
  integration test should assert that.
* **`raid-dumper` is broader than harvesting.** Besides `GET /raid/all-public`
  (open-access records only; embargoed records need `raid-access-handler`), the
  role grants `GET /service-point/**`, whose records include admin and technical
  contact email addresses. The harvester does not need this. Scope is still no
  broader than the existing role, as the spike requires, but the grant should be
  reviewed in its own ticket.
* **The probe used the Admin REST API; the boot hook will not.** RAID-827 found
  that `session.clients().addClient()` leaves `fullScopeAllowed` false and skips
  the realm's default client scopes, which silently strips roles from the token.
  The boot hook must set both, and its integration test must assert that
  `raid-dumper` appears in `realm_access.roles` of a real issued token.
* **Default roles.** The issued token also carries the realm's default roles
  (`default-roles-<realm>`, `offline_access`, `uma_authorization`), as every
  service account does, including today's `raid-dumper` client. Scope is therefore
  no broader than the existing role, provided an agency has not put API roles into
  its default composite. The implementation should assert this in the test realm.
* This rests on Keycloak's `client-jwt` authenticator as verified on 26.6.2.
  Re-run the probe on a Keycloak major upgrade.
