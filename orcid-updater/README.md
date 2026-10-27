# ORCID updater

A service each registration agency runs to authenticate researchers' ORCID
iDs and write RAiDs to their ORCID records with their consent (RAID-975).

* API contract: [idl/orcid-updater-openapi-3.1.yaml](idl/orcid-updater-openapi-3.1.yaml)
* Design decisions: [ADR 2026-10-23](../doc/adr/2026-10-23_per-ra-orcid-updater.md)

## What an RA provides

* A PostgreSQL database and login for the updater only. Never give the RAiD
  API's login access to it: it holds researchers' ORCID tokens. Migrations
  run automatically on startup into the `orcid_updater` schema.
* An SMTP server, with a sender address on the RA's own domain.
* An ORCID member API client, with the RAiD app's callback page registered
  as a redirect URI: `<ORCID_UPDATER_APP_BASE_URL>/orcid/callback`. ORCID
  rejects sign-in if it is missing. The updater asks for the
  `/read-limited /activities/update` scopes.
* The RAiD app (RAID-977), which shows researchers their pages and calls the
  updater. The updater serves no pages of its own.
* The RA's Keycloak realm, which issues minter and administrator tokens,
  and the updater's own token. The updater signs in to Keycloak as the
  client `orcid-updater`, which needs the RAiD API's `contributor-writer`
  role. It authenticates with a key it creates itself, not a secret:
  Keycloak reads the public half from
  `<updater URL>/.well-known/jwks.json`, so Keycloak must be able to reach
  the updater. Set that URL as `RAID_ORCID_UPDATER_JWKS_URL` in Keycloak's
  environment and Keycloak creates the client, with the role, on its next
  start (RAID-1011). See
  [Keycloak configuration](../iam/doc/keycloak-configuration.md#orcid-updater-confidential-signed-jwt-created-at-boot).
* Outbound HTTPS to the ORCID API and to the ROR API (`api.ror.org`).

Nothing needs AWS.

## Background work

The RAiD API tells the updater about every saved RAiD (`POST /raid-changes`),
and the updater answers straight away. The work itself is queued in the
`work_item` table and done in the background, by every running instance:

* RAiD changed: read the RAiD from the RAiD API and bring its ORCID entries
  in step. An embargoed RAiD is held back; it is written when the embargo
  ends and the RAiD is saved as open.
* A researcher's status changed (invitation sent or expired, signed in, token
  rejected by ORCID): set it in the RAiD API with `PUT /contributor/status`.
  On sign-in, every RAiD that names the researcher is written to ORCID.

When the RAiD API or Keycloak can't be reached, the item is tried again,
waiting longer each time. After the last attempt it stays in `work_item`
with `failed_at` and `failure` set, for operators, until the same RAiD or
researcher is queued again.

## What it writes to ORCID

Each authenticated contributor gets one membership entry per RAiD, in
ORCID's JSON format. ORCID's id for it (the put code) is kept in
`orcid_entry`, so a later save of the RAiD updates the same entry, and the
entry is deleted when the contributor is removed from the RAiD. A failed
write is recorded on that row (`failure`) for operators; the next save of
the RAiD tries again. If ORCID rejects a researcher's token, the researcher
is marked revoked and nothing more is written until they authenticate again.
Every entry names the organisation that owns the RAiD, because ORCID
requires one; see the ADR.

## Settings

Set these as environment variables. The updater will not start if any
required setting is missing, and the startup error names each one.

| Environment variable | Required | Notes |
|---|---|---|
| `ORCID_UPDATER_DB_URL` | yes | JDBC URL, e.g. `jdbc:postgresql://db:5432/orcid_updater` |
| `ORCID_UPDATER_DB_USERNAME` | yes | |
| `ORCID_UPDATER_DB_PASSWORD` | yes | Secret |
| `ORCID_UPDATER_ORCID_CLIENT_ID` | yes | |
| `ORCID_UPDATER_ORCID_CLIENT_SECRET` | yes | Secret |
| `ORCID_UPDATER_ORCID_CLIENT_NAME` | yes | Shown to researchers, e.g. `ARDC RAiD Service` |
| `ORCID_UPDATER_ORCID_BASE_URL` | yes | `https://orcid.org`, or `https://sandbox.orcid.org` outside production |
| `ORCID_UPDATER_ORCID_API_URL` | yes | `https://api.orcid.org/v3.0`, or `https://api.sandbox.orcid.org/v3.0` |
| `ORCID_UPDATER_RAID_API_BASE_URL` | yes | The RA's RAiD API |
| `ORCID_UPDATER_RAID_API_CLIENT_ID` | no | Default `orcid-updater`. The updater's own Keycloak client |
| `ORCID_UPDATER_KEYCLOAK_ISSUER_URI` | yes | e.g. `https://iam.example.org/realms/raid` |
| `ORCID_UPDATER_MAIL_HOST` | yes | |
| `ORCID_UPDATER_MAIL_PORT` | no | Default `587` |
| `ORCID_UPDATER_MAIL_USERNAME` | no | Leave unset for a relay without authentication |
| `ORCID_UPDATER_MAIL_PASSWORD` | no | Secret |
| `ORCID_UPDATER_MAIL_START_TLS` | no | Default `true` |
| `ORCID_UPDATER_MAIL_FROM` | yes | Sender on the RA's own domain |
| `ORCID_UPDATER_EMAIL_HASH_SECRET` | yes | Secret. Keys the email fingerprint kept for "Remember me" |
| `ORCID_UPDATER_TOKEN_ENCRYPTION_KEY` | yes | Secret, at least 32 characters. Encrypts ORCID tokens and the updater's signing key at rest. Changing it makes stored tokens unreadable, and the updater creates a new signing key |
| `ORCID_UPDATER_LINK_ENCRYPTION_KEY` | yes | Secret, at least 32 characters. Seals the email fingerprint inside invitation links |
| `ORCID_UPDATER_API_KEY` | yes | Secret. The RAiD API sends it as `X-Api-Key` |
| `ORCID_UPDATER_APP_BASE_URL` | yes | The RA's RAiD app, e.g. `https://app.example.org`. Invitation links and the ORCID redirect URI point into it, and only its origin may call the updater from a browser (CORS) |
| `ORCID_UPDATER_CONTACT_EMAIL` | yes | The RA's contact for researchers' privacy and deletion requests, returned to the app for its researcher pages. Not a service point contact |
| `ORCID_UPDATER_INVITATION_EXPIRY` | no | Default `14d` |
| `ORCID_UPDATER_ORCID_WRITE_MAX_ATTEMPTS` | no | Default `3`. Attempts per ORCID write when ORCID returns 5xx or 429, or doesn't respond |
| `ORCID_UPDATER_ORCID_WRITE_INITIAL_BACKOFF` | no | Default `2s`. Wait before the second attempt, doubled before each later one |
| `ORCID_UPDATER_ROR_API_URL` | no | Default `https://api.ror.org/v2`. Looks up the RAiD owner's organisation, which ORCID requires on every entry |
| `ORCID_UPDATER_WORK_POLL_INTERVAL` | no | Default `5s`. How often each instance looks for queued work |
| `ORCID_UPDATER_WORK_BATCH_SIZE` | no | Default `20`. Items taken per look |
| `ORCID_UPDATER_WORK_MAX_ATTEMPTS` | no | Default `5`. Attempts per item while the RAiD API or Keycloak can't be reached |
| `ORCID_UPDATER_WORK_INITIAL_BACKOFF` | no | Default `30s`. Wait before the second attempt, doubled before each later one |
| `ORCID_UPDATER_WORK_CLAIM_TIMEOUT` | no | Default `5m`. How long an instance may hold an item before another may take it over |

The updater listens on port 8090. `GET /actuator/health` is the health check;
inside the container run `java -cp /opt Status http://localhost:8090/actuator/health`.

## Local development

From the repository root, with the RAiD API's stack running for Keycloak
(`./gradlew :api-svc:raid-api:dockerComposeUp`):

```bash
./gradlew :orcid-updater:dockerComposeUp      # PostgreSQL on 7433, Mailpit on 1025 (web UI on 8025)
./gradlew :orcid-updater:bootRun -Dspring.profiles.active=dev
```

## Tests

```bash
./gradlew :orcid-updater:test      # unit tests, run by CI
./gradlew :orcid-updater:intTest   # boots the updater against PostgreSQL and Keycloak in Testcontainers (needs Docker)
./gradlew :api-svc:idl-raid-v2:openApiValidate --input="$PWD/orcid-updater/idl/orcid-updater-openapi-3.1.yaml"
```

## Container image

```bash
./gradlew :orcid-updater:buildDocker -PimageName=orcid-updater:latest
```
