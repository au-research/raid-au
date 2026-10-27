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
| Researcher, through the app | get invitation, start and complete authentication | Invitation token and OAuth state only |

The API uses an API key rather than a Keycloak service account so the API
needs no token handling for one internal call; both sides read it from
configuration.

## Researcher pages belong to the app; the updater is API only

Decided 2026-10-26 (RAID-1003). The landing, invalid-link, decline and
confirmation pages are React pages in the RAiD app, built under RAID-977.
The updater serves no pages: it provides JSON endpoints the app calls.

* Invitation links point into the app, at
  `<ORCID_UPDATER_APP_BASE_URL>/invitations/{token}`. The app reads what to
  show from `GET /invitations/{token}`.
* "Authenticate with ORCID" calls `POST /invitations/{token}/authentications`,
  which returns ORCID's authorisation URL for the app to send the browser to.
* ORCID's redirect URI is the app's `<ORCID_UPDATER_APP_BASE_URL>/orcid/callback`
  page, which passes ORCID's `state` and `code` or `error` to
  `POST /authentications`. The updater still holds the ORCID client secret
  and does the code exchange.
* Only the app's origin may call the updater from a browser (CORS, without
  credentials).
* Because the link token is in the app page's URL, the app's pages must send
  `Referrer-Policy: no-referrer`, so the token never reaches ORCID.

An earlier draft had the updater serve the pages itself, so that the
researcher flow would not depend on the app. It was rejected (RL,
2026-10-26): the researcher interface is the app's, built in React under
RAID-977, and is outside the updater's scope.

## What each ORCID entry says

Decided 2026-10-27 (RAID-1004). Each authenticated contributor gets one
membership entry per RAiD, sent as ORCID JSON (`application/vnd.orcid+json`).
The fields follow the central updater (raid-aws-private
`handler/orcid-writer`) so RAiD AU's existing entries keep their shape:

* department-name: the current primary title, else any current title.
* role-title: the label of the contributor's position with the latest start
  date, with that position's start and end dates.
* organization: the organisation that owns the RAiD (`identifier.owner.id`),
  looked up in ROR.
* url and an external id of type `raid`, relationship `self`: the RAiD URL.

**Every entry names the RAiD owner's organisation, because ORCID requires
one.** The RAiD schema has no affiliation for a contributor, so for a
contributor from another organisation the entry names an organisation they
may not belong to. We first decided to name it only on a principal
investigator's entry and leave it off the others. The ORCID sandbox
rejected a membership without an organisation
(`400 Bad request: organization must be present and valid`, 2026-10-27),
even though ORCID's affiliations tutorial says it is optional when an
external id is given. Naming the owner organisation on every entry is what
the central updater already does (RL, 2026-10-27).

If ROR can't find the owner organisation, the write is recorded as failed
without calling ORCID, and the next save of the RAiD tries again.

Revisit if the RAiD schema gains a contributor affiliation, or if ORCID
offers a section that fits a RAiD without an organisation.

How writes behave:

* The put code ORCID returns is kept per researcher and RAiD
  (`orcid_entry`), so later saves update the same entry.
* If ORCID says the stored put code no longer exists, the entry is created
  again, as RAID-975 requires. This also restores an entry the researcher
  deleted at ORCID while they are still on the RAiD; revoking access is how
  a researcher stops the updater writing.
* Entries of contributors removed from the RAiD are deleted at ORCID. If the
  researcher is no longer authenticated, the row is kept with a failure
  recorded, since the entry can't be deleted without their token.
* A 401 from ORCID means the researcher revoked access: they are marked
  revoked, nothing more is written for them, and their email fingerprint no
  longer matches new invitations, until they authenticate again.
* 5xx, 429 and no response are retried with back-off (3 attempts from 2
  seconds by default). Other errors are recorded straight away. The last
  failure is kept on the `orcid_entry` row with ORCID's message, never a
  token.
* Each researcher's write holds that iD's advisory lock, so two saves of
  the same RAiD can't both create an entry.

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
* Resolved 2026-10-26 (RAID-1003): the updater requests
  `/read-limited /activities/update`. ORCID's affiliations tutorial says
  editing memberships needs `/activities/update` and that `/read-limited`
  should be requested for reading them
  (https://github.com/ORCID/ORCID-Source/blob/main/orcid-api-web/tutorial/affiliations.md).
* ORCID documents token revocation as `POST /oauth/revoke` with the client
  ID, client secret and token
  (https://info.orcid.org/ufaqs/how-can-i-revoke-tokens/). RAID-1006 still
  has to confirm it against the sandbox.
* How the app's confirmation page tells researchers to revoke access at
  ORCID is for RAID-977. ORCID's help centre refuses automated requests, so
  the button wording was not verified here.
