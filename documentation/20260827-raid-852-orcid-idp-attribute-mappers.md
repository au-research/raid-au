# RAID-852: ORCID identity provider attribute mappers

**Date:** 2026-08-27
**JIRA:** [RAID-852](https://ardc.atlassian.net/browse/RAID-852) — ORCID login writes the ORCID iD into First Name and username
**PR:** [au-research/raid-au#628](https://github.com/au-research/raid-au/pull/628)

## Background

John Aspler (CRKN, Canada) reported in the `#raid-ra-tech` Slack channel on 27 August 2026
that when a user authenticates with ORCID while creating a RAiD account, the ORCID iD
itself is written into the **First Name** field and is also used as the username. He also
asked whether the ORCID iD is meant to appear anywhere on the user profile.

Matthias Liffers suspected a Keycloak attribute mapping issue. That is correct, and the
investigation below confirms the exact mechanism.

## Root cause

Two independent facts combine.

**ORCID does not release the claims Keycloak falls back on.** Its OIDC discovery document
advertises `scopes_supported = ["openid"]` and
`claims_supported = ["family_name", "given_name", "name", "auth_time", "iss", "sub"]`.
There is no `email` and no `preferred_username`. Separately, ORCID returns the name claims
from the **userinfo endpoint only** — its `id_token` carries no name claims at all.

**Keycloak's fallbacks then select the ORCID iD.**
`OIDCIdentityProvider.extractIdentity` resolves the username as `preferred_username`, then
`email`, then the subject identifier; with ORCID the first two are absent, so the username
becomes the ORCID iD. For the name, `firstName` is set only when `given_name` is present;
when `given_name` and `family_name` are both missing, Keycloak calls the deprecated
`BrokeredIdentityContext.setName(name)`, which splits on the last space and assigns the
**entire string to `firstName`** when there is no space in the value.

This is reproducible for any registration agency, because the shipped
`iam/realms/raid-realm.json` contains no identity providers and no identity provider
mappers (`identityProviders: []`, `identityProviderMappers: []`), and
`iam/doc/login-page-configuration.md` mentioned ORCID without saying anything about
attribute mappers.

## What changed

| File | Change |
|------|--------|
| `iam/doc/orcid-identity-provider.md` | New guide covering the problem, the root cause, the endpoint configuration, the mappers, the username decision, and the private-name limitation. Includes six admin console screenshots. |
| `iam/doc/images/orcid-idp/*.png` | Six screenshots captured from Keycloak 26.6.2 with the fix applied. |
| `scripts/configure-orcid-idp.sh` | New idempotent script that creates the ORCID identity provider and its three mappers against any RAiD Keycloak. |
| `iam/doc/login-page-configuration.md` | Cross-reference to the new guide from the identity provider step. |

## The configuration

The identity provider must have a **User Info URL** set and **Disable user info** off,
otherwise no name claims ever reach Keycloak. Three **Attribute Importer** mappers are
then added, all with sync mode `Force`:

| Name | Claim | User attribute |
|------|-------|----------------|
| `orcid-given-name` | `given_name` | `firstName` |
| `orcid-family-name` | `family_name` | `lastName` |
| `orcid-id` | `sub` | `orcid` |

`Force` matters: the default sync mode applies mappers only when the account is first
created, so accounts that already carry the ORCID iD in First Name would keep it. With
`Force`, those profiles are re-imported from ORCID on the user's next login and correct
themselves. This is what makes the fix retroactive for CRKN's existing test accounts.

The `orcid-id` mapper answers John's second question: the ORCID iD is retained as a
dedicated `orcid` user attribute, so the app can display it on the profile without it
being the user's name.

## Decisions

**The username keeps the ORCID iD.** John suggested the ORCID iD should not be used as the
username. Since ORCID supplies no `email` and no `preferred_username`, the only
alternative is to derive it from the name via a Username Template Importer, which trades a
uniqueness and stability guarantee for cosmetics — two researchers with the same name
collide, and a name change breaks the username. Once the mappers are in place the ORCID iD
is no longer visible as the user's name, which addresses the substance of the complaint.
The guide documents the alternative so an agency can choose it deliberately.

**No change to the shipped realm.** Adding an ORCID identity provider to
`raid-realm.json` would require per-agency client credentials and would alter every
agency's realm import, including the ARDC's. Documentation plus a script is the portable
fix, consistent with agencies not running on AWS.

## Verification

- The configuration was applied to a local Keycloak 26.6.2 (`raid` realm) and read back
  through the admin API: `userInfoUrl` set, `disableUserInfo=false`, `defaultScope=openid`,
  `syncMode=FORCE`, and three mappers with the claims and target attributes above.
- The script was run twice against the same realm to confirm it is idempotent — the second
  run left exactly three mappers.
- Screenshots were captured from that configured instance with Playwright.

**Not verified:** an end-to-end ORCID sign-in. That needs ORCID member API credentials and
a test ORCID account, which were not available. The claim analysis is drawn from ORCID's
published discovery document and Keycloak's source rather than from an observed login.

## Known limitation

The mappers cannot recover a name ORCID does not release. ORCID applies the record's
visibility setting to the name returned from userinfo, so if a user's name is not public,
`given_name` and `family_name` are omitted and First Name cannot be populated from ORCID
at all. Those users must either make their name public or complete their profile in RAiD.

`iam/doc/login-page-configuration.md` instructs agencies to disable the **Review Profile**
execution in the First Broker Login flow, which means a missing name is silently accepted
rather than prompted for. Agencies that want users to correct their own details at first
login should leave Review Profile enabled. This is noted in the new guide but not changed,
since it affects all identity providers and not just ORCID.
