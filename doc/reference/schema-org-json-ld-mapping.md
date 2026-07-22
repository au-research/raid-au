# RAiD to schema.org JSON-LD mapping

This page documents the schema.org JSON-LD that RAiD embeds in each public RAiD
landing page. It lists every RAiD field that appears in the output, the
schema.org property and type it maps to, and a worked example. Use it to
understand what a harvester or search engine sees without reading the source
code.

## Audience and scope

This reference is for anyone consuming RAiD metadata from the public web, such as
Research Data Australia (RDA) harvesters and search-engine crawlers.

The mapping described here applies to the **RAiD landing pages** served by the
static site (`raid-agency-app-static`). Each landing page at
`/raids/{prefix}/{suffix}` embeds a single JSON-LD block:

```html
<script type="application/ld+json">
  { "@context": "https://schema.org", "@type": "ResearchProject", ... }
</script>
```

The RAiD REST API and the DataCite export use their own metadata
representations. Those are out of scope for this page, which covers only the
landing-page JSON-LD.

The source of truth is
[`raid-agency-app-static/src/utils/json-ld.ts`](../../raid-agency-app-static/src/utils/json-ld.ts).
Keep this page in sync with that file when the mapping changes.

## Top-level shape

Every RAiD landing page emits one root object:

| JSON-LD property | Value |
| --- | --- |
| `@context` | `https://schema.org` |
| `@type` | `ResearchProject` |

A RAiD is modelled as a schema.org
[`ResearchProject`](https://schema.org/ResearchProject). Contributors,
organisations, subjects, related objects and related RAiDs hang off that root
node as described below.

## Field mapping

The table below lists each RAiD field, the schema.org property it maps to, the
type of the emitted value, and notes. Properties marked *omitted when empty* do
not appear in the output at all when the RAiD has no value for them.

| RAiD field | schema.org property | Type | Notes |
| --- | --- | --- | --- |
| `identifier.id` | `@id` and `identifier` | `PropertyValue` | The RAiD handle. `identifier.propertyID` is the RAiD registry URI and `identifier.name` is `RAiD`. |
| `title[]` (all) | `name` | Text | All title texts joined with ` \| `. |
| `title[]` (primary) | `headline` | Text | The primary title (`title.type.schema/5`), or the first title if none is primary. |
| `description` (primary) | `description` | Text | The primary description (`description.type.schema/318`). *Omitted when empty.* |
| `date.startDate` | `foundingDate` | Date | |
| `date.endDate` | `dissolutionDate` | Date | *Omitted when empty.* |
| `identifier.registrationAgency.id` | `parentOrganization` | `Organization` | The registration agency, identified by a ROR `PropertyValue`. |
| `contributor[]` | `member` | `Role[]` | One `Role` per position and per CRediT role (see [Contributors](#contributors)). |
| `organisation[]` (non-funder) | `member` | `Role[]` | One `Role` per non-funder organisation role. |
| `organisation[]` (funder) | `funder` | `Role[]` | One `Role` per funder role (`organisation.role.schema/186`). |
| `subject[]` | `knowsAbout` | `DefinedTerm[]` | ANZSRC Fields of Research (see [Subjects](#subjects)). |
| `relatedObject[]` | `citation` | `CreativeWork[]` | Inputs and outputs, as a flat list (see [Related objects](#related-objects)). *Omitted when empty.* |
| `relatedRaid[]` | `isPartOf`, `hasPart`, `isBasedOn`, `isRelatedTo` | `ResearchProject[]` | Grouped by relationship type (see [Related RAiDs](#related-raids)). *Each omitted when empty.* |

### Contributors

Each contributor becomes one or more schema.org
[`Role`](https://schema.org/Role) nodes under `member`. The person sits inside
`Role.member` as a [`Person`](https://schema.org/Person) identified by their
ORCID.

- Each **position** (for example Principal or Chief Investigator) produces a
  `Role` carrying `startDate` and `endDate`.
- Each **CRediT role** (for example Conceptualization) produces a `Role` without
  dates.

`Role.roleName` holds the resolved human-readable label. Position and
organisation-role labels come from the RAiD vocabulary; CRediT labels are
derived from the role URI (see [Vocabulary labels](#vocabulary-labels)).

### Organisations

Each organisation role becomes a `Role` whose `member` is an
[`Organization`](https://schema.org/Organization) identified by its ROR. Funder
roles (`organisation.role.schema/186`) are emitted under `funder`; all other
organisation roles are emitted under `member`. An organisation that holds both a
funder role and another role appears in both `funder` and `member`.

### Subjects

Each subject becomes a [`DefinedTerm`](https://schema.org/DefinedTerm) under
`knowsAbout`, with the subject URI as `@id`, the resolved ANZSRC label as `name`
(omitted if the code is not in the mapping), and the vocabulary URI as
`inDefinedTermSet`.

### Related objects

Related objects (both inputs and outputs) are emitted as a flat list of
[`CreativeWork`](https://schema.org/CreativeWork) nodes under `citation`. For
each related object:

- `@id` is the object identifier (for example a DOI URL).
- `identifier` is a `PropertyValue`. DOI, ARK and ISBN identifiers use their
  registry `propertyID` and name; every other scheme falls back to `name: "URL"`
  with the scheme URI as `propertyID`.
- `name` carries the formatted citation text (the APA-style reference shown on
  the landing page) when it is available.
- `additionalType` carries the related-object **category** URI (Input, Output or
  Internal), or an array of URIs when there is more than one category. It is
  omitted when the object has no category.

Two points to note:

- `additionalType` holds the raw category URI, not a resolved label. See the
  [category reference](#related-object-categories) below.
- The related-object *type* (for example Journal Article) is not emitted.

### Related RAiDs

Each related RAiD is emitted as a
[`ResearchProject`](https://schema.org/ResearchProject) reference, grouped under
the schema.org property that matches its relationship type:

| RAiD relationship type | schema.org property |
| --- | --- |
| IsPartOf (`relatedRaid.type.schema/202`) | `isPartOf` |
| HasPart (`relatedRaid.type.schema/201`) | `hasPart` |
| IsDerivedFrom (`relatedRaid.type.schema/200`) | `isBasedOn` |
| Any other type | `isRelatedTo` |

Relationships that map to `isRelatedTo` also carry the original relationship
type on `relationshipType` (the URI) and, when the label is known,
`relationshipTypeName` (the resolved label). The three explicitly mapped
relationships do not carry these extra properties, because the schema.org
property already conveys the relationship.

## Vocabulary labels

Several fields resolve a RAiD vocabulary URI to a human-readable label so
harvesters receive readable values rather than bare URIs:

| Field | Label source |
| --- | --- |
| Contributor position (`Role.roleName`) | RAiD general vocabulary mapping |
| Organisation role (`Role.roleName`) | RAiD general vocabulary mapping |
| CRediT contributor role (`Role.roleName`) | Derived from the role URI slug |
| Subject (`DefinedTerm.name`) | ANZSRC subject mapping |
| `isRelatedTo` relationship (`relationshipTypeName`) | RAiD general vocabulary mapping |

When a URI is not found in the mapping, the output falls back to the raw URI (or,
for the subject `name`, omits the label).

### Related object categories

`citation.additionalType` holds the category URI. The current categories are:

| Category URI | Meaning |
| --- | --- |
| `relatedObject.category.id/190` | Output |
| `relatedObject.category.id/191` | Input |

## Worked example

The following abbreviated RAiD:

- Titles: "A study of coastal ecosystems" (primary) and "Coastal study" (alternative)
- Primary description
- Runs from 2025-01-01 to 2026-12-31
- Registration agency: `https://ror.org/038sjwq14`
- One contributor with a Principal or Chief Investigator position and a
  Conceptualization CRediT role
- One organisation acting as a Contractor and another as a Funder
- One subject (ANZSRC 420399)
- One related output (a journal article DOI) with citation text
- Two related RAiDs: one IsPartOf and one Continues

produces this JSON-LD:

```json
{
  "@context": "https://schema.org",
  "@type": "ResearchProject",
  "@id": "https://raid.org/10.26259/0d7f1865",
  "name": "A study of coastal ecosystems | Coastal study",
  "headline": "A study of coastal ecosystems",
  "identifier": {
    "@type": "PropertyValue",
    "propertyID": "https://registry.identifiers.org/registry/raid",
    "name": "RAiD",
    "value": "https://raid.org/10.26259/0d7f1865"
  },
  "parentOrganization": {
    "@type": "Organization",
    "@id": "https://ror.org/038sjwq14",
    "identifier": {
      "@type": "PropertyValue",
      "propertyID": "https://registry.identifiers.org/registry/ror",
      "name": "ROR",
      "value": "https://ror.org/038sjwq14"
    }
  },
  "description": "The aim of the study is to investigate coastal ecosystem change.",
  "foundingDate": "2025-01-01",
  "dissolutionDate": "2026-12-31",
  "member": [
    {
      "@type": "Role",
      "@id": "https://vocabulary.raid.org/contributor.position.schema/307",
      "roleName": "Principal or Chief Investigator",
      "startDate": "2025-01-01",
      "endDate": "2026-12-31",
      "member": {
        "@type": "Person",
        "@id": "https://orcid.org/0000-0002-4582-7728",
        "identifier": {
          "@type": "PropertyValue",
          "propertyID": "https://registry.identifiers.org/registry/orcid",
          "name": "ORCID",
          "value": "https://orcid.org/0000-0002-4582-7728"
        }
      }
    },
    {
      "@type": "Role",
      "@id": "https://credit.niso.org/contributor-roles/conceptualization/",
      "roleName": "Conceptualization",
      "member": {
        "@type": "Person",
        "@id": "https://orcid.org/0000-0002-4582-7728",
        "identifier": {
          "@type": "PropertyValue",
          "propertyID": "https://registry.identifiers.org/registry/orcid",
          "name": "ORCID",
          "value": "https://orcid.org/0000-0002-4582-7728"
        }
      }
    },
    {
      "@type": "Role",
      "@id": "https://vocabulary.raid.org/organisation.role.schema/185",
      "roleName": "Contractor",
      "startDate": "2025-01-01",
      "member": {
        "@type": "Organization",
        "@id": "https://ror.org/04yx6dh41",
        "identifier": {
          "@type": "PropertyValue",
          "propertyID": "https://registry.identifiers.org/registry/ror",
          "name": "ROR",
          "value": "https://ror.org/04yx6dh41"
        }
      }
    }
  ],
  "funder": [
    {
      "@type": "Role",
      "@id": "https://vocabulary.raid.org/organisation.role.schema/186",
      "roleName": "Funder",
      "startDate": "2025-01-01",
      "member": {
        "@type": "Organization",
        "@id": "https://ror.org/03yrm5c26",
        "identifier": {
          "@type": "PropertyValue",
          "propertyID": "https://registry.identifiers.org/registry/ror",
          "name": "ROR",
          "value": "https://ror.org/03yrm5c26"
        }
      }
    }
  ],
  "knowsAbout": [
    {
      "@type": "DefinedTerm",
      "@id": "https://linked.data.gov.au/def/anzsrc-for/2020/420399",
      "name": "Health services and systems not elsewhere classified",
      "inDefinedTermSet": "https://vocabs.ardc.edu.au/viewById/316"
    }
  ],
  "citation": [
    {
      "@type": "CreativeWork",
      "@id": "https://doi.org/10.1007/s00442-014-2977-8",
      "name": "Smith, J. (2014). A study of things. Oecologia, 176(2), 1-10.",
      "identifier": {
        "@type": "PropertyValue",
        "propertyID": "https://registry.identifiers.org/registry/doi",
        "name": "DOI",
        "value": "https://doi.org/10.1007/s00442-014-2977-8"
      },
      "additionalType": "https://vocabulary.raid.org/relatedObject.category.id/190"
    }
  ],
  "isPartOf": [
    {
      "@type": "ResearchProject",
      "@id": "https://raid.org/10.71821/a945d761",
      "identifier": "https://raid.org/10.71821/a945d761"
    }
  ],
  "isRelatedTo": [
    {
      "@type": "ResearchProject",
      "@id": "https://raid.org/10.26259/efgh5678",
      "identifier": "https://raid.org/10.26259/efgh5678",
      "relationshipType": "https://vocabulary.raid.org/relatedRaid.type.schema/204",
      "relationshipTypeName": "Continues"
    }
  ]
}
```

## Maintenance

This page is a manual reference. When the mapping in `json-ld.ts` changes, update
the field table, the notes and the worked example to match. The unit tests in
`json-ld.test.ts` are the authoritative record of the current behaviour.
