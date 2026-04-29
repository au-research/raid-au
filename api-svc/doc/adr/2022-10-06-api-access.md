### Use simple pre-shared API keys for api-svc access  

* Status: final
* Who:  finalised by STO
* When: finalised on 2022-10-06
* Related: no related ADRs


# Decision

Use simple pre-shared API keys for api-svc access


# Context

The two alternatives that seem most useful are:
* simple API key based access, same as RAID legacy uses, same as handler server
  etc.
* OAuth2 "client credentials" flow

https://stackoverflow.com/a/64944707/924597

I think the OAuth2 flow is what we should do long term.
Choosing the simple API key approach though is going to cause problems with
migration later.  OTOH, choosing the more complicated OAuth2 flow will cause
problems with adoption.

---

**Status note (2026-04):** The pre-shared API key model has been superseded.
The api-svc now uses OAuth2/OIDC via Keycloak as its authentication and
authorisation mechanism. `SecurityConfig` configures Spring Security with
`oauth2ResourceServer` (JWT validation) and `oauth2Login`. Keycloak realm
roles (e.g. `raid-user`, `operator`, `service-point-user`) are mapped to
Spring Security granted authorities for endpoint-level access control.
The simple API key approach described in this ADR is no longer in use.