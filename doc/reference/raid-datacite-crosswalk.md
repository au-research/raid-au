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

## Proof of concept: the declarable subset as a `linkml-map` spec

The "declarable" half of this verdict is not just asserted; it was proven by a
runnable `linkml-map` (LinkML Transformer) specification that transforms a real
`RaidDto` into a DataCite 4.7-shaped instance. See
`doc/spike/RAID-831-datacite-linkml-map-poc/` for the spec
(`raid-to-datacite.transform.yaml`), a sample input, the real output, and run
instructions. It configures the constant `types`, the `titles`/`descriptions`
structure with their controlled-vocabulary lookups (as `enum_derivations`), and
the `dates` start/end concatenation, and runs (exit 0) against the real
`api-svc/datamodel/src/v2/raid-core.yaml`. The imperative floor above (live PID
name resolution, the organisation split, the latest-role reduce) confirmably
cannot be expressed: `linkml-map`'s only extension hook, `@safe_function`,
requires functions be pure and free of I/O.

## Can the mapping be *done by configuring LinkML*? (the execution model)

The endgame of a LinkML-driven crosswalk is to define the mapping as
configuration that an engine executes, with no bespoke transformation code. In
this Java-in-application architecture that endgame is **not reachable**, for
reasons of two different kinds.

### The execution boundary

`linkml-map` is a Python tool; its `compile` target is Python; there is no
in-JVM engine that can execute a `linkml-map` transformation spec at request
time. So even the parts that *are* declaratively expressible (structure, vocab
lookups, constants, concatenation) cannot be *executed from config* inside the
Spring API. Java code must perform the transformation. Configuration can *inform*
that Java code (a vocabulary crosswalk loaded as a data table), but it cannot
*drive* it. This mirrors how the repo already uses LinkML: `raid-core.yaml` is
never executed by the running app; it is run offline (Docker `linkml/linkml`) at
build time to generate committed artifacts (`raid-jsonschema.json` → OpenAPI →
Java models) that Java consumes. The DataCite crosswalk would follow the same
build-time-artifact pattern, not a runtime engine.

### Keeping the factories vs config-driven mapping

Because the transform cannot execute in the JVM, and because of the imperative
floor, the DataCite emission factories under
`api-svc/raid-api/.../factory/datacite/` must stay. That is precisely what "the
mapping is *not* config-driven" looks like: the transformation lives in code. The
most configuration can do while the factories remain is supply the vocabulary
*decisions* as a governed, single-sourced table the factories read (replacing
the hardcoded `Map.of(...)` in the title/description/related-identifier/
contributor factories). That is **config-informed** mapping, not
**config-driven** mapping.

### Would porting `linkml-map` to Java change the answer?

No, and it is not worth attempting. Measured against the installed 0.5.3:

- The transform engine you would need for `map-data` is only ~3–4k LOC
  (`object_transformer.py`, `transformer.py`, `engine.py`, plus eval/session/
  loaders) — the tractable part.
- It stands on ~20k LOC of `linkml_runtime` (SchemaView, the metamodel, schema
  and import loading, class/slot/enum/range/inlining introspection), for which
  there is no mature Java equivalent. Re-creating that slice is the dominant cost.
- `expr` strings are evaluated with Python semantics (`simpleeval`/`asteval`);
  reproducing that faithfully in a JVM evaluator is a subtle, bug-prone
  compatibility surface.
- `linkml-map` is pre-1.0 (0.5.x) and evolving, so a port is a permanent
  maintenance commitment tracking a moving upstream in a second language.

Decisively, even a flawless port would only let the *declarable* part execute
from config in-JVM; it still could not cross the imperative floor (live name
resolution). So the large investment would move the codebase from "Java factories
that read a config table" to "a Java engine that executes a config spec" — a
near-identical end state, with the imperative adapter present in both.

### Conclusion

*This section is about executing a mapping spec **at runtime, in the running
Spring application**. A third option — generating the Java mapping code from the
schema at **build time**, with a generator we write ourselves — is materially
different and is assessed in the next section. Read the two together: the
conclusion below is not the last word.*

The config-driven-mapping endgame is not achievable for RAiD → DataCite in-app,
whether the factories are kept (the mapping stays in code) or `linkml-map` is
ported (a large, ongoing build that still cannot do name resolution). The
realistic and worthwhile win from LinkML here is a **governed, single-sourced
vocabulary crosswalk** — authored/validated in LinkML offline and exported to a
data table the existing factories consume — not the elimination of the
transformation code. If the strategic goal is strictly config-driven mapping, it
would additionally require denormalising resolved PID names into the RAiD record
(so emission needs no live lookup) plus a JVM transform engine, a scope far
beyond this work.

## Hand-rolling a generator: LinkML in, Java mapping code out

The previous section rules out *interpreting* a mapping spec at request time. It
does not rule out **generating the mapping code from the schema at build time
with a generator we write ourselves**. That is a genuinely different proposition,
and the assessment is different: **yes, this is feasible, and it is the cheapest
credible route to a metadata-expert-maintainable crosswalk.** It is a natural
extension of a pattern this repository already runs in production, not a new
capability.

### Why the objections to porting `linkml-map` do not apply here

The port was rejected on three grounds. A hand-rolled build-time generator
avoids all three:

- **"It stands on ~20k LOC of `linkml_runtime`."** Only if the generator has to
  consume arbitrary LinkML with full metamodel introspection. It does not. The
  generator reads *our* crosswalk document, whose dialect we define, plus the two
  schemas we control. `buildSrc` already parses LinkML YAML directly with
  snakeyaml and Jackson (`Utils.loadDynamicEnums`) without any LinkML runtime at
  all.
- **"`expr` strings need Python evaluation semantics."** Only if we adopt
  `linkml-map`'s expression language. We should not. Support a small, fixed,
  closed set of declarative constructs — constant, field rename, dotted source
  path, controlled-vocabulary lookup, concatenation with a separator, and
  single→list coercion — which is demonstrably enough for the declarable subset
  (that is exactly what the PoC spec uses). No general expression evaluator means
  no compatibility surface, and it is the difference between a bounded generator
  and an open-ended one.
- **"Tracking a moving pre-1.0 upstream in a second language."** Does not arise:
  nothing upstream is being tracked. The dialect is ours and changes only when we
  change it. If we want interoperability with `linkml-map`'s spec format we can
  read the subset we support and **fail the build loudly** on any construct we do
  not, which is the safe direction to fail in.

### The precedent already exists in this repo

This is the part that makes the estimate credible. `buildSrc/` contains
hand-written Gradle tasks that already do LinkML-in / artifact-out, in the JVM,
with no Python and no Docker:

- `AddStaticEnums` (93 LOC) reads LinkML enum definitions, resolves dynamic
  enumerations via SPARQL with an on-disk cache, and rewrites the generated JSON
  Schema with materialised enum values.
- `GenerateReferenceDataTask` (116 LOC) reads `core-enums.yaml` and emits
  `referencedata.sql`.
- `Utils` (265 LOC) is the shared LinkML-YAML parsing, SPARQL querying and
  caching layer they sit on.

And generated Java is already a normal part of this build: `openApiGenerate`
emits the entire `au.org.raid.idl.raidv2.model` package, and `compileJava`
depends on it. So "generated Java mapping classes compiled into the API" is not
a new architectural idea here; it is the existing pipeline with one more stage.

A pure-JVM generator is also strictly *better placed* than the existing
`linkml/linkml` Docker tasks: CodeBuild has no Docker, which is why the LinkML
outputs are committed to `generated/` and excluded from `clean`. A `buildSrc`
generator needs no Docker, so it can run on every CI build like
`openApiGenerate` does, rather than relying on a developer to regenerate and
commit.

### What it would and would not generate

Generate the declarable subset:

- **The vocabulary lookup tables.** The six `Map.of(...)` blocks in the title,
  description, contributor and related-identifier factories become generated
  constants derived from one governed crosswalk table.
- **The simple field mappings.** Constants (`types`, `dateType`), renames,
  dotted-path reads, the date concatenation, the identifier/scheme assignments.

Hand-write, and keep hand-written, the imperative floor: live PID name
resolution, the organisation partition into contributors versus funding
references, the latest-role reduce, the primary-title extraction, and the
`relatedObject` exclusion predicate.

The composition pattern matters, because this is where partial generation
usually goes wrong. Make the floor **explicit in the crosswalk document**: a
target field the dialect cannot express is declared as delegating to a named
collaborator, and the generator emits a call to it. Then a missing or
wrongly-shaped hand-written collaborator is a **compile error**, not a silently
unmapped field. The document stays the complete statement of the mapping —
including an honest inventory of which parts are not declarative — which is what
makes it reviewable by a metadata expert even though it does not express
everything.

### The new capability this buys (not available today or from `linkml-map`)

A generator can **fail the build**. That directly fixes Known issue 6: today an
unmapped RAiD vocabulary value yields `Map.get(...)` → `null` and a silently
degraded DataCite record. A generator can assert that every value of a RAiD
vocabulary has a DataCite mapping, and that every mapped target is a member of
the DataCite enum for the targeted kernel version, and refuse to generate
otherwise. Note `linkml-map` gives the *opposite* behaviour: an unmapped source
permissible value yields null and the target slot is simply omitted. So the
generator is not merely a reimplementation — the build-time exhaustiveness check
is the strongest single argument for this route.

It also makes the (RAiD version, DataCite version) pairing above concrete and
enforceable: the generator takes the pair as input and can refuse to build a
crosswalk against a DataCite kernel it has no schema for.

### Cost, honestly

Estimate, not a measurement: the generator itself is comparable in scale to the
existing `buildSrc` tasks, since it does the same kind of work — parse YAML we
control, validate it against two schemas, emit text from a template. On the
order of a few hundred lines plus tests, with the emission templates and the
exhaustiveness validation as the substantive parts. The larger and less
predictable cost is not the generator, it is **authoring the crosswalk document
and refactoring fifteen factories to consume generated code without changing
behaviour** — a refactor whose regression guard is the existing
`Datacite*FactoryTest` suite staying green.

Risks worth stating plainly:

- **Dialect creep.** The whole case rests on the dialect staying small and
  closed. The first time someone wants a conditional, the pressure is to add an
  expression language, and at that point the port objections come back. The
  delegation escape hatch above exists precisely so the answer to "we need
  logic here" is "hand-write a collaborator", not "extend the dialect".
- **Two places to look.** A reader chasing a mapping has a document, generated
  code, and a hand-written adapter. Mitigated by the document being complete
  (delegations included) and the generated code being clearly marked and never
  edited.
- **It does not remove the imperative floor**, and it does not detect that
  DataCite shipped a new kernel. Neither does anything else here.

### Verdict

Feasible, in-pattern, and worth doing if the governance goal is being taken
seriously — but note the honest sequencing. The vocabulary tables are where
essentially all the maintainability benefit sits, and they can be externalised as
a governed data table the current factories *read* with no generator at all.
**Recommendation: externalise the vocabulary crosswalk first, with the
build-time exhaustiveness check; add code generation only if the field-level
mapping subsequently proves to be a real maintenance burden.** The generator is
the right destination and there is no architectural obstacle to it. It is simply
not the first step, because the step before it delivers most of the value and is
a fraction of the work.

## Where identifier and vocabulary resolution should happen

DataCite wants human-readable text (`creators[].name`, `contributors[].name`,
`fundingReferences[].funderName`, `publisher.name`) wherever RAiD holds only a
PID. Today that text is fetched *during* emission, inside the factories:
`DataciteCreatorFactory` calls `OrcidClient`/`IsniClient`, and
`DatacitePublisherFactory`/`DataciteContributorFactory`/
`DataciteFundingReferenceFactory` call `RorClient`. Resolution is hardcoded to
those three schemes with no fallback — `DataciteCreatorFactory` throws
`Unsupported contributor schema` for anything else. ANZSRC FoR/SEO label
resolution and GeoNames/OSM place-name resolution have no resolver at all, which
is one reason `subject` and `spatialCoverage` are not emitted.

**Recommendation: resolution is an enrichment step that runs *before* the
crosswalk, not a capability the crosswalk spec invokes.** Reasons:

- It is the only arrangement in which the transformation is a pure, deterministic
  function of its input. That is what makes the crosswalk testable from fixtures
  with no network, diffable between versions, and — for the declarable subset —
  expressible in `linkml-map` at all, whose only extension hook
  (`@safe_function`) forbids I/O by design.
- It puts caching, timeouts, retries and resolver-unavailable handling in one
  place instead of scattered through fifteen factories. RAiD already has the
  precedent: RAID-809 standardised resolver-unavailable to a 503 across
  validators.
- It makes an unresolvable name a *policy* decision at one boundary rather than a
  `RuntimeException` from deep inside emission. That boundary is where DataCite
  Appendix 3's unknown-value codes belong (below).

Concretely: an enrichment pass walks the `RaidDto`, resolves every PID it can to
a display name, and produces a resolved-names side table keyed by identifier; the
crosswalk then reads names from that table. Adding a fourth scheme becomes a
resolver registration, not a change to the emission code.

### Unknown values (DataCite Appendix 3)

Where a required DataCite field genuinely cannot be populated, DataCite's own
standard is to emit an unknown-value code rather than omit the field or send a
non-conformant record: `:unav` (value unavailable, possibly unknown), `:unas`
(value unassigned), `:unac` (temporarily inaccessible), `:tba`, `:none`. The
enrichment boundary is the natural place to apply them — a name that fails to
resolve becomes `:unav` and the record stays valid and mintable, instead of the
current behaviour where an unsupported scheme aborts the whole emission. This
should be an explicit decision for the team: **fail the mint, or mint a
conformant record carrying `:unav` and flag it for re-sync?** The re-sync
mechanism from RAID-832 makes the second option safe, because a record minted
with `:unav` can be re-pushed once resolution succeeds.

### Richness gaps

Independently of the declarative question, the mapping currently drops metadata
DataCite has homes for: `subject` → `subjects` (needs FoR/SEO label resolution),
`spatialCoverage` → `geoLocations` (needs GeoNames/OSM place-name resolution),
`identifier.license` → `rightsList` (the factory exists but is unwired — see
Known issues), and `access.statement`/`access.embargoExpiry`, which have no clean
DataCite equivalent and are reasonable to keep dropping. These are ordinary
mapping work under either execution model; they do not depend on the LinkML
decision.

## Versioning the crosswalk, and re-syncing when either side changes

RAiD's schema and DataCite's schema move independently, so a single crosswalk
document edited in place to track "current on both sides" loses the ability to
say what a given already-minted DOI was produced from.

**Recommendation: version the crosswalk per (RAiD schema version, DataCite
schema version) pair.** In practice that means the crosswalk artifact is named
and stored for the pair it targets — `raid-core v2` → `datacite 4.7` — alongside
the LinkML models it references, exactly as `api-svc/datamodel/src/v2/` already
versions the RAiD model by directory. A new DataCite kernel is a *new* crosswalk
document derived from the previous one, not an edit to it. The benefits are
practical rather than theoretical: the diff between two crosswalk versions is the
reviewable statement of what changed for a metadata expert, and it is possible to
answer "which crosswalk produced this DOI's current metadata".

Only the newest crosswalk is *executed*. Superseded versions are kept for
provenance and diffing, not for replay — RAiD pushes a full-document PUT, so
there is never a need to re-run an old crosswalk.

The re-sync path already exists and this plugs straight into it. RAID-832
(Ready to Deploy) added a `datacite_resync_required` flag on the `raid` table,
cleared on every successful DataCite post, drained by a scheduled worker that
takes a Postgres advisory lock, batches, throttles to roughly one request per
second, and retries on failure. So the answer to "DataCite shipped a schema
change, what about the RAiDs already minted?" is: cut the new crosswalk version,
flag the affected records, and let the worker drain them. No bespoke backfill
script per correction — that is precisely what RAID-832 was built to retire
(`scripts/backfill-datacite-related-raids.sh`, the one-off written for RAID-797,
is now a fallback only).

The one thing still missing is the *selection* step: deciding which records a
given crosswalk change affects. For a change that touches every record (a new
constant, a changed `resourceTypeGeneral`) that is "flag everything"; for a
narrow change (RAID-797 touched only RAiDs with a `relatedRaid`) it is a
predicate over the data. Worth an explicit operator entry point — flag by
predicate — rather than ad-hoc SQL each time.

## Relationship to RAID-776 and RPDB-112

**Same tooling, different mechanisms — and this spike narrows RAID-776's
recommendation.**

RAID-776 asked how one codebase can support per-registration-agency customised
metadata schemas, and recommended LinkML as the single declarative source of
truth "generating as much as possible (schema, constraints, JSON-LD/RDF, and —
pending the DataCite PoC — mappings)", with compiled SPI modules as the escape
hatch for imperative logic. It explicitly flagged the DataCite mapping as the
uncertain part and scoped this spike as the PoC that gates the
"mappings as config" claim.

This spike is that PoC, and the verdict is the narrowing one: **the
schema.org/RDF side of RAID-776's claim stands; the DataCite side does not.**
schema.org mapping is vocabulary annotation, which LinkML does natively
(`class_uri`, `slot_uri`, `exact_mappings`) and the build already emits. DataCite
is a structural transformation to a different target schema, and it hits both an
imperative floor (live name resolution, the organisation partition) and an
execution boundary (`linkml-map` is Python; nothing executes a transform spec
in-JVM). So DataCite mapping is not the same mechanism as the RAiD schema
pipeline — it shares the LinkML *toolset* and the build-time-artifact *pattern*,
but the mapping itself stays in Java.

Two consequences worth carrying back to RAID-776:

- Its "declarative complexity cliff" risk is now measured, not hypothetical, and
  the cliff is where it predicted: external I/O.
- Its framing of DataCite as consuming only the compiled core is unaffected by
  this spike. Nothing here depends on whether per-RA extensions exist; if they
  later do and an RA wants them in DataCite, that is a new crosswalk version for
  the pair, which is what the versioning scheme above is for.

RPDB-112 (efficient management of metadata schema versions) is about reducing the
manual burden of rolling out new *RAiD* schema versions, and mostly
cross-references RAID-776. The crosswalk versioning above is a direct instance of
the same problem, so RPDB-112 should treat the (RAiD version, DataCite version)
pair as one of the artifacts a schema rollout has to produce. It is not a
separate mechanism to build.

## Effort and maintenance cost versus continuing bespoke mapping

The honest comparison, using RAID-797 as the worked example, because it is
exactly the shape of change this spike is meant to make cheaper: DataCite added a
native `RAiD` `relatedIdentifierType` in 4.6/4.7, and RAiD was emitting related
RAiDs as `DOI`.

**What RAID-797 actually cost under bespoke mapping.** Two production lines: one
enum constant added to `RelatedIdentifierType`, one map value changed in
`DataciteRelatedIdentifierFactory`. Around that: eight existing factory unit
tests updated plus a casing guard, one gated live DataCite test-API regression
test, and a backfill script for already-minted records (since retired by
RAID-832's re-sync worker). The mapping edit itself was trivial. The expensive
parts were **noticing** that DataCite had changed, and **re-pushing** the
already-minted records.

**What it would cost under a declarative vocabulary crosswalk.** One row changed
in a governed mapping table, no Java edit, no recompile of mapping logic, and the
same tests. The saving on the edit is real but small — one line either way. The
change in *who can make it* is the actual difference: a metadata expert can edit
and review a mapping table; changing `Map.of(...)` inside a factory requires a
developer.

**What it would not fix.** Neither approach notices that DataCite shipped 4.7.
That is a monitoring problem, not a mapping-representation problem, and it is
worth naming plainly because it was the root cause of the RAID-797 gap the ticket
cites as the motivating example. The re-push, likewise, is solved by RAID-832
regardless of how the mapping is expressed.

So the cost/benefit:

| | Bespoke Java factories (today) | Declarative vocab tables + thin adapter (recommended first step) | Hand-rolled build-time generator | Runtime config-driven engine |
|---|---|---|---|---|
| Build cost | zero (exists) | small: extract ~6 `Map.of` tables to a governed artifact, load them, keep the factories | moderate: a `buildSrc` generator in the existing pattern, plus authoring the crosswalk and refactoring 15 factories onto generated code | very large: JVM transform engine (~3–4k LOC on top of re-creating ~20k LOC of `linkml_runtime`), plus denormalising resolved names into the record |
| A RAID-797-shaped vocab change | 1 dev line + tests | 1 table row + tests, editable by a metadata expert | 1 table row + tests, editable by a metadata expert | 1 spec line + tests |
| A structural change (new field, new split rule) | Java | Java | crosswalk document, if it stays above the imperative floor | spec, if it stays above the imperative floor |
| Imperative floor (name resolution, org split) | Java | Java | still Java (explicit delegation) | still Java |
| Unmapped vocab value | silent `null` | build failure | build failure | silently omitted slot |
| Needs Docker in CI | no | no | no | no |
| Detects an upstream DataCite release | no | no | no | no |

**Recommendation: the second column first, the third as its destination.** The
vocabulary tables carry essentially all of the governance benefit the ticket asks
for — a crosswalk a metadata expert can maintain without developer involvement —
at a small, bounded cost, and they bring the build-time exhaustiveness check with
them. Code generation is a sound next step on the same path (see the hand-rolled
generator section) and should be taken if field-level mapping proves to be a real
maintenance burden, but it is not where to start. The fourth column buys a
near-identical end state for a large and permanent maintenance commitment
tracking a pre-1.0 upstream in a second language, and is not recommended.

Two things should happen before or alongside it: fix the two correctness bugs
(hardcoded `publicationYear`, unwired `rightsList`), since a table-driven rewrite
would faithfully preserve them; and add a deliberate watch on DataCite schema
releases, since that is the gap that actually caused RAID-797.

## How a LinkML-driven crosswalk would be tested

The spike ships no code, but the testing shape should be settled before any of it
is built. Four layers, all of which have existing precedent in the repo:

1. **Vocabulary table coverage (new).** For each RAiD vocabulary that feeds a
   DataCite enum, assert every RAiD value has a mapping and every mapped target
   is a member of the DataCite enum for the targeted kernel version. This is the
   test that does not exist today and is the direct fix for the unhandled-key risk
   in Known issues, where `Map.get(...)` silently yields `null` for an unmapped
   value. It is a data-driven test over the table, so it stays correct as the
   table grows.
2. **Factory unit tests (existing, unchanged).** The fifteen
   `Datacite*FactoryTest` classes already cover the transformation per field with
   mocked resolvers. A table-driven refactor must leave them green; that is the
   regression guard for the extraction itself.
3. **Whole-payload golden files (partly existing).** `DataciteDtoFactoryTest` and
   `DataciteRequestFactoryTest` cover assembly. Extend to a fixture-in,
   JSON-payload-out comparison per crosswalk version, so a crosswalk diff shows
   as a payload diff. This is what makes a new (RAiD, DataCite) version pair
   reviewable.
4. **Gated live DataCite test-API check (existing pattern).**
   `DataciteLiveRelatedRaidIntegrationTest` is self-cleaning and skipped unless
   `DATACITE_LIVE_TEST=true` (`@EnabledIfEnvironmentVariable`). Credentials come
   from a real service point. Any crosswalk change that touches a DataCite
   vocabulary should get one such case, because it is the only layer that proves
   DataCite *accepts* the value; the local mocked equivalent
   (`DataciteRelatedRaidMockIntegrationTest`) covers the wiring in CI.

Because resolution moves to an enrichment step (above), all of layers 1–3 run
with no network at all: the crosswalk under test is a pure function of a fixture
plus a resolved-names table. That is the main testability argument for the
enrichment-first design.
