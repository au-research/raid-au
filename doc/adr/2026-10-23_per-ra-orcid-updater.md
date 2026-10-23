### Each registration agency runs its own ORCID updater, built as a Java module in raid-au

* Status: proposed
* Who: proposed by RL
* When: 2026-10-23
* Related: RAID-974 (epic), RAID-975 (the updater), RAID-976 (RAiD API
  integration), RAID-977 (app), RAID-978 (cutover), RAID-1000 (this ADR and
  the API contract), RAID-887 (central updater has no caller authentication),
  RAID-904 (retries with no stop condition)


# Context

Today a single central ORCID updater, run by ARDC on AWS (Lambda and
DynamoDB, in raid-aws-private), writes RAiDs to researchers' ORCID records
for every registration agency (RA). It stores researchers' access tokens
unencrypted, writes them to its logs, does not authenticate its callers
(RAID-887), and keeps its own record of who has authenticated, separate from
the RAiD API. RAID-974 replaces it with an updater that each RA runs itself,
so that an RA writes to ORCID records only with its own client and only with
the researcher's consent.

The decisions below were recorded on RAID-974 and RAID-975 on 2026-10-20 and
2026-10-23. This ADR collects them in one place, with the API contract in
[orcid-updater-openapi-3.1.yaml](../../orcid-updater/idl/orcid-updater-openapi-3.1.yaml).


# Decision

## No dependency on AWS

The updater, its database and its email sending must not need AWS. Many RAs
run on premises. Each RA provides a PostgreSQL database and an SMTP server;
ARDC ships the updater as a container image whose Flyway migrations run on
startup, as the RAiD API's already do. RAiD AU may host its copy on AWS.

## A Java/Spring Boot module in raid-au

The updater is a new Gradle module in this repository, alongside `api-svc`
and `iam`. It shares the toolchain, CI and container build, and RAs that run
the RAiD API already run this stack. Reusing the central updater's
TypeScript was rejected: it is built around Lambda and DynamoDB, and its
token handling is what this work replaces.

## Its own database and login

The updater's PostgreSQL database and login are separate from the RAiD
API's. The API's login cannot read researcher tokens, the updater's login
cannot read RAiD data, and each can be upgraded or rolled back on its own.

## Status is per person, and the updater drives it

A contributor's status belongs to the person (ORCID iD), not to each RAiD.
The RAiD API already stores it that way, on the shared `contributor` row.
When the updater changes a status it calls a new RAiD API endpoint,
`PUT /contributor/status`, once per person. The existing
`PATCH /raid/{prefix}/{suffix}` was rejected for this: it replaces the whole
contributor list, creates a RAiD version, updates DataCite, notifies the
updater again (a loop), and overwrites the status it is sent.

Statuses keep the RAiD API's existing values so Member API integrators see
no breaking change: AUTHENTICATED, AWAITING_AUTHENTICATION,
AUTHENTICATION_REVOKED and UNAUTHENTICATED. AUTHENTICATION_FAILED is no
longer used.

## The updater is the only ORCID writer

After every mint, update and patch, the RAiD API sends the updater the RAiD
handle. The updater reads the RAiD from the API with its own Keycloak client
(holding the existing `contributor-writer` role), writes entries for
authenticated contributors, holds back embargoed RAiDs until the embargo
ends, and deletes entries for contributors no longer on the RAiD. One
notification covers all of these, so the API needs no separate "contributor
removed" call. A failure in the updater never fails a RAiD save.

## The email never reaches the RAiD API

The app saves the RAiD first, then sends the handle, the contributor's iD
and the email straight to the updater. The updater checks the minter can
edit the RAiD by calling the API's permissions endpoint with the minter's
token, reads the title and service point name from the API, sends the
invitation and keeps only a keyed hash (HMAC-SHA-256), and only when the
researcher ticks "Remember me". The API's leftover contributor `email` field
is removed in RAID-976.

## Callers and credentials

| Caller | Endpoints | Credential |
|---|---|---|
| RAiD app | register invitation | Minter's Keycloak access token |
| RAiD API | RAiD changed, status | Shared API key (`X-Api-Key`) |
| RA administrator | data deletion, put code import | Keycloak token with `orcid-updater-admin` |
| Researcher | invitation and callback pages | Invitation token and OAuth state only |

The API uses an API key rather than a Keycloak service account so the API
needs no token handling for one internal call; both sides read it from
configuration.

## Researcher pages are hosted by the updater

The landing, invalid-link, decline and confirmation pages are served by the
updater, so the researcher flow does not depend on the app. This is a joint
decision with the app owner (RAID-977) and is pending their agreement.

## Other defaults

* Invitations expire after 14 days (configurable) and fall back to
  UNAUTHENTICATED. At most one invitation per researcher while a link is
  valid.
* Failed writes are retried with back-off up to a configured limit, then
  recorded for operators. There are no background retries; the next status
  change or save tries again.
* A deletion request removes the entries the updater wrote, revokes the
  token at ORCID, then deletes the hash, consent time, token and put codes.
* RAiD AU keeps its existing ORCID client, so put codes from the central
  updater are imported (from a CSV file, so the updater needs no AWS access)
  and existing entries are updated, not duplicated. Other RAs start empty.


# Consequences

* RAID-976 adds `PUT /contributor/status`, the "RAiD changed" notification,
  the API key configuration, and removes the contributor `email` field.
* Each RA must provide PostgreSQL, SMTP, an ORCID member client and two
  Keycloak clients or roles. RAID-886 documents the settings.
* The updater depends on the RAiD API being reachable to send invitations
  and to write; the API does not depend on the updater to save.


# Open questions

* When a researcher authenticates as a different iD from the one the minter
  typed, the RAiD still names the typed iD. Whether the app corrects the
  RAiD, or the updater reports the mismatch, is for RAID-976 and RAID-977.
* ORCID's current write scope, its token revocation mechanism and the
  revoke-access steps for researchers are still to be verified (RAID-1003,
  RAID-1006).
