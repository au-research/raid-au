# Creating a Public Client for Authorization Code Flow with PKCE

This guide walks through creating a **public** OpenID Connect client in Keycloak for the OAuth2 Authorization Code flow with PKCE (Proof Key for Code Exchange). This is used when the external client operates a browser-based single-page application (SPA) that cannot securely store a client secret.

For server-side web applications that can securely store a secret, see [authorization-code-flow-client.md](authorization-code-flow-client.md) instead.

For details on how to use the authorization code flow once a client is created, see [authorization-code-flow.md](authorization-code-flow.md).

## Prerequisites

- Admin access to the Keycloak administration console
- Access to the `raid` realm
- The redirect URI(s) and web origin(s) for the external client's application

## 1. Navigate to the Clients page

Log in to the Keycloak admin console and select the **raid** realm. Click **Clients** in the left sidebar to view the list of existing clients.

![Clients list](images/cc-clients-list.png)

Click the **Create client** button.

## 2. Configure General Settings

On the **General settings** step:

1. **Client type** should be set to `OpenID Connect` (the default)
2. Enter a **Client ID** — this is the identifier the external client will use in their application (e.g. `my-spa-app`)
3. Optionally add a **Name** and **Description** to document the client's purpose

![General settings](images/pc-general-settings.png)

Click **Next** to continue.

## 3. Configure Capability Config

On the **Capability config** step:

1. **Client authentication** — leave this **Off**. This makes the client "public", meaning it has no `client_secret`. This is the correct setting for browser-based applications where a secret cannot be kept confidential.

The **Standard flow** checkbox should already be checked by default — this enables the OAuth2 Authorization Code grant type.

Leave all other options at their defaults.

The external client's application will use **PKCE** (Proof Key for Code Exchange) to secure the authorization code exchange instead of a client secret. Most modern OAuth2/OIDC client libraries (e.g. `keycloak-js`) handle PKCE automatically.

![Capability config](images/pc-capability-config.png)

Click **Next** to continue.

## 4. Login Settings

The **Login settings** step is where you configure the redirect URIs and web origins.

1. **Valid redirect URIs** — enter the URI(s) provided by the external client. For SPAs, this is typically the application's base URL with a wildcard path (e.g. `https://example.org/*`). Wildcards should be scoped as tightly as possible.

2. **Valid post logout redirect URIs** — optionally enter the URI(s) where Keycloak can redirect after the user logs out.

3. **Web origins** — enter the origin(s) of the external client's application (e.g. `https://example.org`). This configures CORS to allow the browser to make token requests directly to Keycloak. You can enter `+` to allow all origins that match the valid redirect URIs.

![Login settings](images/pc-login-settings.png)

Click **Save** to create the client.

## 5. Client created

After saving, you will be taken to the client details page. A success banner confirms the client was created.

Note that public clients do not have a **Credentials** or **Keys** tab, since there is no client secret.

![Client created](images/pc-client-created.png)

## 6. Provide configuration to the external client

The external client will need the following values to configure their application:

| Parameter | Value |
|-----------|-------|
| `client_id` | The Client ID you entered (e.g. `my-spa-app`) |
| `redirect_uri` | One of the Valid redirect URIs you configured |
| Authorization endpoint | `https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/auth` |
| Token endpoint | `https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/token` |

Replace `iam.prod.raid.org.au` with `iam.demo.raid.org.au` for the DEMO environment.

Note that there is no `client_secret` — the external client's application will use PKCE to secure the token exchange.

## Which client type should I create?

When configuring a client for an external party, choose the type that matches their application architecture:

**Create a public client** (this guide) when the external client:

- Operates a browser-based single-page application (SPA) that cannot securely store a secret
- Uses a JavaScript framework (e.g. React, Angular, Vue) that runs entirely in the browser
- Will use PKCE (Proof Key for Code Exchange) instead of a client secret

**Create a [confidential client](authorization-code-flow-client.md)** when the external client:

- Operates a server-side web application (e.g. Spring Boot, Django, Rails) that can securely store a `client_secret` on its backend
- Exchanges the authorization code for tokens on the server side, not in the browser

**Create a [Client Credentials client](client-credentials-flow.md)** when the external client:

- Runs an automated service or script that calls the RAiD API without user interaction
- Authenticates as the application itself rather than on behalf of individual users

### Comparison

| | Confidential (Auth Code) | Public (Auth Code + PKCE) | Client Credentials |
|---|---|---|---|
| **Use case** | Server-side web applications | Browser-based SPAs | Machine-to-machine |
| **Client authentication** | On (has `client_secret`) | Off (no secret) | On (has `client_secret`) |
| **Security mechanism** | Client secret | PKCE | Client secret |
| **User login** | Yes | Yes | No |
| **Redirect URIs** | Required | Required | Not required |
| **Token represents** | A specific user | A specific user | The application itself |
