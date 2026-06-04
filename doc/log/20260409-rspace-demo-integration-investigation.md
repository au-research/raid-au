# RSpace DEMO Integration Investigation

**Date:** 2026-04-09

## Background

Nico Ferrante (Research Space) reported two issues with the RSpace integration on the RAiD AU DEMO environment:

1. No `refresh_token` returned during token exchange
2. `access_token` returns 403 FORBIDDEN from the API

The STAGE environment works fine for RSpace.

## Environment Details

- **DEMO Keycloak:** `iam.demo.raid.org.au`
- **DEMO API:** `api.demo.raid.org.au`
- **Client:** `rspace` (confidential client)

## Investigation Steps

### 1. Compared Keycloak Client Configs (DEMO vs STAGE)

Compared the `rspace` client configuration between DEMO and STAGE Keycloak instances. Configs were found to be identical.

### 2. Attempted Local Reproduction via keycloak-js

Pointed the local RAiD UI (`raid-agency-app`) at the DEMO `rspace` client by modifying `.env`:

```
VITE_KEYCLOAK_URL=https://iam.demo.raid.org.au
VITE_KEYCLOAK_CLIENT_ID=rspace
```

Added `http://localhost:7080/*` as a valid redirect URI on the DEMO `rspace` client.

**Result:** keycloak-js failed with "Server responded with an invalid status" during `fetchAccessToken`. This is expected — keycloak-js is designed for public clients and does not send `client_secret`, which is required for the `rspace` confidential client.

### 3. Manual OAuth Authorization Code Flow (curl)

Switched to a manual approach:

1. Navigated browser to the authorize endpoint to get an auth code
2. Exchanged the code via curl with `client_secret`

```
POST https://iam.demo.raid.org.au/realms/raid/protocol/openid-connect/token
  grant_type=authorization_code
  client_id=rspace
  client_secret=<secret>
  code=<auth_code>
  redirect_uri=http://localhost:7080/callback
```

**Result:** Tokens returned successfully, API returned 200. Both `access_token` and `refresh_token` were present.

### 4. Matched Rob's User Config to Nico's

Modified Rob's DEMO user to match Nico's configuration:

- Removed `operator` role (left `group-admin`, `service-point-user`, `default-roles-raid`)
- Changed `activeGroupId` to the Research Space group

**Result:** Flow still worked — tokens returned, API returned 200. Issue not reproduced.

### 5. Tested Without `offline_access` Scope

Requested only `scope=openid` (no `offline_access`).

**Result:** `refresh_token` was still returned, but with a finite expiry (`refresh_expires_in: 603182` ~7 days) instead of infinite (`refresh_expires_in: 0`). This rules out scope as the cause of a missing refresh_token.

### 6. Tested Wrong/Missing Client Secret

```
# Wrong secret
client_secret=WRONG_SECRET_HERE  →  "error": "unauthorized_client"

# No secret
(omitted)                         →  "error": "unauthorized_client"
```

**Result:** Keycloak rejects the request entirely — no tokens at all. This means if Nico is getting tokens back, the client_secret is correct.

## Conclusion

We were unable to reproduce either issue on RAiD AU DEMO. The authorization code flow with the `rspace` confidential client works correctly — tokens (including refresh_token) are returned and the API accepts them.

To progress further, we need Nico to share:

- The exact HTTP requests RSpace sends (authorize URL with scopes, and token exchange request with secret redacted)
- Which Keycloak user the authorization_code flow logs in as
- Confirmation of whether RSpace is using authorization_code or client_credentials flow on DEMO

## Appendix: Full Request/Response Examples

The following captures show the complete OAuth authorization code flow against DEMO, demonstrating that it works correctly.

### Step 1: Authorization Request

Navigate to the Keycloak authorize endpoint in a browser:

```
GET https://iam.demo.raid.org.au/realms/raid/protocol/openid-connect/auth
  ?client_id=rspace
  &response_type=code
  &redirect_uri=http://localhost:7080/callback
  &scope=openid
```

The user authenticates via the Keycloak login page. On success, Keycloak redirects to the `redirect_uri` with an authorization code:

```
HTTP/1.1 302 Found
Location: http://localhost:7080/callback
  ?session_state=<session_id>
  &iss=https://iam.demo.raid.org.au/realms/raid
  &code=<authorization_code>
```

### Step 2: Token Exchange

Exchange the authorization code for tokens using the token endpoint with `client_secret`:

**Request:**
```
POST https://iam.demo.raid.org.au/realms/raid/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&client_id=rspace
&client_secret=<secret>
&code=<authorization_code>
&redirect_uri=http://localhost:7080/callback
```

**Response Headers:**
```
HTTP/2 200
content-type: application/json
cache-control: no-store
pragma: no-cache
referrer-policy: no-referrer
strict-transport-security: max-age=31536000; includeSubDomains
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
```

**Response Body:**
```json
{
  "access_token": "<jwt>",
  "expires_in": 86400,
  "refresh_expires_in": 604746,
  "refresh_token": "<jwt>",
  "token_type": "Bearer",
  "id_token": "<jwt>",
  "not-before-policy": 0,
  "session_state": "<session_id>",
  "scope": "openid email service_point_group_id profile"
}
```

**Key observations:**
- `refresh_token` is present (contradicts Nico's report)
- `refresh_expires_in: 604746` (~7 days) — finite expiry because `offline_access` was not requested
- `scope` includes `service_point_group_id` — the custom scope mapper is working

### Step 3: Decoded Access Token Claims

```json
{
  "exp": 1775799181,
  "iat": 1775712781,
  "iss": "https://iam.demo.raid.org.au/realms/raid",
  "aud": "account",
  "sub": "<user_uuid>",
  "typ": "Bearer",
  "azp": "rspace",
  "realm_access": {
    "roles": [
      "group-admin",
      "offline_access",
      "service-point-user",
      "uma_authorization",
      "default-roles-raid",
      "operator"
    ]
  },
  "scope": "openid email service_point_group_id profile",
  "name": "Rob Leney",
  "preferred_username": "rob.leney@ardc.edu.au",
  "service_point_group_id": "<group_uuid>",
  "email": "rob.leney@ardc.edu.au"
}
```

**Key observations:**
- `service_point_group_id` claim is present — required by the API for authorization
- `realm_access.roles` includes `operator` and `service-point-user`
- Token is issued by `azp: rspace` (the confidential client)
- No `clientHost` or `clientAddress` — confirms this is an authorization_code flow, not client_credentials

### Step 4: API Request

Call the RAiD API with the access token:

**Request:**
```
GET https://api.demo.raid.org.au/raid/
Authorization: Bearer <access_token>
Accept: application/json
```

**Response Headers:**
```
HTTP/2 200
content-type: application/json
vary: Origin, Access-Control-Request-Method, Access-Control-Request-Headers
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
x-frame-options: DENY
```

**Response Body:** 200 OK — returns list of RAiD records as expected.

**Key observation:** The API accepts the token from the `rspace` client without any 403 error.

## Cleanup

All changes made during investigation have been reverted:

- Rob's `operator` role restored on DEMO Keycloak
- Rob's `activeGroupId` restored to original value
- `.env` reverted to local dev defaults
- `http://localhost:7080/*` redirect URI removed from the DEMO `rspace` client
