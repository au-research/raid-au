# private_key_jwt probe (RAID-836)

A throwaway check that Keycloak accepts signed-JWT client authentication
([RFC 7523](https://datatracker.ietf.org/doc/html/rfc7523), `private_key_jwt`)
for a service-account client holding the `raid-dumper` role. It is the evidence
behind the RAID-836 findings and the ADR
[`doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md`](../../../doc/adr/2026-09-30_federation-harvest-client-uses-private-key-jwt.md).

Re-run it on any Keycloak major upgrade before relying on the mechanism.

```bash
./iam/probe/private-key-jwt/run-probe.sh
```

Requires Docker, Python 3 (standard library only) and `openssl`. It starts
`quay.io/keycloak/keycloak:26.6.2` in dev mode on port 8095 and a JWKS server on
port 8765, then removes both. Exit code 0 means every check behaved as expected.

The fixture is created through the Admin REST API. The real implementation
creates the client in-process from the `PostMigrationEvent` boot hook, which
behaves differently (see the ADR), so this probe proves the authenticator, not
the provisioning code.

| Check | Expected |
|-------|----------|
| T1, T2 | Token issued with `aud` = realm issuer or token endpoint; carries `raid-dumper` and not `decoy-admin` |
| T3 | Replayed assertion (same `jti`) rejected |
| T4, T5 | Key absent from the JWKS, or a forged `kid`, rejected |
| T6, T7 | Expired assertion, or wrong audience, rejected |
| T8, T9 | `client_secret`, or no client authentication, rejected |
| T10 | A new key added to the JWKS is picked up without a restart |
| T11 | Observational: a key removed from the JWKS is **still accepted** from Keycloak's cache |
| T11b, T11c, T11d | An assertion with an unknown `kid` forces a refetch; the retired key is then rejected and the current key still works |
| T12, T12b, T12c | Embedded certificate: arbitrary `kid` rejected; no `kid` or SPKI thumbprint `kid` accepted |
| T13 | Embedded-certificate client rejects a different key |
| T15, T15c | A client holding two keys in `jwks.string` (no URL) accepts both and rejects a third |
| T16 | A per-client `access.token.lifespan` of 300 seconds is honoured |
| T14 | Disabling the client stops token issue immediately |

Every rejected key returns HTTP 400 with `error=invalid_client`; the
`error_description` varies. A disabled client returns HTTP 401 `invalid_client`.

Not tested here: the key cache's idle expiry. Keycloak 26.6.2 defines
`KEYS_CACHE_MAX_IDLE_SECONDS = 3600` in `InfinispanConnectionProvider`
(`keycloak-model-infinispan`), so cached keys unused for an hour are dropped.
