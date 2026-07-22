# RAID-758: Publish a reference doc for the RAiD to schema.org JSON-LD mapping

## What changed and why

Melanie Barlow (RDA) asked in HELP-2753 for a link to RAiD's intended
schema.org mapping, so she can see the full picture rather than reverse
engineering it from sample records. No such reference existed.

Added a new reference page,
[`doc/reference/schema-org-json-ld-mapping.md`](../doc/reference/schema-org-json-ld-mapping.md),
that documents the schema.org JSON-LD embedded in RAiD landing pages:

- The top-level shape (`ResearchProject`).
- A field-by-field table mapping every RAiD field to its schema.org property and
  type.
- Sections explaining contributors, organisations, subjects, related objects and
  related RAiDs.
- The vocabulary label resolution behaviour.
- A full worked example.

The page names
[`raid-agency-app-static/src/utils/json-ld.ts`](../raid-agency-app-static/src/utils/json-ld.ts)
as the source of truth and includes a maintenance note to keep the two in sync.

## Sequencing

The reference documents the post-fix output. It was written after the two
prerequisite tickets merged to `main`, so it reflects resolved vocabulary labels
and included related outputs rather than the earlier gaps:

- RAID-756 (resolved vocabulary labels in schema.org output) — merged.
- RAID-757 (related outputs included in schema.org output) — merged.

## Notes

- Scope is the static-site landing-page JSON-LD only. The REST API and DataCite
  export use their own representations and are called out as out of scope.
- Next step suggested in the ticket: loop in Rorie Edmunds to review and polish,
  then link the page from HELP-2753.

## Links

- JIRA: [RAID-758](https://ardc.atlassian.net/browse/RAID-758)
- Related: [RAID-756](https://ardc.atlassian.net/browse/RAID-756), [RAID-757](https://ardc.atlassian.net/browse/RAID-757)
- Original request: [HELP-2753](https://ardc.atlassian.net/browse/HELP-2753)
- PR: _to be added_
