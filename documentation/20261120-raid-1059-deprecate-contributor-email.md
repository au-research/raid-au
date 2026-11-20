# RAID-1059: Deprecate the contributor email field

- JIRA: [RAID-1059](https://ardc.atlassian.net/browse/RAID-1059), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976)
- Branch: `feature/RAID-1059` into `feature/RAID-976`
- PR: [#800](https://github.com/au-research/raid-au/pull/800)
- Status: in review

## What changed and why

Decision (Rob, 2026-11-12): the contributor `email` field in the RAiD API is
deprecated and ignored. The API never stores or logs it, so Member API clients
that still send it keep working. The RAiD app sends the email to the
registration agency's ORCID updater instead.

[RAID-569](https://ardc.atlassian.net/browse/RAID-569) removed the field from
the legacy 3.0 spec and the RAiD app, but it stayed in the LinkML data model,
which generates the 3.1 specs and the Java models. So until now an email sent
on mint, update or patch was saved in the RAiD's metadata, returned on read
and written to the RDF output.

- **Ignored on save**: `ContributorService.dropEmail` clears each
  contributor's email on mint, update and patch, before anything is saved or
  sent.
- **RDF**: `RaidRdfService` no longer writes a contributor's `vcard:email`.
- **Spec**: the LinkML `email` attribute is marked deprecated, and its
  description in the generated JSON schemas and both 3.1 OpenAPI specs says it
  is ignored. The LinkML JSON schema generator doesn't emit a `deprecated`
  flag, so the description carries the deprecation.
- **ADR**: `doc/adr/2026-10-23_per-ra-orcid-updater.md` now says the field is
  deprecated and ignored rather than removed.

## Not changed

RAiDs saved before this change may still hold a contributor email in their
stored metadata, and a read returns it until the RAiD is next updated. Removing
stored emails would give each affected RAiD a new version, so it is left for a
separate decision.

## Testing

- Unit tests: `ContributorServiceTest` (email dropped, no contributors),
  `RaidServiceTest` (mint, update and patch drop the email before saving),
  `RaidRdfServiceTest` (no `vcard:email` in the RDF). 1,118 run, 0 failures,
  18 skipped.
- Integration: `ContributorsIntegrationTest` checks that an email sent on mint
  and on update is accepted and not stored. Full suite: 276 run, 0 failures,
  16 skipped (existing).
