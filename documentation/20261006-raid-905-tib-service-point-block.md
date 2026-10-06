# RAID-905: allocate a Service Point ID block to TIB

- JIRA: [RAID-905](https://ardc.atlassian.net/browse/RAID-905) (Task)
- Related: [RAID-806](https://ardc.atlassian.net/browse/RAID-806) (per-agency Service Point ID ranges), [RAID-895](https://ardc.atlassian.net/browse/RAID-895) (the same change for DRAC)
- PR: [#703](https://github.com/au-research/raid-au/pull/703)

## Problem

RAID-806 gave each Registration Agency its own Service Point ID block, recorded in `api-svc/raid-api/src/main/resources/registration-agencies.yaml`. Block 6 was allocated to TIB, but TIB's ROR was not known at the time, so the file held only a `TODO` comment reserving the block.

An instance resolves its block from `raid.identifier.registration-agency-identifier` and refuses to start if that ROR is not in the register. Without an entry, a TIB deployment would fail at startup with `RegistrationAgencyNotRegisteredException`.

## What changed and why

- Added a TIB entry to the register: ROR `https://ror.org/04aj4c181`, block 6. This replaces the `TODO`. Block 6 starts at Service Point ID 60000000.
- Checked the ROR against the ROR API before adding it. The record is active and names "TIB – Leibniz Information Centre for Science and Technology", Hanover.
- Left `instance` out, as for SURF, because TIB's hostname is not yet known. The field is documentation only, so it can be added later without effect.
- `RegistrationAgencyRegisterTest.derivesStartFromBlock` now asserts that TIB's ROR resolves to 60000000 in the shipped register. Removing or renumbering the entry fails the build.

No documentation under `doc/` needed changing. `doc/reference/deployment-configuration.md` and `doc/reference/service-point-id-ranges.md` describe the register generally and do not list agencies.

## Testing

- Register unit tests: 18 of 18 pass (`RegistrationAgencyRegisterTest`, `ServicePointIdRangeConfigTest`).
- Full intTest suite run locally against this branch's API: 256 tests, 0 failures, 0 errors, 16 skipped.

## Deployment

TIB is not yet deployed, so this change blocks nothing today. The entry ships in the next release, and a TIB instance must run that release or later. Setting `raid.identifier.registration-agency-identifier` to `https://ror.org/04aj4c181` is TIB's own deployment configuration.
