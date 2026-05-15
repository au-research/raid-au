# Token and Session Lifetimes

Token and session lifetimes configured in the `raid` Keycloak realm across all environments. Captured 2026-05-15.

## Access Token Lifespan

The access token lifespan determines how long an access token is valid after it is issued.

| Environment | Access Token Lifespan |
|---|---|
| Local Dev | 24 Hours |
| Demo AU | 1 Day |
| Test AU | 1 Day |
| Stage AU | 1 Day |
| **Prod AU** | **10 Minutes** |

Production uses a significantly shorter access token lifespan. Clients must use refresh tokens to obtain new access tokens without requiring the user to re-authenticate. See [tokens.md](tokens.md) for curl examples.

## Session Lifetimes (= Refresh Token Lifetimes)

Keycloak does not have a separate refresh token lifetime setting. Refresh tokens are tied to the SSO session — a refresh token can be used to obtain new access tokens as long as the SSO session is still valid. When the session expires, the refresh token becomes invalid and the user must re-authenticate.

- **SSO Session Idle**: how long a session can be inactive before it expires. Each time a refresh token is used, the idle timer resets.
- **SSO Session Max**: the absolute maximum duration of a session regardless of activity. Once reached, the user must re-authenticate even if they have been continuously active.

| Setting | Local Dev | Demo AU | Test AU | Stage AU | Prod AU |
|---|---|---|---|---|---|
| SSO Session Idle | 24 Hours | 7 Days | 7 Days | 7 Days | 7 Days |
| SSO Session Max | 2 Hours | 7 Days | 7 Days | 7 Days | 7 Days |
| Offline Session Idle | 30 Days | 30 Days | 30 Days | 30 Days | 30 Days |
| Offline Session Max Limited | N/A | Disabled | Disabled | Disabled | Disabled |

Local dev has a much shorter SSO Session Max (2 Hours) compared to deployed environments (7 Days).

## Refresh Token Settings

| Setting | Local Dev | Demo AU | Test AU | Stage AU | Prod AU |
|---|---|---|---|---|---|
| Revoke Refresh Token | N/A | Disabled | Disabled | Disabled | Disabled |

With "Revoke Refresh Token" disabled, refresh tokens are reusable — the same refresh token can be exchanged for new access tokens multiple times within the SSO session lifetime.

## Action Token Lifetimes

Action tokens are used for operations like password reset and email verification.

| Setting | Local Dev | Demo AU | Test AU | Stage AU | Prod AU |
|---|---|---|---|---|---|
| User-Initiated Action Lifespan | N/A | 1 Day | 5 Minutes | 1 Day | 5 Minutes |
| Admin-Initiated Action Lifespan | N/A | 12 Hours | 12 Hours | 12 Hours | 12 Hours |

Test and Prod use a shorter User-Initiated Action Lifespan (5 Minutes) compared to Demo and Stage (1 Day).

## Other Token Settings

| Setting | Local Dev | Demo AU | Test AU | Stage AU | Prod AU |
|---|---|---|---|---|---|
| Access Token Lifespan (Implicit Flow) | N/A | 15 Minutes | 15 Minutes | 15 Minutes | 15 Minutes |
| Client Login Timeout | N/A | 1 Minute | 1 Minute | 1 Minute | 1 Minute |

## Practical Impact

In **production**, with a 10-minute access token and 7-day SSO session:
- Access tokens expire after 10 minutes — clients must use the refresh token to get a new one
- Refresh tokens remain valid for up to 7 days (SSO Session Max), with the idle timer resetting on each use
- If a refresh token is not used for 7 days (SSO Session Idle), the session expires and the user must re-authenticate
- Offline tokens (if requested) remain valid for 30 days of inactivity

In **non-production environments**, the 1-day access token means refresh token handling is less critical during development and testing — a single token lasts a full working day.

## Keycloak Instances

| Environment | URL |
|---|---|
| Local Dev | `http://localhost:8001` |
| Demo AU | `https://iam.demo.raid.org.au` |
| Test AU | `https://iam.test.raid.org.au` |
| Stage AU | `https://iam.stage.raid.org.au` |
| Prod AU | `https://iam.prod.raid.org.au` |
| Demo US | `https://iam.demo.projectpid.org` |
