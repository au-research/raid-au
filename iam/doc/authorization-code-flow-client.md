# Creating a Keycloak Client for Authorization Code Flow

This guide walks through creating an OpenID Connect client in Keycloak that authenticates users via the OAuth2 Authorization Code flow. This is used for web applications where a user logs in through a browser-based login page.

For details on how to use the authorization code flow once a client is created, see [authorization-code-flow.md](authorization-code-flow.md).

## Prerequisites

- Admin access to the Keycloak administration console
- Access to the `raid` realm
- The redirect URI(s) for your application (the URL Keycloak will redirect to after authentication)

## 1. Navigate to the Clients page

Log in to the Keycloak admin console and select the **raid** realm. Click **Clients** in the left sidebar to view the list of existing clients.

![Clients list](images/cc-clients-list.png)

Click the **Create client** button.

## 2. Configure General Settings

On the **General settings** step:

1. **Client type** should be set to `OpenID Connect` (the default)
2. Enter a **Client ID** — this is the identifier your application will use to authenticate (e.g. `my-web-app`)
3. Optionally add a **Name** and **Description** to document the client's purpose

![General settings](images/ac-general-settings.png)

Click **Next** to continue.

## 3. Configure Capability Config

On the **Capability config** step, you need to configure one setting:

1. **Client authentication** — toggle this to **On**. This makes the client "confidential", which means it will be assigned a `client_secret`. When this is off, the client is "public" and has no secret.

The **Standard flow** checkbox should already be checked by default — this enables the OAuth2 Authorization Code grant type, which is the flow used for browser-based login.

Leave **Service account roles** unchecked — that option is for machine-to-machine authentication (Client Credentials flow) and is not needed here.

![Capability config default](images/ac-capability-config-default.png)

After toggling **Client authentication** to **On**:

![Capability config configured](images/ac-capability-config.png)

Click **Next** to continue.

## 4. Login Settings

The **Login settings** step is where you configure the redirect URIs that Keycloak is allowed to redirect to after authentication. This is critical for the authorization code flow.

1. **Valid redirect URIs** — enter the URI(s) where your application will receive the authorization code callback. For example, `https://example.org/callback`. You can add multiple URIs by clicking **Add valid redirect URIs**. Wildcards are supported (e.g. `https://example.org/*`) but should be avoided in production for security reasons.

2. **Valid post logout redirect URIs** — optionally enter the URI(s) where Keycloak can redirect after the user logs out.

3. **Web origins** — optionally enter the allowed CORS origins for browser-based requests. You can enter `+` to allow all origins that match the valid redirect URIs.

![Login settings](images/ac-login-settings.png)

Click **Save** to create the client.

## 5. Client created

After saving, you will be taken to the client details page. A success banner confirms the client was created.

![Client created](images/ac-client-created.png)

## 6. Retrieve the Client Secret

Click the **Credentials** tab to view the client secret.

![Credentials tab](images/ac-credentials-tab.png)

On this tab you can see:

- **Client Authenticator** is set to `Client Id and Secret`
- **Client Secret** is displayed (masked by default — click the eye icon to reveal it, or the copy icon to copy it to your clipboard)
- The **Regenerate** button allows you to generate a new secret if the current one is compromised

Copy the **Client Secret** value — you will need this along with your **Client ID** to authenticate.

## 7. Use the client

With your `client_id`, `client_secret`, and configured `redirect_uri`, you can now use the authorization code flow as described in [authorization-code-flow.md](authorization-code-flow.md).

The key values you will need:

| Parameter | Value |
|-----------|-------|
| `client_id` | The Client ID you entered (e.g. `my-web-app`) |
| `client_secret` | The secret from the Credentials tab |
| `redirect_uri` | One of the Valid redirect URIs you configured |
| Authorization endpoint | `https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/auth` |
| Token endpoint | `https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/token` |

Replace `iam.prod.raid.org.au` with `iam.demo.raid.org.au` for the DEMO environment.

## Which flow should I use?

When configuring a client for an external party, choose the flow that matches how they will interact with the RAiD API:

**Create an Authorization Code client** (this guide) when the external client:

- Operates a web application where their users log in through a browser
- Needs to identify individual users making requests (e.g. to associate RAiDs with a specific person)
- Requires refresh tokens to maintain long-lived user sessions
- Will redirect users to Keycloak for authentication and receive a callback

**Create a [Client Credentials client](client-credentials-flow.md)** when the external client:

- Runs an automated service or script that calls the RAiD API without user interaction
- Authenticates as the application itself rather than on behalf of individual users
- Needs a simple token request without browser redirects
- Operates a system-to-system integration (e.g. a data ingest pipeline, a reporting service, or a scheduled job)

### Comparison

| | Authorization Code | Client Credentials |
|---|---|---|
| **Use case** | User-facing web applications | Machine-to-machine / API integrations |
| **User login** | Yes — user authenticates via browser | No — client authenticates directly |
| **Standard flow** | Enabled | Not required |
| **Service account roles** | Not required | Enabled |
| **Redirect URIs** | Required | Not required |
| **Refresh tokens** | Yes | No |
| **Token represents** | A specific user | The application itself |
