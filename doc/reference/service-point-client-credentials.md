# Managing service point client credentials

This guide shows a Service Point Admin how to create and manage API client
credentials for their service point using `curl`. A client credential is a
machine account (an OAuth2 client with a secret) that a script or system can use
to call the RAiD API on behalf of your service point, without a person logging
in.

The examples use the **demo** environment. Substitute the host for another
environment as needed.

| Environment | IAM host |
| --- | --- |
| Demo | `iam.demo.raid.org.au` |

## Before you start

You need:

- A user account with the **Service Point Admin** role for your service point.
  In technical terms your account must hold the scoped realm role
  `service-point-admin:<groupId>`, where `<groupId>` is your service point's
  Keycloak group id. Operators can manage any service point.

  > The older flat `group-admin` role is **not** accepted by these endpoints,
  > even if you are a member of the service point. You need the scoped
  > `service-point-admin:<groupId>` role specifically. If your calls return
  > `403` and you believe you administer the service point, this is the most
  > likely cause; contact ARDC to have the scoped role granted.
- Your service point's **group id**. This is the same value as the
  `service_point_group_id` claim in your own access token; the first step below
  shows how to read it.
- `curl` and (for readability) `jq`.

All management calls go to the IAM host, not the RAiD API host. Every request
must be authenticated with your own user access token.

## Step 1: Get an access token

Request a token with your username and password. `client_id` is `raid-api`.

```bash
IAM=https://iam.demo.raid.org.au

TOKEN=$(curl -s -X POST \
  "$IAM/realms/raid/protocol/openid-connect/token" \
  -d grant_type=password \
  -d client_id=raid-api \
  -d username='YOUR_USERNAME' \
  -d password='YOUR_PASSWORD' \
  | jq -r .access_token)
```

If the username or password is wrong you get
`{"error":"invalid_grant","error_description":"Invalid user credentials"}`.

Confirm the token was issued and read your service point group id from it:

```bash
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq .service_point_group_id
```

Use that value as `GROUP_ID` below.

```bash
GROUP_ID='YOUR_SERVICE_POINT_GROUP_ID'
```

## Step 2: Create a credential

Provide a human-readable `label` so you can recognise the credential later. The
response includes the generated `clientId` and `secret`.

> The secret is shown **only** in this response and in the get-secret and rotate
> responses. Store it somewhere safe now. You can retrieve it again later with
> get-secret (Step 5) while the credential is active.

```bash
curl -s -X POST \
  "$IAM/realms/raid/client-credential" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"groupId\": \"$GROUP_ID\", \"label\": \"my-pipeline\"}" \
  | jq .
```

Example response:

```json
{
  "clientId": "raid-cred-a1b2c3d4-...",
  "label": "my-pipeline",
  "secret": "GENERATED_SECRET_VALUE",
  "createdAt": "2026-08-28T03:17:59Z",
  "lastRotatedAt": null
}
```

Notes:

- A service point may hold at most **10 active credentials**. Creating an
  eleventh returns `409 Conflict`. Revoke one you no longer need to free a slot.
- You can only create credentials for your own service point. Passing another
  service point's `groupId` is rejected.

## Step 3: Use the credential

The credential authenticates with the `client_credentials` grant, using the
`clientId` and `secret` from Step 2. This is what your script or system does.

```bash
CLIENT_ID='raid-cred-a1b2c3d4-...'
CLIENT_SECRET='GENERATED_SECRET_VALUE'

APP_TOKEN=$(curl -s -X POST \
  "$IAM/realms/raid/protocol/openid-connect/token" \
  -d grant_type=client_credentials \
  -d client_id="$CLIENT_ID" \
  -d client_secret="$CLIENT_SECRET" \
  | jq -r .access_token)
```

`$APP_TOKEN` is then used as the bearer token on calls to the RAiD API. It is
scoped to your service point.

## Step 4: List your credentials

Returns the active and revoked credentials for your service point, without their
secrets.

```bash
curl -s -G \
  "$IAM/realms/raid/client-credential" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "groupId=$GROUP_ID" \
  | jq .
```

## Step 5: Retrieve a credential's secret

Returns the current secret for a credential you own.

```bash
curl -s -G \
  "$IAM/realms/raid/client-credential/secret" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "clientId=$CLIENT_ID" \
  | jq .
```

## Step 6: Rotate a credential's secret

Issues a new secret and returns it. The **previous secret stops working
immediately**, so update whatever uses the credential straight after rotating.
The `clientId` and `label` do not change.

```bash
curl -s -X POST \
  "$IAM/realms/raid/client-credential/rotate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"clientId\": \"$CLIENT_ID\"}" \
  | jq .
```

## Step 7: Revoke a credential

Disables the credential so it can no longer obtain a token. Revoking is safe to
repeat, and frees a slot against the 10-credential limit.

```bash
curl -s -X DELETE \
  "$IAM/realms/raid/client-credential" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "clientId=$CLIENT_ID" \
  | jq .
```

A revoked credential cannot be rotated. If you need a working credential again,
create a new one.

## Common responses

| Status | Meaning |
| --- | --- |
| `401 Unauthorized` | Your user access token is missing or expired. Repeat Step 1. |
| `403 Forbidden` | Your account is not a Service Point Admin for that service point. |
| `404 Not Found` | The `clientId` does not exist, or is not a credential your service point owns. |
| `409 Conflict` | The service point already has 10 active credentials, or you tried to rotate a revoked one. |

## Notes on secrets

- Responses that contain a secret (create, rotate, get-secret) are sent with
  `Cache-Control: no-store`. Do not cache or log them.
- Secret values are never written to server logs. Credential management actions
  are audit-logged with who, when and which credential only.
