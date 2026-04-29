
> **Note:** This document has been updated to reflect the current Keycloak-based
> authentication architecture. The previous custom api-token exchange flow
> (where api-svc generated its own HS256 JWTs) has been replaced by standard
> Keycloak OAuth2/OIDC token issuance.

This documents a "happy day" sign-in process for a human user.

Authentication is handled entirely by Keycloak (the Identity and Access
Management server). Users authenticate via Keycloak, which federates to
external identity providers (AAF via SATOSA/SAML, ORCID, etc.).
Keycloak issues standard RS256-signed JWTs.

Assume:
* the user has a Keycloak account (either local or federated via AAF/ORCID)
* the user has been assigned to a service-point group with appropriate roles

```mermaid
sequenceDiagram
autonumber
actor user as User<br/>(web browser)
participant app as raid-agency-app<br/>(SPA served from<br/> AWS CloudFront) 
participant keycloak as Keycloak<br/>(iam.{env}.raid.org.au)
participant api as api-svc<br/>(container hosted on<br/>AWS ECS)

user->>app: user navigates to <br/>app.prod.raid.org.au
user->>app: user clicks Sign in
app-->>keycloak: redirect to Keycloak login page
  note right of app: scope: openid<br/>client_id: raid-agency-app

opt Keycloak authentication
  keycloak->>keycloak: user authenticates<br/>(local credentials, AAF, or ORCID)
  keycloak-->>app: redirect back with authorization code
  app->>keycloak: exchange code for tokens
  keycloak->>app: {access_token, id_token, refresh_token}
    note left of keycloak: RS256-signed JWTs containing<br/>realm_access.roles,<br/>service_point_group_id, etc.
end

app-->>app: _
  note right of app: store access_token for use<br/>with API requests

user->>app: user interacts with app
app->>api: GET /raid/{prefix}/{suffix}<br/>{Authorization: Bearer access_token}
api->>api: _
  note right of api: validate JWT signature via<br/>Keycloak JWKS endpoint,<br/>extract roles from realm_access claim
```

---

The Keycloak-issued access token must be included in the `Authorization`
header for every request to an endpoint that requires authorization.

```mermaid
sequenceDiagram
autonumber
actor client as HTTP client<br/>(raid-agency-app, RedBox, etc.)
participant api as api-svc

client->>api: GET /raid/{prefix}/{suffix}<br/>{Authorization: Bearer access_token}
```

The api-svc validates the JWT using Spring Security's OAuth2 Resource Server
support, which verifies the token signature against Keycloak's JWKS endpoint.
Roles are extracted from the `realm_access.roles` claim in the JWT.

See [spring-security-configuration.md](../authorization/spring-security-configuration.md)
for details of how the security filter chain is configured.