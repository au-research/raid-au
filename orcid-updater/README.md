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
* An ORCID member API client, with the updater's callback URL registered as
  a redirect URI: `<ORCID_UPDATER_PUBLIC_BASE_URL>/orcid/callback`. ORCID
  rejects sign-in if it is missing. The updater asks for the
  `/read-limited /activities/update` scopes.
* The RA's Keycloak realm, which issues minter and administrator tokens.

Nothing needs AWS.

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
| `ORCID_UPDATER_KEYCLOAK_ISSUER_URI` | yes | e.g. `https://iam.example.org/realms/raid` |
| `ORCID_UPDATER_MAIL_HOST` | yes | |
| `ORCID_UPDATER_MAIL_PORT` | no | Default `587` |
| `ORCID_UPDATER_MAIL_USERNAME` | no | Leave unset for a relay without authentication |
| `ORCID_UPDATER_MAIL_PASSWORD` | no | Secret |
| `ORCID_UPDATER_MAIL_START_TLS` | no | Default `true` |
| `ORCID_UPDATER_MAIL_FROM` | yes | Sender on the RA's own domain |
| `ORCID_UPDATER_EMAIL_HASH_SECRET` | yes | Secret. Keys the email fingerprint kept for "Remember me" |
| `ORCID_UPDATER_TOKEN_ENCRYPTION_KEY` | yes | Secret, at least 32 characters. Encrypts ORCID tokens at rest. Changing it makes stored tokens unreadable |
| `ORCID_UPDATER_LINK_ENCRYPTION_KEY` | yes | Secret, at least 32 characters. Seals the email fingerprint inside invitation links |
| `ORCID_UPDATER_API_KEY` | yes | Secret. The RAiD API sends it as `X-Api-Key` |
| `ORCID_UPDATER_PUBLIC_BASE_URL` | yes | Where researchers reach the updater, e.g. `https://orcid-updater.example.org`. Invitation links start with it |
| `ORCID_UPDATER_CONTACT_EMAIL` | yes | The RA's contact for researchers' privacy and deletion requests, shown on the researcher pages. Not a service point contact |
| `ORCID_UPDATER_INVITATION_EXPIRY` | no | Default `14d` |

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
./gradlew :orcid-updater:intTest   # boots the updater against PostgreSQL in Testcontainers (needs Docker)
./gradlew :api-svc:idl-raid-v2:openApiValidate --input="$PWD/orcid-updater/idl/orcid-updater-openapi-3.1.yaml"
```

## Container image

```bash
./gradlew :orcid-updater:buildDocker -PimageName=orcid-updater:latest
```
