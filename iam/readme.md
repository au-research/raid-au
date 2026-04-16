# Keycloak Documentation

## Realm Configuration

- [Adding a Role to a User](doc/add-role-to-user.md) — Step-by-step guide for assigning realm roles to users in Keycloak (with screenshots)

## Development

- [SPIs](doc/spis.md) — Custom `RealmResourceProvider` endpoints for group management and per-RAiD access control
- [Local Development](doc/local-development.md) — Running Keycloak locally with Docker Compose, test users, and exporting realm changes

### Client Configuration

- [Authorization Code Flow — Confidential Client](doc/authorization-code-flow-client.md) — Setting up a server-side web application client that can securely store a `client_secret`
- [Authorization Code Flow — Public Client](doc/authorization-code-flow-public-client.md) — Setting up a browser-based SPA client using PKCE (no client secret)
- [Client Credentials Flow](doc/client-credentials-flow.md) — Setting up machine-to-machine authentication with `client_id` and `client_secret`
- [CORS Configuration](doc/cors-configuration.md) — What CORS is, why it exists, and how to configure allowed origins for a Keycloak client (with screenshots)

### Concepts

- [Authorization Code Flow](doc/authorization-code-flow.md) — How the OAuth2 authorization code flow works
- [Tokens](doc/tokens.md) — Token structure and usage
- [Role Permissions](doc/role-permissions.md) — Role-based access control model
- [Service Point Group ID](doc/service-point-group-id.md) — How service point groups are identified
