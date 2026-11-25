# How the RAiD app, API, ORCID updater and ORCID communicate

Each registration agency (RA) runs its own [ORCID updater](../../orcid-updater/README.md).
It is the only part of RAiD that holds researchers' ORCID tokens and writes to
ORCID records. The RAiD API never talks to ORCID about researchers' records, and
the RAiD app never holds an ORCID token.

The updater's endpoints are defined in its
[API contract](../../orcid-updater/idl/orcid-updater-openapi-3.1.yaml). The
design decisions are in the
[ADR](../adr/2026-10-23_per-ra-orcid-updater.md).

## Who calls whom

```mermaid
flowchart LR
  APP["RAiD app<br/>(minter and researcher pages)"]
  API["RAiD API"]
  UPD["ORCID updater<br/>(own PostgreSQL)"]
  ORCID["ORCID<br/>(OAuth and member API)"]
  KC["RA's Keycloak"]
  SMTP["RA's mail server"]
  ROR["ROR"]
  APP -- "save RAiD, read contributor status" --> API
  APP -- "invite contributor, forward sign-in" --> UPD
  APP -. "researcher's browser signs in" .-> ORCID
  API -- "POST /raid-changes (API key)" --> UPD
  UPD -- "read RAiDs, set contributor status (service token)" --> API
  UPD -- "exchange code, write memberships" --> ORCID
  UPD -- "service token (private_key_jwt)" --> KC
  UPD -- "invitation email" --> SMTP
  UPD -- "owning organisation" --> ROR
```

Browser calls to the updater are allowed only from the RA's app origin.

## A minter saves a RAiD and invites a contributor

```mermaid
sequenceDiagram
  autonumber
  participant M as Minter's browser (app)
  participant API as RAiD API
  participant U as ORCID updater
  participant Mail as Mail server
  M->>API: POST or PUT /raid (minter's token)
  API-->>M: saved
  Note over API,U: after the transaction commits, in the background
  API-)U: POST /raid-changes {handle} (X-Api-Key)
  M->>U: POST /invitations {handle, ORCID iD, email} (minter's token)
  U->>API: GET /raid/{handle}/permissions (minter's token)
  U->>API: GET /raid/{handle} (service token)
  U->>Mail: invitation email with a single-use link
  U->>API: PUT /contributor/status AWAITING_AUTHENTICATION
  U-->>M: AWAITING_AUTHENTICATION
```

- The API tells the updater about every mint, update and patch. A failed call
  is logged and the save still succeeds; the next save tells the updater again.
- The email is used only to send the invitation and is never sent to the API.
  The updater stores only a keyed hash of it, for "Remember me".
- No new email is sent while an invitation for the same iD is still valid, or
  when a researcher who ticked "Remember me" matches the email.

## The researcher authenticates their ORCID iD

```mermaid
sequenceDiagram
  autonumber
  participant R as Researcher's browser (app)
  participant U as ORCID updater
  participant O as ORCID
  participant API as RAiD API
  R->>U: GET /invitations/{token}
  U-->>R: RAiD title, service point, status
  R->>U: POST /invitations/{token}/authentications {rememberMe}
  U-->>R: ORCID authorise URL (state, 30 minutes, single use)
  R->>O: sign in, grant /read-limited /activities/update
  O-->>R: redirect to app /orcid/callback?code&state
  R->>U: POST /authentications {code, state}
  U->>O: POST /oauth/token (exchange code)
  O-->>U: access and refresh tokens, authenticated iD
  Note over U: token stored encrypted, ORCID's iD replaces the typed one
  U->>API: PUT /contributor/status AUTHENTICATED
  U->>API: GET /raid/?contributor.id={iD}
  loop each RAiD that is not embargoed
    U->>O: POST membership (organisation from ROR)
  end
  U-->>R: AUTHENTICATED
```

- `PUT /contributor/status` gives every RAiD naming the contributor a new
  version holding the status. DataCite isn't called: every contributor is a
  creator on the DOI whatever their status, so the DOI doesn't change.
  Repeating the call changes nothing, so the updater can retry after a
  failure.
- Mint, update and patch ignore any status the client sends and use the
  stored one, UNAUTHENTICATED for a contributor the API hasn't seen.
- ORCID sends the researcher back to the app, so each app's
  `<app URL>/orcid/callback` must be registered as a redirect URI on the RA's
  ORCID member client.
- If the researcher declines (`error=access_denied`), the updater stores
  nothing.

## A later save keeps ORCID records up to date

```mermaid
sequenceDiagram
  autonumber
  participant M as Minter's browser (app)
  participant API as RAiD API
  participant U as ORCID updater
  participant O as ORCID
  M->>API: PUT or PATCH /raid/{handle}
  API-)U: POST /raid-changes {handle} (queued, repeats merged)
  U->>API: GET /raid/{handle} (service token)
  alt RAiD is embargoed
    Note over U: held until the API re-saves it as open
  else RAiD is open
    U->>O: create or update a membership per authenticated contributor
    U->>O: delete entries of removed contributors (stored put codes)
  end
  M->>API: GET /raid/{handle}
  API-->>M: contributors with their stored status
```

## What each call carries

| Caller → callee | Credential |
|---|---|
| API → updater | Shared API key in `X-Api-Key` |
| Minter's browser → updater | Minter's Keycloak access token. The updater checks the minter can edit the RAiD. |
| Researcher's browser → updater | None. Protected by the single-use invitation token and the OAuth state. |
| Updater → API | Service token for the updater's Keycloak client (`contributor-writer` role), got with `private_key_jwt`. Keycloak reads the public key from `/.well-known/jwks.json`, so the client has no secret. |
| Updater → ORCID | The RA's ORCID member client for the code exchange, then the researcher's stored token for writes. |
| RA administrators → updater | Token with the `orcid-updater-admin` role, for deleting a researcher's data and importing put codes. |
