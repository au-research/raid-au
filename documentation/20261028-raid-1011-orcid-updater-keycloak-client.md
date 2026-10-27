# RAID-1011: Keycloak creates the ORCID updater's client at boot

- JIRA: [RAID-1011](https://ardc.atlassian.net/browse/RAID-1011), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-task: [RAID-1005](https://ardc.atlassian.net/browse/RAID-1005) (PR [#748](https://github.com/au-research/raid-au/pull/748)), which gave the updater its signing key and JWKS URL
- PR: [#749](https://github.com/au-research/raid-au/pull/749) (targets `feature/RAID-975`)
- Status: PR open

## What changed and why

The ORCID updater calls the RAiD API as the Keycloak client `orcid-updater`,
using `private_key_jwt` with its own JWKS (RAID-1005). Rob required the
client credentials to be created without manual intervention, so Keycloak
now creates the client itself.

- A boot hook in the IAM SPI jar, `OrcidUpdaterClientBootstrapper`, runs on
  Keycloak's `PostMigrationEvent`, the same hook as the RAID-884 role
  backfill. It is registered from
  `ClientCredentialControllerResourceProviderFactory.postInit`.
- The RA turns it on by setting `RAID_ORCID_UPDATER_JWKS_URL` in Keycloak's
  environment to `<updater URL>/.well-known/jwks.json`. Unset, it does
  nothing, because the updater is optional for an RA. An invalid URL is
  logged and ignored.
- `OrcidUpdaterClientProvisioner` works in every realm that holds
  `service-point-user`, so master is left alone. It:
  - creates `contributor-writer` if the realm lacks it (the shipped realm
    export does not define it);
  - creates `orcid-updater` as a confidential, service-account-only client
    with signed-JWT authentication (`client-jwt`, RS256, keys from the JWKS
    URL), full scope and the realm's default client scopes;
  - grants its service account `contributor-writer`.
- On every later boot it puts back anything that has drifted, including a
  changed JWKS URL, and leaves the client's enabled flag alone so an
  operator can still switch it off. No secret is generated, set or handed
  back.
- A failure is logged per realm and never stops Keycloak starting.
- The local stack (`iam/docker-compose.yaml`) sets the URL to the updater's
  `bootRun` on the host, so local development needs no manual step either.

## Testing

- IAM unit tests: 220 pass (18 new). `OrcidUpdaterClientProvisionerTest`
  (10) covers create, no secret, the missing role, non-RAiD realms, an
  unchanged restart, a changed URL, drift with the enabled flag kept, a
  missing service account, and URL validation.
  `OrcidUpdaterClientBootstrapperTest` (7) covers the event filter, every
  realm, trimming, unset and invalid URLs, and failures not propagating. The factory
  test checks `postInit` registers the hook. Built with `--release 17`.
- Live check on a throwaway Keycloak 26.6.2 with the built jar, the RAiD
  realm and a local JWKS:
  - first boot created the client in `raid` only; master has none;
  - settings and the service account's `contributor-writer` were as above;
  - an assertion signed with the published key got a token with
    `azp=orcid-updater` and `contributor-writer` in `realm_access.roles`;
  - an assertion signed with another key, and a request with no assertion,
    were refused (400 `invalid_client`);
  - after a restart nothing was created or updated again, and the token
    still worked.
- RAID-1005's `ServiceTokenKeycloakIntTest` already showed the updater's own
  assertion is accepted by a `client-jwt` client on Keycloak 26.6.2.
- API integration tests against the rebuilt local stack (Keycloak with the
  new jar, which logged creating the client): 256 run, 16 skipped, 0
  failures.

## Documentation

- `iam/doc/keycloak-configuration.md`: the `orcid-updater` client, the
  setting, and `contributor-writer` in the realm roles table.
- `iam/doc/keycloak-production-clients.md`: `orcid-updater`, not yet set in
  any ARDC environment.
- `orcid-updater/README.md`: what the RA sets for Keycloak.
- `doc/adr/2026-10-23_per-ra-orcid-updater.md`: how the client is created,
  under "Callers and credentials" and in the consequences.
