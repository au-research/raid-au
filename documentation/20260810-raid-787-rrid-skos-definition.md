# RAID-787 follow-up: RRID (scicrunch) `skos:definition` change

- **JIRA:** [RAID-787](https://ardc.atlassian.net/browse/RAID-787) (task checklist item)
- **Repo to change:** `au-research/raid-skos`
- **File:** `data/core/relatedObject.ttl` (concept block at lines 42-46 on `main`)
- **Type:** vocabulary quality fix (no API code impact)

## What is wrong

The `skos:definition` for the RRID scheme is a placeholder that just repeats the `skos:prefLabel`, so it carries no meaning.

Current:

```turtle
<https://scicrunch.org/resolver/> a skos:Concept ;
    skos:prefLabel "RRID"@en ;
    skos:definition "RRID"@en ;
    skos:inScheme <https://vocabulary.raid.org/relatedObject.schemaUri/scheme> ;
    skos:topConceptOf <https://vocabulary.raid.org/relatedObject.schemaUri/scheme> .
```

## Required change

Replace only the `skos:definition` line with a real, one-sentence definition that describes what belongs under the scheme and includes the RRID format (the ticket asks for the format explicitly). Proposed wording, matching the existing DOI/Handle style:

```turtle
    skos:definition "Research Resource Identifiers (RRIDs) for research resources such as antibodies, cell lines, model organisms, software tools and databases, registered through and resolved via SciCrunch. RRIDs take the form RRID:{Source}_{id} (for example RRID:AB_2298772)."@en ;
```

Leave `skos:prefLabel`, `skos:inScheme`, `skos:topConceptOf` and the concept URI unchanged.

## Style reference (real definitions already in the file)

| Scheme | `skos:definition` |
|--------|-------------------|
| DOI | `"All DOIs, including IGSNs, CrossRef Publication IDs or Grant IDs, DataCite DOIs, instrument DOIs, etc."@en` |
| Handle | `"All non-DOI handles."@en` |
| Archive.org | `"Fallback for any Object that has no ID other than a webpage ..."@en` |

## Why this is safe (no code coupling)

The RRID validator keys off the concept URI `https://scicrunch.org/resolver/`, which is not changing. It does not read `skos:definition`. So this change affects the published vocabulary only, with no impact on the API. (Contrast: the concept URI itself must match `RelatedObjectSchemaUriEnum` exactly, so it must not be touched.)

## Remaining manual steps (per the ticket task)

1. Make the edit above and raise a PR in `au-research/raid-skos`.
2. Regenerate the aggregated vocabulary.
3. Publish to Research Vocabularies Australia.

## Related placeholders in the same file

`ARK` and `ISBN` have the identical placeholder problem (`skos:definition` repeats `skos:prefLabel`). Worth fixing in the same pass, or tracking under their own identifier tickets (siblings under epic RAID-789).
