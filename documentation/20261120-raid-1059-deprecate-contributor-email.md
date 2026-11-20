# RAID-1059: Remove the contributor email field

- JIRA: [RAID-1059](https://ardc.atlassian.net/browse/RAID-1059), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976)
- Branch: `feature/RAID-1059` into `feature/RAID-976`
- PR: [#800](https://github.com/au-research/raid-au/pull/800)
- Status: in review

## What changed and why

Decision (Rob, 2026-11-12): the API ignores the contributor `email` field, so
Member API clients that still send it keep working. The RAiD app sends the
email to the registration agency's ORCID updater instead. On 2026-11-20 Rob
chose to remove the field from the data model rather than keep it as a
deprecated field the API clears.

[RAID-569](https://ardc.atlassian.net/browse/RAID-569) removed the field from
the legacy 3.0 spec and the RAiD app, but it stayed in the LinkML data model,
which generates the 3.1 specs and the Java models. So until now an email sent
on mint, update or patch was saved in the RAiD's metadata, returned on read
and written to the RDF output.

- **Data model**: `email` is removed from the LinkML `Contributor` class. The
  regenerated JSON schemas and both 3.1 OpenAPI specs no longer list it, and
  the generated Java `Contributor` has no email.
- **Ignored, not refused**: the API uses Jackson's default of skipping unknown
  properties, so a request that still sends an email succeeds and the email
  goes nowhere. No code clears it.
- **RDF**: `RaidRdfService` no longer writes `vcard:email`.

The schema's `additionalProperties: false` now describes a request with an
email as invalid, but the API doesn't enforce that at runtime.

## Stored emails

The API reads saved metadata into the generated model, which has no email, so
an email stored before this change is no longer returned on read and is
dropped the next time the RAiD is saved. It remains in the stored metadata and
history until then.

## Testing

- Unit tests: 1,115 run, 0 failures, 18 skipped.
- Integration: `ContributorsIntegrationTest` sends raw JSON with a contributor
  email on mint and on update, and checks each is accepted and that a read
  returns no email. Full suite: 276 run, 0 failures, 16 skipped (existing).
