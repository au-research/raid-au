
RAiD uses Keycloak as its Identity and Access Management (IAM) server.
Keycloak handles all OAuth2/OIDC authentication flows, including federation
to external identity providers (AAF via SATOSA/SAML, ORCID).

The api-svc acts as an OAuth2 Resource Server, validating Keycloak-issued
JWTs on each API request using Spring Security's built-in support.

[oauth2_api-token_exchange.md](../authentication/oauth2_api-token_exchange.md)
shows how the OAuth2/OIDC authentication flow works with Keycloak.


# Terminology

## OAuth2/OIDC terms
* access_token
  * Keycloak issues RS256-signed JWTs as access tokens. These contain the
    user's roles (in the `realm_access.roles` claim) and custom claims such
    as `service_point_group_id`, `admin_raids`, and `user_raids`.
  * The api-svc validates these tokens against Keycloak's JWKS endpoint.
* id_token
  * The OIDC id_token identifies the user. Keycloak issues this alongside
    the access_token during the authorization code flow.
* client_id
  * The OAuth2 client identifier registered in Keycloak. The frontend app
    uses `raid-agency-app` and the api-svc uses `raid-api-2`.
* redirect_uri
  * The URI Keycloak redirects the user back to after authentication.
    Configured per-client in the Keycloak realm.
  

## RAiD terms
* service-point
* role (Keycloak realm roles such as `service-point-user`, `operator`, etc.)

See [authorization](../authorization) for details about these terms and how 
the RAiD authorization scheme works.

## Generic cryptography terms

This is more a list of things you should understand before diving into doing any
serious identity work:

### JWTs / bearer tokens
* HMAC 
  * https://en.wikipedia.org/wiki/HMAC
* JWTs and bearer tokens
  * https://jwt.io/introduction
* Keycloak-issued tokens are JWTs with both standard OIDC claims and custom
  claims (e.g. `service_point_group_id`, `admin_raids`, `user_raids`)
* Encryption vs Signature verification
  * JWTs are secured via _signature_, not encrypted
  * the bearer tokens are protected _in transit_ via encryption, provided at the 
  protocol level by TLS (HTTPS)


### Asymmetric crypto
* https://en.wikipedia.org/wiki/Public-key_cryptography
* Keycloak uses _asymmetric_ cryptographic signatures (RS256)
  * Keycloak signs tokens with its private key
  * the api-svc verifies JWT signatures using Keycloak's public key,
    retrieved via the JWKS standard endpoint
    (certificate retrieval is secured via TLS)
  
