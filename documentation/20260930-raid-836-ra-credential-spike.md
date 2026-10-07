# RAID-836 Spike: How can each RAiD Service automatically generate appropriately-scoped client credentials for the Federation API to harvest from it?

- **Ticket:** [RAID-836](https://ardc.atlassian.net/browse/RAID-836) (Spike), parent epic [RAID-739](https://ardc.atlassian.net/browse/RAID-739)
- **Related:** [RAID-752](https://ardc.atlassian.net/browse/RAID-752) (federation ingest), [RAID-884](https://ardc.atlassian.net/browse/RAID-884) (boot-time provisioning hook), [RAID-825](https://ardc.atlassian.net/browse/RAID-825)/[RAID-826](https://ardc.atlassian.net/browse/RAID-826)/[RAID-827](https://ardc.atlassian.net/browse/RAID-827) (in-process credential SPI)
- **Pull request:** [#706](https://github.com/au-research/raid-au/pull/706)
- **ADR:** [`doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md`](../doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md) (proposed)
- **Evidence:** [`iam/probe/private-key-jwt/`](../iam/probe/private-key-jwt/README.md); test-environment client `raid-836-signed-jwt-probe` in `iam.test.raid.org.au` realm `raid` (token with `raid-dumper`, `GET /raid/all-public` HTTP 200, 2029 RAiDs; client and service account deleted on 30 September 2026)
- **Proposal for review:** [`20261007-raid-836-federation-harvest-credentials-proposal.md`](./20261007-raid-836-federation-harvest-credentials-proposal.md)
- **Date:** 30 September 2026 (first draft 9 September 2026)
- **Author:** Rob Leney

## Summary

Each RAiD Service can create its own harvest client automatically, on any platform, from logic in `raid-iam.jar` that runs at every Keycloak boot. This hook is already built and verified for RAID-884. If the client authenticates with a **signed JWT** (`private_key_jwt`) against a public key ARDC publishes, **no secret needs to be handed back at all**. The agency's one manual step becomes telling ARDC its base URL.

This has been verified against Keycloak 26.6.2 in an isolated container. It is a proposal for review, not yet a decision.

## Problem

The Federation API's ingest handler (`raid-aws-private`, `handler/federation-ingest/src/source/http-agency-source.ts`) gets a client-credentials token per agency, for a client holding the `raid-dumper` role, using a `client_secret` from Secrets Manager. ARDC staff created every such client by hand.

For production, ARDC has no administrative access to another agency's infrastructure, and there can be **no manual steps during or after deployment**. The credential must come into existence automatically, on any platform. The only accepted manual step is the agency sending the credential to ARDC once.

## Question 1: What lets a RAiD Service self-generate a deployment-agnostic, appropriately-scoped credential?

**A `PostMigrationEvent` listener in the IAM SPI that creates the client if it is absent.** The 9 September draft proposed this but had not verified it. It has since been built and verified for RAID-884 ([ADR](../doc/adr/2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md)).

- On Keycloak 26.6.2, it fires on first boot, again on every restart, and after realm import.
- It is running in demo and sandbox. In demo it repaired drift that the manual migration step had left behind.

The harvest client is a second use of the same mechanism, with the same rules: idempotent, failures swallowed per realm, and no realm name configured. The client would be created in any realm that has a `raid-dumper` role.

`iam/realms/raid-realm.json` declares a `raid-dumper` client, but it is imported only on local-dev first boot. It is not a deployment mechanism and is not part of the answer.

## Question 2: Where should this logic live?

**In the app codebase (the IAM SPI), not deployment tooling.** ARDC does not control agencies' CDK, Compose or Kubernetes definitions. ARDC does control `raid-iam.jar`, which every agency runs. RAID-884 has now demonstrated this in practice.

## Question 3: Does this overlap with RAID-825/826/827?

**Different problem; the low-level code transfers.** RAID-827 authorises a human Service Point Admin to create a credential in ARDC's own realm. RAID-836 has no caller to authorise: it is unattended bootstrap in a realm the agency fully administers.

What transfers is RAID-827's in-process client creation (`ClientCredentialController`), including two traps it found. `session.clients().addClient()` leaves `fullScopeAllowed` false, and it skips the realm's default client scopes. Either one silently strips the role from the token. The harvest client needs both set.

It should be a separate piece of work, not an extension of RAID-827.

## Credential scope

- **Its own client.** The harvester gets a dedicated client holding the `raid-dumper` role, separate from any `raid-dumper` client an agency already uses for Zenodo archiving. Sharing one credential would couple two consumers' rotation, revocation and audit.
- **No rename.** Renaming the existing client is not recommended.
- **Scope is no broader than today's.** In the probe, the issued token carried `raid-dumper` and did not carry an unrelated role granted in the same realm. It also carried the realm's default roles (`default-roles-<realm>`, `offline_access`, `uma_authorization`). Every service account carries these, including today's `raid-dumper` client.

## Question 4: Handback path for the credential

### Finding: signed JWT removes the secret from the handback

With a client secret, the secret must cross from the agency to ARDC. That needs a secure channel, a second copy of the secret, and a repeat exchange on every rotation.

Keycloak's `client-jwt` authenticator avoids this:

1. **At boot:** the agency's boot hook creates the client configured to trust a JWKS URL (a published set of public keys) hosted by ARDC for the Federation API.
2. **At harvest time:** the Federation API signs a short-lived assertion with a private key held in AWS KMS, and exchanges it for a token.
3. **Handback:** the agency sends ARDC its base URL, which isn't secret. The client ID is fixed and compiled into the jar.

### Evidence: probe against `quay.io/keycloak/keycloak:26.6.2`

Run on 30 September 2026. Every check behaved as expected.

| Result | What was shown |
|--------|----------------|
| Works | Token issued; `aud` may be the realm issuer or the token endpoint; token holds `raid-dumper` and not the decoy role |
| Rejected | Replayed assertion (`Token reuse detected`), unknown key, forged `kid`, expired assertion, wrong audience, `client_secret`, no authentication, disabled client |
| Rotation | A key added to the JWKS was picked up without a restart |
| Caveat | A key removed from the JWKS was **still accepted** from Keycloak's cache |
| Fallback | Embedding the public certificate in the client instead of a JWKS URL also works (assertion must have no `kid`, or a thumbprint `kid`) |

### Existing ARDC secret-intake mechanism

**Not confirmed.** I found no reference to an approved mechanism for receiving secrets from external parties in the Slack channels I can search. That doesn't prove there isn't one, so ARDC Services still needs to be asked.

The answer now matters less. Under the recommended design, it only matters for any agency that has to fall back to a client secret.

### Yopass

**Not evaluated in depth.** It is only relevant if the client-secret route is kept. It should be assessed only if the signed-JWT route is rejected at review.

## Risks and open items

A second probe pass and a code read on 30 September 2026 answered most of these.

1. **Revocation lag: answered.** A key removed from the JWKS stays cached, but the Registration Authority can evict it itself. An assertion with an unknown `kid` forces a refetch, which replaces the cached key set, and the retired key is then rejected (probe T11b to T11d). Otherwise cached keys drop after an hour unused (Keycloak 26.6.2 `KEYS_CACHE_MAX_IDLE_SECONDS = 3600`). **Not verified:** multi-node agencies, where each node has its own cache.
2. **Issued tokens: answered.** The RAiD API validates tokens locally (`SecurityConfig`: `oauth2ResourceServer().jwt()` against `issuer-uri`), so a leaked token is usable until it expires. The realm default is 24 hours in test. A per-client `access.token.lifespan` is honoured (T16), so the harvest client can have a short one.
3. **Fallback error code: answered.** Every rejected key returns HTTP 400 `invalid_client`. A disabled client returns HTTP 401 `invalid_client`.
4. **Several keys on one client: answered.** A client's `jwks.string` attribute holds a key set with no URL, and both keys were accepted (T15). The embedded fallback should use it to ship the current and next keys in each release. That also removes any need to generate a certificate.
5. **Impact of a compromised key: answered, with a new finding.** `/raid/all-public` returns open-access records only (`RaidRepository.findAllPublic`). Embargoed records need `raid-access-handler`, and `raid-dumper` is in none of the read rules for individual RAiDs. However, `raid-dumper` also grants `GET /service-point/**` (`SecurityConfig`), and service point records include admin and technical contact email addresses. The harvester doesn't need that. It should be reviewed in its own ticket.
6. **Outbound HTTPS.** Each agency's Keycloak must reach the JWKS URL at harvest time. The embedded key set covers agencies that block egress.
7. **The probe used the Admin REST API.** The boot hook will use the model layer, which behaves differently (see Question 3). Its integration test must assert on a real issued token.
8. **Dormant client secret.** The Admin REST API generated one on every `client-jwt` client. The model-layer path generates a secret only when explicitly asked, so the boot hook should have none; its test should assert that.
9. **Default composite.** In test, `default-roles-raid` holds only Keycloak's defaults. An agency that adds API roles to its default composite passes them to every service account, as it already does today. This is worth a line in agency guidance.
10. **ARDC Services secret intake: still open.** It needs a person to ask, and only matters if a secret-based path is kept.
11. **Decisions to settle:**
   - the fixed client ID (the probe's `raid-federation-harvester` is a placeholder);
   - the production JWKS URL;
   - the name of the environment variable that overrides it.

## Proposed follow-up work (to scope after review with Matthias)

Suggested order: publish the keys first, then ship the client, then switch the harvester.

1. **ARDC-side keys (`raid-aws-private`).** A KMS asymmetric signing key, plus a published JWKS endpoint for the Federation API.
2. **IAM SPI (`raid-au/iam`).** Boot-hook provisioning of the harvest client. It uses `client-jwt`, a default JWKS URL with an environment override, `fullScopeAllowed`, the default client scopes, and no secret. Integration test on a real token.
3. **Ingest handler (`raid-aws-private`).** Sign client assertions through KMS. Keep `client_secret` support while the demo agencies (ARDC, SDSC) remain on their hand-made clients.
4. **Revocation.** Establish the Keycloak key-cache lifetime, and write the agency-facing kill-switch guidance.

## Key references

- `iam/src/main/java/au/org/raid/iam/provider/group/ServicePointAdminRoleBootstrapper.java` (RAID-884 boot hook)
- `iam/src/main/java/au/org/raid/iam/provider/credential/ClientCredentialController.java` (RAID-827 in-process client creation and its traps)
- `doc/adr/2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md`
- `raid-aws-private`: `handler/federation-ingest/src/source/http-agency-source.ts` (current `client_secret` flow)
- [RFC 7523](https://datatracker.ietf.org/doc/html/rfc7523), JWT profile for OAuth 2.0 client authentication
