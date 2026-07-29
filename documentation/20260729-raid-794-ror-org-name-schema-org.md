# RAID-794: Include resolved organisation name (from ROR) in schema.org output

- **JIRA**: [RAID-794](https://ardc.atlassian.net/browse/RAID-794) (Story)
- **Related**: [RAID-756](https://ardc.atlassian.net/browse/RAID-756) (vocabulary label resolution), HELP-2753 (RDA request that prompted this)
- **PR**: https://github.com/au-research/raid-au/pull/587
- **Date**: 2026-07-29

## What changed and why

The static site's schema.org / JSON-LD `Organization` nodes emitted the ROR URI
and a `PropertyValue` identifier but not the organisation's resolved name (for
example `https://ror.org/03sd43014` without "QCIF Ltd."). Downstream consumers
such as RDA's NESP Domain Data Portal therefore had to call the ROR API per
record to obtain the name (raised via HELP-2753).

The static site already resolves and caches ROR names at build time
(`scripts/fetch-ror.js` writes them to `.ror-cache.json` and onto
`organisation[].rorDetails`). The JSON-LD builder simply wasn't consuming them.
This change consumes the existing build-time data instead of adding any
render-time ROR calls, consistent with how RAID-756 handled vocabulary-backed
label resolution (build-time, no runtime fetch). This approach was confirmed
with Rob per acceptance criterion 3.

### Changes

- `raid-agency-app-static/src/utils/json-ld.ts`
  - `buildOrganisationRole` emits `name` from `organisation.rorDetails?.name` on
    member and funder `Organization` nodes.
  - `parentOrganization` emits the registration agency's resolved ROR name.
  - Both use a conditional spread, so a node still emits with just the ROR
    identifier when the name is unavailable (graceful degradation).
  - The three org-role helpers now take `OrganisationDetails`; the registration
    agency is read as `RegistrationAgencyDetails`.
- `raid-agency-app-static/scripts/fetch-ror.js`
  - Build-time enrichment now also resolves the registration agency's ROR name
    (previously only `organisation[]` was enriched), reusing the same ROR cache.
- `raid-agency-app-static/src/model/raid/RegistrationAgencyDetails.ts` (new)
  - `*Details` model type mirroring `OrganisationDetails`, exported from
    `model/raid/index.ts`.
- `raid-agency-app-static/src/utils/json-ld.test.ts`
  - Five new tests covering name present on member/funder/parentOrganization and
    name omitted when `rorDetails` is absent or the name is an empty string.

## Acceptance criteria

1. Organization node includes `name` alongside the ROR PropertyValue — done.
2. Graceful degradation when the name is unavailable — done.
3. Approach reuses build-time resolution, consistent with RAID-756, confirmed
   with Rob — done.

## Testing

- 43 json-ld unit tests pass (5 new).
- `tsc --noEmit` clean for the changed files.
- `scripts/fetch-ror.js` enrichment smoke-tested with a mocked ROR response:
  both `organisation` and `registrationAgency` receive `rorDetails.name`, and
  non-ROR ids are skipped without error.

## Follow-up (out of scope)

`OrganisationDetails.rorDetails` is declared as `{rorId, name, country, types}`
but `getSimplifiedRorInfo` actually returns `{rorId, name, type, rorUrl}`. This
change only reads `.name`, so it is unaffected, but the inaccurate type is worth
a follow-up ticket now that `json-ld.ts` consumes `rorDetails`.
