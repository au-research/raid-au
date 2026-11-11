# Federation harvest client: guidance for registration agencies

The RAiD Registration Authority, run by ARDC, harvests the public RAiDs of every
registration agency into the RAiD Federation API. It does this with a Keycloak
client called `raid-federation-harvester`, which `raid-iam.jar` creates in your
Keycloak when it starts.

This guide explains what the client is, the few things you must do or avoid, and
how to stop harvesting if you need to.

## What the client is

- **Created for you.** Every time Keycloak starts, `raid-iam.jar` creates the
  client in any realm that has a `raid-dumper` role, and puts its settings back
  if they have changed. You do not create it by hand and it is not in the realm
  export.
- **No secret.** The client authenticates with a signed JWT (`private_key_jwt`).
  ARDC signs each request with a private key that never leaves ARDC. Your
  Keycloak checks the signature against ARDC's published public keys. Nothing
  secret is exchanged between your organisation and ARDC.
- **Public data only.** The client's service account holds the `raid-dumper`
  role and your realm's default role, nothing else. With it, the harvester can
  read `GET /raid/all-public` (open-access RAiDs) and `GET /service-point/public`
  (service point id, name and identifier owner). It cannot read embargoed
  RAiDs, service point contact details, or anything else.
- **Short-lived tokens.** Tokens issued to the client last 300 seconds.

| Setting | Value |
| --- | --- |
| Client ID | `raid-federation-harvester` |
| Client authenticator | Signed JWT (`client-jwt`), RS256 |
| Flows | Service accounts only |
| Keys | Fetched from a JWKS URL (default `https://keys.raid.org/federation/jwks.json`) |
| Access token lifespan | 300 seconds |
| Service account roles | `raid-dumper` and the realm default role |

## Onboarding: send ARDC two URLs

Your only manual step is to tell ARDC where to reach your instance. Send:

1. **Your RAiD API base URL**, for example `https://api.raid.example.org`.
2. **Your Keycloak token URL.** Use the `token_endpoint` value your Keycloak
   publishes at `https://<your Keycloak host>/realms/<realm>/.well-known/openid-configuration`.
   For a realm named `raid` it normally looks like
   `https://<your Keycloak host>/realms/raid/protocol/openid-connect/token`.

Both URLs must be reachable from the internet over HTTPS. Neither is secret, so
email is fine.

## Check the client was created

After you deploy a release that includes the harvest client, check either of
these:

- **Keycloak log.** At startup, look for a line like:

  ```
  Federation harvest: realm 'raid': client 'raid-federation-harvester' created, signed-JWT authentication from JWKS URL https://keys.raid.org/federation/jwks.json
  ```

  Later restarts log nothing at INFO level unless something had drifted, in
  which case the line says `updated` instead of `created`.
- **Admin console.** In your realm, open **Clients** and check that
  `raid-federation-harvester` is listed.

Log messages show the JWKS URL without any query string, fragment or user name
and password, because these can hold credentials.

## Do not add API roles to the realm default role

Every service account token, including the harvest client's, carries the
realm's default role (`default-roles-<realm>`) and everything it contains. If
you add a RAiD API role such as `service-point-user`, `operator` or
`raid-admin` to it, the harvester's tokens get that role too, and it is no
longer limited to public data.

To check, open **Realm settings**, then the **User registration** tab, and look
at **Default roles**. It should hold only Keycloak's own roles, such as
`offline_access`, `uma_authorization` and the `account` client roles. Give API
roles to users and groups directly instead.

## Non-production instances must set the JWKS URL

The JWKS URL compiled into `raid-iam.jar` is ARDC's **production** key set. A
test, staging or demo instance that keeps the default would trust the
production harvester instead of ARDC's non-production one.

On every non-production instance, set this environment variable on the Keycloak
container:

| Variable | Value |
| --- | --- |
| `RAID_FEDERATION_HARVEST_JWKS_URL` | The non-production JWKS URL ARDC gives you |

The URL must use HTTPS. If you do not want a non-production instance harvested
at all, disable the client there instead (see below).

## Stop harvesting: disable the client, do not delete it

To stop ARDC harvesting your instance:

1. In your realm, open **Clients**, then `raid-federation-harvester`.
2. Turn **Enabled** off and select **Save**.

Keycloak refuses new tokens straight away. A token issued before you disabled
the client stays valid until it expires, at most 300 seconds later. To resume
harvesting, turn **Enabled** back on.

The startup hook never re-enables a disabled client, so it stays disabled
across restarts and upgrades.

Do not:

- **Delete the client.** The next Keycloak restart creates it again, enabled.
- **Change its settings.** The next restart puts back its authenticator, flows,
  key settings, token lifespan and full-scope setting, and clears any client
  secret. Roles or groups added to its service account are removed at startup.
  Client scopes are attached only when the client is first created, so do not
  remove them: without the realm's default scopes, its tokens lose the
  `raid-dumper` role.

## Things you may see in your logs

- **`Federation harvest: the JWKS ... may be unreachable`.** At startup Keycloak
  checks in the background that it can fetch the JWKS URL. If it cannot, it
  logs this warning but starts normally. Harvesting will fail until Keycloak can
  reach the URL, so allow outbound HTTPS to it, including through any egress
  proxy. Until ARDC publishes the production key set at `keys.raid.org`, every
  instance using the default URL logs this warning.
- **Failed token requests from `raid-federation-harvester`.** When ARDC rotates
  its signing key, it may send a few requests with an unknown key ID. These make
  each Keycloak node refetch the key set so it stops accepting the retired key.
  Keycloak rejects them with `invalid_client`. This is expected and needs no
  action, including on instances that run several Keycloak nodes.

## Instances without outbound HTTPS

If your Keycloak cannot make outbound HTTPS calls, it can use a key set bundled
in `raid-iam.jar` instead of fetching one:

| Variable | Value |
| --- | --- |
| `RAID_FEDERATION_HARVEST_USE_JWKS_URL` | `false` |

The bundled key set is **empty** in current releases, so with this setting
ARDC cannot harvest your instance. Keycloak logs a warning at startup to say
so. A later release will bundle ARDC's keys. With bundled keys, a key rotation
needs you to upgrade `raid-iam.jar`, while the JWKS URL picks up new keys
without any action.

## More detail

- Every client setting and environment variable:
  [`iam/doc/keycloak-configuration.md`](../../iam/doc/keycloak-configuration.md#raid-federation-harvester-created-at-boot-signed-jwt).
- Why the client works this way:
  [`doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md`](../adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md).
