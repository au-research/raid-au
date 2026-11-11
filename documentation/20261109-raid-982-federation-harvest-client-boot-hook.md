# RAID-982: Federation harvest client created at boot

- JIRA story: [RAID-982](https://ardc.atlassian.net/browse/RAID-982)
- Sub-tasks:
  - [RAID-1042](https://ardc.atlassian.net/browse/RAID-1042): record the decisions in the ADR, [#774](https://github.com/au-research/raid-au/pull/774)
  - [RAID-1043](https://ardc.atlassian.net/browse/RAID-1043): boot hook and unit tests, [#776](https://github.com/au-research/raid-au/pull/776)
  - [RAID-1044](https://ardc.atlassian.net/browse/RAID-1044): real-Keycloak integration suite, [#777](https://github.com/au-research/raid-au/pull/777)
  - [RAID-1045](https://ardc.atlassian.net/browse/RAID-1045): one-off demo verification (no PR; results are recorded on RAID-982)
  - [RAID-1047](https://ardc.atlassian.net/browse/RAID-1047): log the JWKS URL without its query string, [#778](https://github.com/au-research/raid-au/pull/778)
  - [RAID-1046](https://ardc.atlassian.net/browse/RAID-1046): agency guidance, [#779](https://github.com/au-research/raid-au/pull/779)
- Story PR into `main`: see the RAID-982 ticket for the link
- Status: sub-tasks Dev Complete. Not yet merged to `main` or released.

## Why

The RAiD Registration Authority's Federation API harvests every registration
agency's public RAiDs. Until now that needed a Keycloak client created by hand in
each agency's realm, and a client secret sent back to ARDC. The product owner
requires creation to be automatic before any non-ARDC agency is onboarded.

The design is in
[`doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md`](../doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md).
`raid-iam.jar` creates the client at boot, and the client authenticates with a
signed JWT (`private_key_jwt`) checked against ARDC's published keys, so no
secret crosses an organisational boundary.

## What changed

- **Boot hook** (`iam/src/main/java/au/org/raid/iam/provider/federation/`).
  - On `PostMigrationEvent` it creates or repairs `raid-federation-harvester` in
    every realm that has a `raid-dumper` role.
  - Settings: `client-jwt` (RS256), service accounts only, 300-second tokens,
    full scope, and the realm's default scopes on create.
  - The service account holds only `raid-dumper` and the realm default role. Any
    other role or group is removed, and any stored secret is cleared.
  - It never re-enables a disabled client, because disabling is the agency's way
    to stop harvesting.
  - It never blocks or fails Keycloak startup.
- **Key source.**
  - The default JWKS URL is `https://keys.raid.org/federation/jwks.json`.
  - `RAID_FEDERATION_HARVEST_JWKS_URL` overrides it. The URL must be https unless
    `RAID_FEDERATION_HARVEST_ALLOW_HTTP_JWKS_URL=true`.
  - `RAID_FEDERATION_HARVEST_USE_JWKS_URL=false` uses a key set bundled in the
    jar instead. That set ships empty until the Registration Authority's keys
    exist.
- **Reachability check.** After boot, a background fetch warns if the JWKS URL may
  be unreachable. It uses Keycloak's own HTTP client, so it honours the same proxy
  and truststore settings.
- **Logging.** Log messages show the JWKS URL without its query string, fragment
  or user info (RAID-1047).
- **Tests.**
  - Unit tests for every class.
  - A new `iam` `intTest` suite (Testcontainers, 22 tests) runs the Keycloak
    version in `iam/Dockerfile` with the built jar and the shipped realm. It also
    runs in CI through the root `./gradlew intTest`.
- **Docs.**
  - The ADR's implementation decisions table.
  - The client section in `iam/doc/keycloak-configuration.md`.
  - The agency guide
    [`doc/reference/federation-harvest-client.md`](../doc/reference/federation-harvest-client.md).

## Verified

- **Integration suite, 22/22.** It covers:
  - the client's configuration, roles and token contents
  - rejection of replayed, expired, wrong-audience, wrongly signed and
    secret-based requests
  - restart idempotency, and drift repair that leaves a disabled client disabled
  - the JWKS URL override, an unreachable JWKS, the embedded key set, and a
    query-bearing URL that is used but never logged
- **Key eviction is per node.** On two clustered nodes, an unknown-kid request to
  one node left the retired key accepted on the other until that node got its own
  request. The ADR records this.
- **Demo, 2026-11-09 (RAID-1045).**
  - The hook created the client on first boot, so no hand-made client existed.
  - A signed assertion got a token, and `GET /raid/all-public` returned 200.
  - `GET /raid/` returned 403.
  - Demo was then reverted to `main`. The client remains in the demo realm,
    trusting a deleted test URL, so it can't be used until this story reaches
    `main`.

## Before and after release

- **Demo and test need a JWKS URL override.** No CDK in `raido-v2-aws-private`
  sets `RAID_FEDERATION_HARVEST_JWKS_URL` yet. Until the key host exists
  (RAID-983) this is harmless: every instance logs the "may be unreachable"
  warning. A non-production instance must set the override before the
  production key set is published, or it will trust the production Federation
  API. There is no non-production JWKS URL yet.
- **ARDC instances only until RAID-980 is in production.** The harvester reads
  `GET /service-point/public`, which RAID-980 adds.
- **Merging RAID-975 will conflict.** Both stories change
  `ClientCredentialControllerResourceProviderFactory.postInit` and its
  `register(notNull())` test. Whichever merges second resolves the conflict.
- **The ADR status is still "proposed".** Update it when the product owner
  accepts the design.

Proposed changelog entry for the release:

```markdown
## IAM
* Keycloak now creates the `raid-federation-harvester` client at startup in any realm with a
  `raid-dumper` role. The RAiD Registration Authority uses it to harvest public RAiDs into the
  Federation API. It authenticates with a signed JWT, so no secret is exchanged, and it can read
  only public RAiDs and public service point details. Non-production instances must set
  `RAID_FEDERATION_HARVEST_JWKS_URL`. To stop harvesting, disable the client; do not delete it.
  Until the Registration Authority publishes its keys, Keycloak logs a warning at startup that
  the JWKS at `keys.raid.org` may be unreachable; this does not affect anything else. See
  `doc/reference/federation-harvest-client.md`.
```

## Follow-ups not ticketed

- Older `iam` tests still contain ticket numbers in comments (credential, group
  and CORS tests).
- The boot hook and the ORCID updater's client provisioner (RAID-1011) duplicate
  client set-up code that could become a shared helper.
