# RSpace Keycloak Client Configuration Comparison

**Date:** 2026-04-10

## Background

Following the RSpace DEMO integration investigation (see `20260409-rspace-demo-integration-investigation.md`), we compared the Keycloak client configurations across environments to identify why the ProjectPID DEMO environment returns 403 FORBIDDEN and missing `refresh_token` for RSpace.

## Environments Compared

| Environment | Keycloak URL | Client ID | Description |
|-------------|-------------|-----------|-------------|
| **RAiD AU STAGE** | `iam.stage.raid.org.au` | `rspace` | Working reference configuration |
| **ProjectPID DEMO** | `iam.demo.projectpid.org` | `rspace` | Secondary troubleshooting client (created 2025-06-15) |
| **ProjectPID DEMO** | `iam.demo.projectpid.org` | `rspace-client` | Original RSpace client (created 2025-06-03) |

## Client Scopes Comparison

This is the most significant difference. Missing scopes directly affect which claims appear in the JWT access token.

| Scope | STAGE `rspace` | ProjectPID `rspace` | ProjectPID `rspace-client` |
|-------|---------------|--------------------|-----------------------------|
| rspace-dedicated | None | MISSING | — |
| rspace-client-dedicated | — | — | None |
| acr | Default | Default | Default |
| basic | Default | Default | Default |
| roles | Default | Default | Default |
| **service_point_group_id** | **Default** | **Default** | **MISSING** |
| **email** | **Default** | **MISSING** | **MISSING** |
| **profile** | **Default** | **MISSING** | **MISSING** |
| **service_account** | **Default** | **MISSING** | **MISSING** |
| **web-origins** | **Default** | **MISSING** | **MISSING** |
| offline_access | Optional | Optional | Optional |
| address | Optional | MISSING | MISSING |
| phone | Optional | MISSING | MISSING |
| microprofile-jwt | Optional | MISSING | MISSING |

### Impact of Missing Scopes

- **`service_point_group_id`** — Custom scope that adds the `service_point_group_id` claim to the JWT. The API uses this claim to authorize requests. **Missing from `rspace-client` — this is the primary cause of the 403 FORBIDDEN error.**
- **`email`** — Adds `email` and `email_verified` claims. Missing from both ProjectPID clients.
- **`profile`** — Adds `name`, `preferred_username`, `given_name`, `family_name` claims. Missing from both ProjectPID clients.
- **`web-origins`** — Populates `allowed-origins` in the token for CORS. Missing from both ProjectPID clients.
- **`service_account`** — Required for service account functionality. Missing from both ProjectPID clients.

## General Settings Comparison

| Setting | STAGE `rspace` | ProjectPID `rspace` | ProjectPID `rspace-client` |
|---------|---------------|--------------------|-----------------------------|
| Enabled | true | true | true |
| Client type | Confidential | Confidential | Confidential |
| Standard Flow | true | true | true |
| Direct Access Grants | **true** | **true** | **false** |
| Implicit Flow | false | false | false |
| Service Accounts | true | true | true |
| Full Scope Allowed | unknown | true | true |
| Front Channel Logout | true | true | true |

## Redirect URIs Comparison

| STAGE `rspace` | ProjectPID `rspace` | ProjectPID `rspace-client` |
|---------------|--------------------|-----------------------------|
| raid-test2.researchspace.com | raid-test2.researchspace.com | raid-test2.researchspace.com |
| pangolin8086.researchspace.com | pangolin8086.researchspace.com | pangolin8086.researchspace.com |
| community.researchspace.com | community.researchspace.com | community.researchspace.com |
| demos.researchspace.com | demos.researchspace.com | demos.researchspace.com |
| howler8085.researchspace.com | howler8085.researchspace.com | howler8085.researchspace.com |
| researchspace2.eu.ngrok.io | researchspace2.eu.ngrok.io | researchspace2.eu.ngrok.io |
| researchspace.eu.ngrok.io | researchspace.eu.ngrok.io | researchspace.eu.ngrok.io |
| researchspace3.eu.ngrok.io | researchspace3.eu.ngrok.io | researchspace3.eu.ngrok.io |
| pangolin8085.researchspace.com | pangolin8085.researchspace.com | pangolin8085.researchspace.com |
| raid-test.researchspace.com | raid-test.researchspace.com | raid-test.researchspace.com |
| — | — | **`/*` (wildcard)** |

All paths end with `/apps/raid/callback`.

**Note:** `rspace-client` has a `/*` wildcard redirect URI — this is a security concern and should be removed.

## Web Origins Comparison

| STAGE `rspace` | ProjectPID `rspace` | ProjectPID `rspace-client` |
|---------------|--------------------|-----------------------------|
| `*` | Explicit list (10 origins matching redirect URIs) | `/*` |

## Protocol Mappers

| Mapper | STAGE `rspace` | ProjectPID `rspace` | ProjectPID `rspace-client` |
|--------|---------------|--------------------|-----------------------------|
| Client Host | No | Yes | Yes |
| Client ID | No | Yes | Yes |
| Client IP Address | No | Yes | Yes |

These mappers add `clientHost`, `client_id`, and `clientAddress` claims to tokens. They are auto-created when service accounts are enabled in Keycloak. They only affect tokens issued via the `client_credentials` flow (service account tokens) and do not impact the `authorization_code` flow.

## Confirmed Client in Use

RSpace is using the `rspace` client on ProjectPID DEMO (not `rspace-client`).

## Reproduction Test on ProjectPID DEMO

Performed a manual authorization_code flow against the ProjectPID DEMO `rspace` client.

A temporary `http://localhost:7080/*` redirect URI was added to the `rspace` client for the duration of testing, then removed afterwards.

### Test 1: User without `service-point-user` role

Authenticated as `0009-0006-4129-5257` (rob.leney@ardc.edu.au) via authorization_code flow. At the time of this test, the user had `default-roles-raid` but did not have the `service-point-user` role.

#### Authorization Request

```
GET https://iam.demo.projectpid.org/realms/raid/protocol/openid-connect/auth
  ?client_id=rspace
  &response_type=code
  &redirect_uri=http://localhost:7080/callback
  &scope=openid
```

#### Token Exchange

**Request:**
```
POST https://iam.demo.projectpid.org/realms/raid/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&client_id=rspace
&client_secret=<secret>
&code=<authorization_code>
&redirect_uri=http://localhost:7080/callback
```

**Response:** `200 OK`
```json
{
  "access_token": "<jwt>",
  "expires_in": 7161,
  "refresh_expires_in": 7161,
  "refresh_token": "<jwt>",
  "token_type": "Bearer",
  "id_token": "<jwt>",
  "not-before-policy": 0,
  "session_state": "<session_id>",
  "scope": "openid service_point_group_id"
}
```

**Key observations:**
- `refresh_token` is present
- `scope` includes `service_point_group_id`

#### Decoded Access Token Claims

```json
{
  "iss": "https://iam.demo.projectpid.org/realms/raid",
  "aud": "account",
  "sub": "092ca9d0-45e4-48e4-af5a-2feb3d5d65b1",
  "typ": "Bearer",
  "azp": "rspace",
  "acr": "1",
  "realm_access": {
    "roles": [
      "offline_access",
      "uma_authorization",
      "default-roles-raid"
    ]
  },
  "scope": "openid service_point_group_id",
  "service_point_group_id": "54ca2aab-5bb0-4a18-b90c-b51e2bc8ae50"
}
```

**Key observations:**
- `service_point_group_id` claim is present with a valid value
- `service-point-user` role is NOT present in `realm_access.roles`

#### API Request

```
GET https://api.demo.projectpid.org/raid/
Authorization: Bearer <access_token>
Accept: application/json
```

**Response:** `403 FORBIDDEN`
```
HTTP/2 403
content-length: 0
www-authenticate: Bearer error="insufficient_scope", error_description="The request requires higher privileges than provided by the access token."
```

### Test 2: Same user with `service-point-user` role added

Added `service-point-user` realm role to the same user and re-authenticated using the same session (same authorize URL, new auth code).

#### Token Exchange

Same request as Test 1 with a new authorization code.

**Response:** `200 OK`

#### Decoded Access Token Claims

```json
{
  "iss": "https://iam.demo.projectpid.org/realms/raid",
  "aud": "account",
  "sub": "092ca9d0-45e4-48e4-af5a-2feb3d5d65b1",
  "typ": "Bearer",
  "azp": "rspace",
  "acr": "0",
  "realm_access": {
    "roles": [
      "offline_access",
      "service-point-user",
      "uma_authorization",
      "default-roles-raid"
    ]
  },
  "scope": "openid service_point_group_id",
  "service_point_group_id": "54ca2aab-5bb0-4a18-b90c-b51e2bc8ae50"
}
```

**Key observations:**
- `service-point-user` role is now present in `realm_access.roles`
- `service_point_group_id` claim remains present

#### API Request

```
GET https://api.demo.projectpid.org/raid/
Authorization: Bearer <access_token>
Accept: application/json
```

**Response:** `200 OK` — RAiD records returned successfully.

### Test Cleanup

- `http://localhost:7080/*` redirect URI removed from the ProjectPID DEMO `rspace` client

## Conclusion

### `rspace` (the client RSpace is using)

The `rspace` client on ProjectPID DEMO has `service_point_group_id` as a Default scope. The authorization_code flow produces tokens with the `service_point_group_id` claim and a `refresh_token`.

In our testing, the 403 was caused by the authenticating user missing the `service-point-user` realm role. Adding the role resolved the 403. We have not yet confirmed whether this is the same cause for Nico's reported issue.

The `rspace` client is missing `email`, `profile`, `web-origins`, and `service_account` Default scopes compared to STAGE. This means the JWT is missing standard identity claims (`name`, `preferred_username`, `email`) but does not cause a 403.

### `rspace-client` (not currently in use)

The `rspace-client` on ProjectPID DEMO is missing 8 client scopes compared to STAGE, including the critical `service_point_group_id` scope. If RSpace were to use this client, tokens would not contain the `service_point_group_id` claim.

### Outstanding Items

1. Confirm with Nico whether the user performing the OAuth flow on ProjectPID DEMO has the `service-point-user` realm role
2. Ask Nico to share a decoded access token from ProjectPID DEMO so we can verify the claims
3. **For `rspace`**: Consider adding missing Default client scopes: `email`, `profile`, `web-origins`, `service_account` to match STAGE
4. **For `rspace-client`**: If this client is to be used in future, add: `service_point_group_id`, `email`, `profile`, `web-origins`, `service_account`
5. **For `rspace-client`**: Remove the `/*` wildcard redirect URI (security concern)
