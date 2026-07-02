# Fix branch pipeline e2e failures caused by oversized JWT (RAID-617)

**Date:** 2026-07-03
**JIRA:** [RAID-617](https://ardc.atlassian.net/browse/RAID-617)

## Problem

Five e2e tests failed consistently in the `Branch-Build-Deploy` pipeline for `feature/RAID-617` (execution `6f46606c`), timing out while waiting for the RAiD list grid or the post-save URL. The API, frontend config and CORS setup all checked out when tested with curl, which made the failure look environment-specific.

## Root cause

The access token for `raid-test-user` had grown too large for the API to accept:

1. Every RAiD mint appends the new handle to the user's `adminRaids` attribute in Keycloak (`RaidService.mint()` → `addHandleToAdminRaids`).
2. A protocol mapper on the `raid-api` client copies that attribute into the `admin_raids` JWT claim. After months of e2e runs the claim held **213 handles**, making the token ~7.5KB.
3. Browser requests (token plus ~1KB of standard Chrome headers) exceeded Tomcat's default 8KB request header limit. Tomcat rejected them with a bare 400 (`text/html`, no CORS headers) **before Spring's filter chain ran**.
4. Chrome reported the missing `Access-Control-Allow-Origin` header as a CORS error, and the frontend stayed stuck on its Loading state.

Curl requests stayed just under the 8KB limit because curl sends almost no extra headers — which is why every server-side check passed while the browser failed. The decisive evidence came from a Playwright CDP capture (`Network.responseReceivedExtraInfo`), which exposed the raw 400 hidden behind Chrome's CORS error.

## Fix

Test data cleanup only — no code change:

- Backed up `raid-test-user`'s attributes, then trimmed `adminRaids` to empty via the Keycloak admin API on `iam.test.raid.org.au`. Token size dropped from 7,459 to 1,758 characters.
- Verified all 21 e2e tests pass locally against `app-raid-617.test.raid.org.au`.
- Retried the failed `E2e-Test` action on pipeline execution `6f46606c` — succeeded at 08:26 AEST on 3 July 2026.

## Follow-up

- Once RAID-617 merges (permissions fetched server-side via the Keycloak SPI, no JWT claim needed), remove the `admin_raids` protocol mapper from the `raid-api` client in each realm. It cannot be removed earlier because main still reads the claim (`SecurityConfig`, `RaidService`, `TokenUtil`).
- Until then the attribute regrows by a few handles per e2e run; at that rate it will take a long time to approach the limit again.
- The dead `RaidAdminAuthorizationManager` class on the RAID-617 branch (unreferenced, still reads `admin_raids`) should be deleted before the PR merges.
