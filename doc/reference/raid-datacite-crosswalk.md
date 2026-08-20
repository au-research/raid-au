# RAiD to DataCite crosswalk

Exploratory design note. Direction is **RAiD to DataCite** (the emission
direction: RAiD mints or updates a DOI and pushes metadata to DataCite).

## Scope and sources

- The crosswalk is authored from the live Java factories under
  `api-svc/raid-api/src/main/java/au/org/raid/api/factory/datacite/`, which are
  the real, current implementation.
- The **target** of these factories is the DataCite **REST JSON** `attributes`
  block (the payload of a `POST/PUT /dois` request), not the DataCite XML.
  `datacite.yaml` (in `api-svc/datamodel/src/v2/`) models the DataCite **XML**
  Metadata Schema 4.7 (the version RAID-377 is aligning the mapping to). The two
  share the same conceptual model but are not byte-identical: the
  XML wraps repeatables (`<titles><title>…`), the JSON flattens them
  (`"titles": [ … ]`); the JSON uses `rightsList`, `nameIdentifiers`, camelCase
  keys, and a `types` object; the XML uses `resourceType/@resourceTypeGeneral`.
  Where a factory value maps to a DataCite vocabulary, that vocabulary is the
  same in both serialisations.
- The RAiD source classes/slots are defined in
  `api-svc/datamodel/src/v2/raid-core.yaml`.
- DataCite vocabulary values the code can emit are enumerated in
  `api-svc/raid-api/src/main/java/au/org/raid/api/vocabularies/datacite/`
  (`RelatedIdentifierType`, `RelationType`, `ResourceTypeGeneral`) and in inline
  maps inside the factories.

## Assembly overview

`DataciteAttributesDtoFactory` builds the whole `attributes` object. It has
three near-identical overloads (`RaidCreateRequest`, `RaidUpdateRequest`,
`RaidDto`); the only material difference is that the `RaidDto` overload
null-guards `getContributor()` before building creators. `DataciteDtoFactory`
wraps the attributes with `schemaVersion = "http://datacite.org/schema/kernel-4"`,
`type = "dois"`, and a top-level `identifiers` entry `{identifier: handle,
identifierType: "DOI"}`. `DataciteRequestFactory` wraps that as `{data: …}`.

## Field-by-field mapping

| DataCite `attributes` field | RAiD source | Transformation | Factory / class |
|---|---|---|---|
| `prefix` | the minted `handle` | `handle.split("/")[0]` | `DataciteAttributesDtoFactory` |
| `doi` | `handle` | verbatim | `DataciteAttributesDtoFactory` |
| `url` | `handle` | `identifierProperties.landingPrefix + handle` (config-driven) | `DataciteAttributesDtoFactory` |
| `event` | `access.type.id` | COAR open-access id `…c_abf2` → `"publish"`, else `"register"` | `DataciteAttributesDtoFactory` |
| `publicationYear` | none | **Hardcoded** `String.valueOf(java.time.Year.now())` — see Known issues | `DataciteAttributesDtoFactory` |
| `types` | none | Constant `{resourceType: "RAiD", resourceTypeGeneral: "Project"}` | `DataciteTypesFactory` |
| `publisher` | `identifier.owner` | ROR `owner.id` → `publisherIdentifier`; `publisherIdentifierScheme="ROR"`; `schemeUri=owner.schemaUri`; **`name` resolved via live `RorClient.getOrganisationName`** | `DatacitePublisherFactory` |
| `titles[]` | `title[]` | primary title (`title.type.id …/5`) emitted first with no `titleType`; others mapped via title-type map; `lang` from `title.language.id` | `DataciteTitleFactory` |
| `creators[]` | `contributor[]` | one creator per contributor; `nameType="Personal"`; `nameIdentifiers=[{id, schemeUri, scheme}]`; **`name` resolved via live `OrcidClient`/`IsniClient`**; empty-name creators filtered; empty list → one empty `DataciteCreator` | `DataciteCreatorFactory` |
| `contributors[]` (registration agency) | `identifier.registrationAgency` | `contributorType="RegistrationAgency"`; `name` from `DataciteProperties.registrationAgencyName` (config); `nameType="Organizational"`; ROR nameIdentifier | `DataciteContributorFactory` |
| `contributors[]` (organisations) | `organisation[]` **whose roles are not solely Funder (…/186)** | `nameType="Organizational"`; ROR nameIdentifier; `contributorType` from the latest non-Funder role via role map; **`name` resolved via live `RorClient`** | `DataciteContributorFactory` + split logic in `DataciteAttributesDtoFactory` |
| `fundingReferences[]` | `organisation[]` **with a Funder role (…/186)** | `funderName` (**live `RorClient`**); `funderIdentifier=org.id`; `funderIdentifierType="ROR"`; `schemeUri=org.schemaUri` | `DataciteFundingReferenceFactory` + split logic |
| `dates[]` | `date` | single element; `date = startDate` or `startDate + "/" + endDate`; `dateType="Other"` (constant) | `DataciteDateFactory` |
| `descriptions[]` | `description[]` | `description=text`; `descriptionType` via description-type map; `lang` from `description.language.id` | `DataciteDescriptionFactory` |
| `relatedIdentifiers[]` (related objects) | `relatedObject[]` | excludes objects that are BOTH category `…/191` AND type `…/272`; maps identifier type, resourceTypeGeneral, relationType (see vocab tables) | `DataciteRelatedIdentifierFactory` (RelatedObject overload) |
| `relatedIdentifiers[]` (alternate URLs) | `alternateUrl[]` | `relatedIdentifier=url`; type `URL`; relationType `IsDocumentedBy`; resourceTypeGeneral `Other` (all constant) | `DataciteRelatedIdentifierFactory` (AlternateUrl overload) |
| `relatedIdentifiers[]` (related RAiDs) | `relatedRaid[]` | `relatedIdentifier=id`; type **`DOI`** (constant); relationType via RAiD-relation map; resourceTypeGeneral `Project` (constant) | `DataciteRelatedIdentifierFactory` (RelatedRaid overload) |
| `alternateIdentifiers[]` (agency URL) | `identifier.raidAgencyUrl` | `alternateIdentifier=raidAgencyUrl`; `alternateIdentifierType="RaidAgencyUrl"` (constant, always added first) | `DataciteAlternateIdentifierFactory` (Id overload) |
| `alternateIdentifiers[]` (RAiD alt ids) | `alternateIdentifier[]` | `alternateIdentifier=id`; `alternateIdentifierType="URL"` (**constant — RAiD's own `alternateIdentifier.type` free-text is discarded**) | `DataciteAlternateIdentifierFactory` (AlternateIdentifier overload) |
| `rightsList` | — | **Never populated. See Known issues.** | (`DataciteRightFactory` exists but is unwired) |

### Fields present in the RAiD model but not emitted

`subject`, `spatialCoverage` (present on `RaidDto` slots), `access.statement`,
`access.embargoExpiry`, contributor `position`/`role`/`leader`/`contact`/`email`,
and `alternateIdentifier.type` are **not** carried into the DataCite payload.
DataCite `geoLocations`, `relatedItems`, `sizes`, `formats`, `version`,
`language`, and `subjects` are never produced by the factories.

## Controlled-vocabulary crosswalks

These are the pure code→string lookup maps in the factories. They are the
subset most amenable to a declarative SSSOM mapping table (RAiD vocab URI →
DataCite enum value), because each is a total, side-effect-free function.

### Title type (`DataciteTitleFactory.TITLE_TYPE_MAP`)

| RAiD `title.type.id` | DataCite `titleType` |
|---|---|
| `…/title.type.schema/5` (primary) | *(omitted — primary title carries no titleType)* |
| `…/title.type.schema/4` | `AlternativeTitle` |
| `…/title.type.schema/156` | `Other` |
| `…/title.type.schema/157` | `Other` |

### Description type (`DataciteDescriptionFactory.DESCRIPTION_TYP_MAP`)

| RAiD `description.type.id` | DataCite `descriptionType` |
|---|---|
| `…/description.type.schema/318` | `Abstract` |
| `…/description.type.schema/8` | `Methods` |
| `…/description.type.schema/3, /6, /7, /9, /319` | `Other` |

### Organisation role → contributorType (`DataciteContributorFactory.ORGANISATION_ROLE_MAP`)

| RAiD `organisation.role` | DataCite `contributorType` |
|---|---|
| Lead Research Organisation (`…/182`) | `HostingInstitution` |
| Facility (`…/187`) | `Sponsor` |
| Other Research (`…/183`), Partner (`…/184`), Contractor (`…/185`), Other (`…/188`) | `Other` |
| Funder (`…/186`) | *(routed to `fundingReferences`, not contributors)* |
| Registration Agency | `RegistrationAgency` (constant, separate overload) |

### Related object schemaUri → relatedIdentifierType (`IDENTIFIER_TYPE_MAP`)

| RAiD `relatedObject.schemaUri` | DataCite `relatedIdentifierType` |
|---|---|
| `https://arks.org/` | `ARK` |
| `https://doi.org/` | `DOI` |
| `https://www.isbn-international.org/` | `ISBN` |
| `https://scicrunch.org/resolver/` | `RRID` |
| `https://web.archive.org/` | `URL` |
| `https://hdl.handle.net/` | `Handle` |

### Related object category → relationType (`OBJECT_RELATION_TYPE_MAP`)

| RAiD `relatedObject.category.id` | DataCite `relationType` |
|---|---|
| `…/191` | `References` |
| `…/190` | `IsReferencedBy` |
| `…/192` | `IsSupplementedBy` |

Note: only `category[0]` is read, so a multi-category related object maps by its
first category only.

### Related object type → resourceTypeGeneral (`RESOURCE_TYPE_MAP`)

Maps `relatedObject.type.schema/247…274` to DataCite general types. Full map
(from the factory): 247→OutputManagementPlan, 248→Text, 249→Workflow,
250→JournalArticle, 251→Standard, 252→Report, 253→Dissertation, 254→Preprint,
255→DataPaper, 256→ComputationalNotebook, 257→Image, 258→Book, 259→Software,
260→Event, 261→Sound, 262→ConferenceProceeding, 263→Model, 264→ConferencePaper,
265→Text, 266→Instrument, 267→Other, 268→Other, 269→Dataset,
270→PhysicalObject, 271→BookChapter, 272→Other, 273→Audiovisual, 274→Service.

### Related RAiD type → relationType (`RAID_RELATION_TYPE_MAP`)

| RAiD `relatedRaid.type.id` | DataCite `relationType` |
|---|---|
| `…/204` | `Continues` |
| `…/203` | `IsContinuedBy` |
| `…/202` | `IsPartOf` |
| `…/201` | `HasPart` |
| `…/200` | `IsDerivedFrom` |
| `…/199` | `IsSourceOf` |
| `…/198` | `Obsoletes` |
| `…/205` | `IsObsoletedBy` |

## Declarable vs imperative

**(a) Pure structural / vocabulary mapping — declarable.** Everything that is a
constant, a field rename, a concatenation, or a finite code→value lookup:

- `types` (constant), `dates` (concat + constant `dateType`), the title /
  description / organisation-role / related-object / related-raid vocabulary
  maps, `alternateIdentifierType` constants, `relationType`/`resourceTypeGeneral`
  constants on the alternate-URL and related-RAiD paths, `prefix` derivation,
  `doi`, and the identifier-type/scheme assignments.
- These are candidates for LinkML `exact_mappings` on the RAiD slots plus a
  `linkml-map` transformation spec; the vocabulary maps specifically are a
  natural SSSOM mapping set.

**(b) Imperative floor — cannot be expressed declaratively.** These require
runtime logic or live network I/O and must stay in code:

- **Live name resolution.** `creators[].name` (ORCID/ISNI via `OrcidClient`/
  `IsniClient`), `contributors[].name` and `fundingReferences[].funderName` and
  `publisher.name` (ROR via `RorClient`). Each is an outbound HTTP call whose
  result is not present in the RAiD record; declarative mapping cannot produce it.
- **The organisation split.** One RAiD `organisation[]` list is partitioned into
  `contributors` vs `fundingReferences` by inspecting roles for the Funder id
  (`…/186`); an org with mixed roles appears in both branches. This is
  set-partition logic driven by predicate evaluation.
- **"Latest role" selection.** `contributorType` uses the non-Funder role with
  the maximum `startDate` — a sort/reduce over a collection.
- **Primary-title extraction and re-ordering** (`filter … findFirst …
  orElseThrow`, then the rest), and the `relatedObject` exclusion predicate
  (category `…/191` AND type `…/272`).
- **Conditional constants** derived from data: `event` from access type;
  empty-creator fallback to a single empty object.
- **`publicationYear`** currently `Year.now()` (not from the record at all).

## Known issues found in the code

1. **`publicationYear` is hardcoded to the current year.**
   `setPublicationYear(String.valueOf(java.time.Year.now()))` in all three
   overloads, with an inline `// TODO: year of start date`. It should almost
   certainly derive from `date.startDate`. This means an update in a later year
   silently rewrites the DOI's publication year.

2. **`rightsList` is never emitted.** `DataciteAttributesDto` has a `rightsList`
   field and `DataciteRightFactory.create(Access, Id)` exists and is even
   injected into `DataciteAttributesDtoFactory` (`private final
   DataciteRightFactory dataciteRightFactory;`), but it is **never called** and
   `setRightsList(...)` is never invoked. RAiD's CC-0 licence (`identifier.license`)
   therefore does not reach DataCite. (Also, `DataciteRight` serialises
   `rightsURI` as JSON key `rightsUri`, which differs from DataCite's
   `rightsUri`/`rightsURI` expectations — moot while unwired, but worth noting.)

3. **`relatedRaid` is emitted with `relatedIdentifierType = "DOI"` on `main`,
   which 4.7 makes correctable.** RAiDs are handles. Until 4.6 the DataCite
   `relatedIdentifierType` enumeration had no `RAiD` value, so `main` uses `DOI`
   as a pragmatic stand-in. **DataCite 4.7 (published 2026-03-03) adds `RAiD`
   as a native `relatedIdentifierType`** (verified in
   `datacite-relatedIdentifierType-v4.xsd` at kernel-4.7: `RAiD` and `SWHID`
   were added over 4.6), so `datacite.yaml` here includes it. RAID-797 (PR #617,
   in review) changes this same `DataciteRelatedIdentifierFactory` RelatedRaid
   overload to emit `RAiD` instead of `DOI` and adds `RAiD` to the app's
   `vocabularies/datacite/RelatedIdentifierType` enum, backed by a gated live
   DataCite test. Note the sequencing: RAID-797 emitted `RAiD` before it was in
   the published XSD, relying on the REST JSON API accepting it ahead of the XML
   schema (a historical XML-XSD-vs-REST-JSON lag); 4.7 has since closed that gap.
   Once PR #617 merges, this row's `DOI` becomes `RAiD`.

4. **`alternateIdentifier.type` is discarded.** RAiD's free-text alternate
   identifier type is replaced by the constant `"URL"`.

5. **Only `relatedObject.category[0]`** is used to choose the relationType; other
   categories are ignored.

6. **Unhandled-key risk.** The vocabulary maps use `Map.get(...)`; a RAiD vocab
   value with no entry yields a `null` DataCite value rather than a validation
   error. `DataciteCreatorFactory` does throw on an unknown contributor schema.

## Feasibility verdict

Roughly the vocabulary and structural layer — the title/description/role/
relation/resource-type lookups, the constant `types`/`dateType`, the identifier
renames and concatenations — is genuinely declarable and would map well to
LinkML `exact_mappings` plus a `linkml-map` spec, with the code→enum lookups
extracted as SSSOM tables. But the payload cannot be produced declaratively
end to end: every human/organisation **name** is fetched live from ORCID, ISNI,
or ROR at emission time, and the organisation list is split into contributors
versus funding references (and a "latest role" chosen) by imperative predicate
logic. Those two things are the irreducible imperative floor.

Recommendation: treat this as a hybrid. Externalise the vocabulary crosswalks
as declarative mapping tables (SSSOM / LinkML `exact_mappings`) so they are
reviewable and testable in isolation and reused by any future crosswalk, but
keep name resolution and the organisation-partition/role-selection logic in a
thin imperative adapter that the declarative layer feeds into. Before doing any
of that, fix the two correctness bugs (hardcoded `publicationYear`, unwired
`rightsList`), since a declarative rewrite would otherwise faithfully preserve
them.
