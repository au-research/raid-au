# Creating a Keycloak Client with Client Credentials

This guide walks through creating an OpenID Connect client in Keycloak that authenticates using a `client_id` and `client_secret` (the OAuth2 Client Credentials flow). This is used for machine-to-machine communication where no user interaction is required.

## Prerequisites

- Admin access to the Keycloak administration console
- Access to the `raid` realm

## 1. Navigate to the Clients page

Log in to the Keycloak admin console and select the **raid** realm. Click **Clients** in the left sidebar to view the list of existing clients.

![Clients list](images/cc-clients-list.png)

Click the **Create client** button.

## 2. Configure General Settings

On the **General settings** step:

1. **Client type** should be set to `OpenID Connect` (the default)
2. Enter a **Client ID** — this is the identifier your application will use to authenticate (e.g. `my-api-client`)
3. Optionally add a **Description** to document the client's purpose

![General settings](images/cc-general-settings.png)

Click **Next** to continue.

## 3. Configure Capability Config

This is the most important step for setting up client credentials authentication.

![Capability config default](images/cc-capability-config-default.png)

You need to change two settings:

1. **Client authentication** — toggle this to **On**. This makes the client "confidential", which means it will be assigned a `client_secret`. When this is off, the client is "public" and has no secret.

2. **Service account roles** — check this box. This enables the OAuth2 Client Credentials grant type, allowing the client to authenticate directly with its `client_id` and `client_secret` without requiring a user to log in.

![Capability config configured](images/cc-capability-config.png)

Click **Next** to continue.

## 4. Login Settings

The **Login settings** step allows you to configure redirect URIs and web origins. For a pure client credentials client (machine-to-machine, no browser-based login), these fields can be left empty.

![Login settings](images/cc-login-settings.png)

Click **Save** to create the client.

## 5. Client created

After saving, you will be taken to the client details page. A success banner confirms the client was created.

![Client created](images/cc-client-created.png)

## 6. Retrieve the Client Secret

Click the **Credentials** tab to view the client secret.

![Credentials tab](images/cc-credentials-tab.png)

On this tab you can see:

- **Client Authenticator** is set to `Client Id and Secret`
- **Client Secret** is displayed (masked by default — click the eye icon to reveal it, or the copy icon to copy it to your clipboard)
- The **Regenerate** button allows you to generate a new secret if the current one is compromised

Copy the **Client Secret** value — you will need this along with your **Client ID** to authenticate.

## 7. Request an access token

With your `client_id` and `client_secret`, you can request an access token using the Client Credentials grant:

```
POST /realms/raid/protocol/openid-connect/token
Host: iam.prod.raid.org.au
Content-Type: application/x-www-form-urlencoded

client_id=my-api-client
    &client_secret=YOUR_CLIENT_SECRET
    &grant_type=client_credentials
```

Example using `curl`:

```bash
curl -X POST "https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=my-api-client" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=client_credentials"
```

Replace `iam.prod.raid.org.au` with `iam.demo.raid.org.au` for the DEMO environment.

## 8. Use the access token

The response will include an access token:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "token_type": "Bearer",
  "not-before-policy": 0,
  "scope": "..."
}
```

Include this token in the `Authorization` header when calling the RAiD API:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

Note that client credentials tokens do not include a `refresh_token`. When the access token expires, request a new one using the same client credentials request.

## Which flow should I use?

When configuring a client for an external party, choose the flow that matches how they will interact with the RAiD API:

**Create a Client Credentials client** (this guide) when the external client:

- Runs an automated service or script that calls the RAiD API without user interaction
- Authenticates as the application itself rather than on behalf of individual users
- Needs a simple token request without browser redirects
- Operates a system-to-system integration (e.g. a data ingest pipeline, a reporting service, or a scheduled job)

**Create an [Authorization Code client](authorization-code-flow-client.md)** when the external client:

- Operates a web application where their users log in through a browser
- Needs to identify individual users making requests (e.g. to associate RAiDs with a specific person)
- Requires refresh tokens to maintain long-lived user sessions
- Will redirect users to Keycloak for authentication and receive a callback

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
