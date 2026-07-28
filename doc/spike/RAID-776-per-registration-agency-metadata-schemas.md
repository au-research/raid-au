# RAID-776 Spike: Supporting per-registration-agency customised metadata schemas

**Type:** Spike (research / investigation)
**Status:** Draft for discussion
**Author:** Rob Leney
**Date:** 2026-07-29

## The question

> How can a single codebase support an arbitrary number (at most the number of
> registration agencies) of customised metadata schemas?

Seed idea (Matthias): *a metadata schema registry where each RA maintains their
own extended/local components, and that registry builds the artefacts that the
RAiD API pulls in at runtime.*

## TL;DR

- The RAiD API today assumes **exactly one metadata schema, fixed at build
  time**. There is no runtime schema resolution, no per-agency branching, and
  no runtime schema-validation engine.
- Supporting truly *arbitrary, divergent* per-RA schemas would fight RAiD's core
  purpose: a **shared, interoperable, harvestable standard** (ISO 23527). Full
  divergence breaks cross-RA interoperability and the hand-coded DataCite/RDF
  mappings.
- The defensible target is a **common, strongly-typed core + governed,
  namespaced extension blocks** that each RA registers. The core stays
  compiled and guaranteed; extensions are validated at runtime against the RA's
  registered schema and stored/serialised generically.
- **A registry can only ever carry declarative constraints** (types, patterns,
  enums, cardinality). Business logic — the ~30 Java validators, external
  identifier resolution, computed rules — is imperative code and cannot be
  loaded from a registry. To the core, an extension is opaque JSON gated by one
  declarative validation pass; the core never interprets it, so core behaviour
  is unchanged. Global integrators (DataCite) consume the core only; extensions
  serve RA-local integrators. See "What can and cannot be loaded at runtime".
- This is Matthias's registry idea, scoped: it governs *extensions*, not the
  *core*. But "registry" mostly means a **build-time artefact registry**
  (CodeArtifact, as already used for `@raid-org`/`@raid-au`) plus **external
  runtime config** — not a live schema service. Per-RA behaviour (LinkML model +
  `Validator`/mapper SPI implementations) ships as build-time Gradle
  dependencies; config keyed by `registrationAgencyOrganisationId` selects what
  is active. A *live* registry earns its place only for the optional
  declarative-only tier. Recommended direction is **Option E** below.
- **Schema ownership is the registration agency's, not the service point's.**
  Service points have no direct control; a service point raises a requirement to
  its RA, and the RA decides — in consultation with its service points — whether
  to adopt it. This is expected to be a **slow, deliberate governance process**.
  Because change cadence is low, the pressure for *runtime, no-redeploy* schema
  loading is weak: a **build-time / per-release** mechanism (Option A) is likely
  adequate for a long time, with the runtime registry (Option B engine) a later
  step. The hard problem is **governance**, not runtime plumbing.
- **LinkML-centric direction (Matthias's preference):** rather than hand-coding,
  make LinkML the single source of truth and *generate* schema, validation
  constraints, JSON-LD/RDF context and — as far as feasible — the external
  mappings. This is **more viable than a "generic store" framing suggests**:
  LinkML *is* an interoperable-contract language and already drives much of the
  build. It converges with Option E rather than competing: LinkML generation is
  the declarative backbone; compiled SPI modules remain the escape hatch only
  where imperative logic (I/O, stateful rules) is unavoidable. Two open forks:
  (a) generate at **build time** vs (b) interpret LinkML/transforms at
  **runtime**; and whether **DataCite** mapping can be config-driven (needs a
  PoC — see follow-ups). schema.org/RDF is LinkML's sweet spot and almost
  certainly can. See "Alternatives considered" and "Mapping the external formats".

## Current architecture (grounded in the code)

All paths relative to `raid-au`.

### Schema is baked in at build time
1. LinkML source of truth: `api-svc/datamodel/src/v2/` — `raid-core.yaml`,
   `raid-extended.yaml`, `shared.yaml`, `core-enums.yaml`, `extended-enums.yaml`,
   `researchproject.yaml` (schema.org / JSON-LD shape).
2. `datamodel/build.gradle`: `gen-json-schema` (Docker/LinkML) → non-strict
   schema; `AddStaticEnums` (buildSrc) resolves dynamic SPARQL enums into a
   **strict** schema. Three artefacts are committed because CI has no
   Docker/LinkML: `raid-jsonschema.json`, `raid-strict-jsonschema.json`,
   `researchproject.json`.
3. `idl-raid-v2/build.gradle`: `AssembleOpenAPI` merges the template +
   JSON Schemas → `raid-openapi-strict-3.1.yaml`; `openApiGenerate` (spring,
   bean validation) generates the Java model/interfaces from the **strict**
   spec into `src/main/generated/`.

Consequence: the metadata contract, including every `@Pattern` and typed enum,
is a **single compiled artefact**. Adding/altering a schema = code change +
build + release.

### Runtime validation is global and dual-track (no schema engine)
- Bean validation via generated `@NotNull/@Pattern/@Valid` on the DTOs.
- ~30 hand-written validators orchestrated by
  `raid-api/.../validator/ValidationService.java`, called from
  `RaidController` (`validateForCreate/Update/Patch`).
- Allowed vocab URIs hardcoded in `util/SchemaValues.java` — a **second source
  of truth** parallel to the LinkML enums.
- **No JSON Schema / SHACL validation library is present at runtime** (verified:
  no networknt/everit/JsonSchemaFactory). Any runtime-schema approach must add
  one.

### Registration agency vs service point
- `RegistrationAgency` metadata block on `Id` (ROR `id` + `schemaUri`) = "who
  minted this RAiD".
- At runtime the RA is effectively a **global singleton** from
  `config/properties/IdentifierProperties.java`, stamped on every raid in
  `IdService`/`IdFactory`.
- **But** `registrationAgencyOrganisationId` is already **persisted per-raid**
  (`RaidRecordFactory`, `RaidIngestService`). This is the natural key for
  per-RA schema resolution.
- **Service point** is the real tenancy/authorisation unit
  (`Owner.servicePoint`, service-point-scoped auth). No schema concept is
  attached to it today.

**Ownership decision (confirmed):** a customised schema belongs to the
**registration agency**, never to the service point. A service point cannot
change the schema; it raises a requirement to its RA, which decides in
consultation with its service points. This is expected to be a slow, deliberate
process — so schema resolution keys off `registrationAgencyOrganisationId`, and
low change frequency is a design assumption, not just a hope.

### Serialisation is hand-coded, not schema-driven
- Default JSON: Jackson over the generated `RaidDto`.
- JSON-LD (schema.org): `converter/RaidJsonLdConverter.java` →
  `service/rdf/RaidRdfService.java` (Apache Jena, hardcoded namespaces).
- DataCite: `service/datacite/DataciteService.java` + ~15 hand-written
  factories.
- The LinkML-derived JSON-LD context / SHACL / OWL exist as vocabulary/doc
  outputs but are **not wired into the request path**.

**Key finding:** unknown/arbitrary metadata cannot be hand-mapped to DataCite or
the schema.org RDF shape. Any per-RA extension mechanism must decide what
happens to extension data in each serialisation format.

## Options

### Option A — Additive namespaced extensions, compiled at build time
Extend the existing `in_subset: extended` LinkML pattern. Each RA contributes an
extension LinkML file; all are compiled into the one codebase. RA-specific
blocks are optional and validated conditionally on the raid's RA.

- **Pros:** smallest change; reuses the whole existing toolchain; core stays
  typed; no runtime schema engine needed.
- **Cons:** not "arbitrary" — every new/changed RA schema needs a build +
  release; doesn't scale to many independently-governed agencies; the committed
  strict-schema/CI constraint bites harder as N grows.
- **Fit:** good stop-gap / phase 1; does **not** by itself satisfy "arbitrary
  number ... at runtime".

### Option B — Runtime schema registry + generic validation engine (Matthias's idea, literal)
Store extension metadata generically (JSON), add a runtime JSON-Schema (or
SHACL) engine, and validate each raid's extensions against the RA's schema
fetched/cached from a registry at runtime. Registry builds versioned artefacts
the API pulls in.

- **Pros:** true "arbitrary, no redeploy"; clean separation of governance
  (registry) from enforcement (API); directly realises the seed idea.
- **Cons:** large build; needs a validation engine (currently none), a generic
  persistence model, schema caching/versioning, and a governance/publishing
  pipeline; **serialisation problem is unsolved** (extensions can't be
  hand-mapped to DataCite/RDF); risk to interoperability if applied to the core.
- **Fit:** right long-term engine, but should govern **extensions only**, not
  the core.

### Option C — N compiled contracts (one DTO set/endpoint per RA)
Generate N typed models/endpoints, one per RA schema.

- **Rejected:** combinatorial code bloat; can't serve N typed contracts cleanly
  on one path; doesn't scale to "arbitrary"; nightmare to maintain.

### Option D — Typed common core + runtime-validated namespaced extension envelope (recommended)
Synthesis of A's discipline and B's engine:

- **Core** RAiD metadata stays strongly typed and compiled — the
  interoperability guarantee (DataCite mapping, harvesting, ISO 23527) is
  preserved.
- **Customisation lives only in a namespaced `extensions` envelope**, keyed by
  RA. Each RA registers a versioned extension schema in the registry.
- Extensions are **validated at runtime** against the RA's registered schema
  (new engine), **stored as JSON**, and **serialised generically**: JSON-LD via
  an RA-supplied `@context`; omitted or passed through opaquely for
  DataCite/RDF (extensions are not part of the interoperable core, so this is
  acceptable and explicit).
- Schema resolution keys off the already-persisted
  `registrationAgencyOrganisationId` (or service point).

- **Pros:** supports arbitrary RA extension without redeploy; protects the
  interoperable core; contains the serialisation blast radius; incremental
  (can ship the envelope + registry after a compiled phase-1).
- **Cons:** still substantial; requires governance for the extension namespace;
  two validation modes (compiled core + runtime extensions) to reason about.
- **Refinement:** the "runtime" claim holds **only for the declarative tier**
  (see below). Anything imperative is build-time — which leads to Option E.

### Option E — Per-RA modules behind SPI contracts + external config (refined recommendation)
The pragmatic JVM realisation of D once business logic is in scope. Each RA's
customisation (LinkML-generated model + `Validator`/mapper implementations
conforming to core SPI contracts) is a **build-time Gradle dependency** published
to CodeArtifact, exactly as the project already ships `@raid-org`/`@raid-au`
libraries. The core defines the SPI contracts; `ValidationService` cycles a
discovered `List<Validator>`; **external config keyed by
`registrationAgencyOrganisationId`** selects/parameterises which behaviours are
active per RA. The optional declarative-runtime engine from D sits on top for
constraints an RA wants to change without a release.

- **Pros:** supports business logic safely (compiled code behind an SPI, no
  RCE); reuses existing CodeArtifact + Spring DI; core stays typed and
  untouched; "single codebase" preserved literally.
- **Cons:** all RA code ships in every deployment (blast radius / isolation,
  growing build+test surface); onboarding an RA needs a rebuild + release (fine
  given slow cadence); needs the `Validator` SPI refactor (doesn't exist today).
- **Fit:** the recommended primary mechanism. See "Reframing 'registry'" below.

## What can and cannot be loaded at runtime (the decisive constraint)

A naive "load the whole schema at runtime" breaks the moment you ask how the
Java validation layer and the external mappings would follow. They can't — and
the reasons draw a clean, principled boundary that the whole design rests on.

### Two kinds of validation — only one is data
- **Declarative / structural** (types, required/optional, cardinality, regex/URI
  patterns, enums, string length, simple `if/then/else` conditional presence).
  Expressible in JSON Schema / SHACL, therefore **safe to load from a registry**
  and enforce with a generic runtime engine.
- **Behavioural / business logic** — the ~30 hand-written validators: external
  identifier resolution and liveness (ROR/ORCID/ISNI/GeoNames network checks),
  computed cross-field rules (e.g. embargo ≤ 18 months, open-access ⇒ no
  embargo), and anything depending on system state or other services. This is
  **not expressible declaratively and must not be loaded from a registry** —
  loading executable logic as "config" is a remote-code-execution surface, not a
  configuration mechanism.

**Rule:** the registry carries *declarative constraints only*. Imperative logic
is code. If an RA genuinely needs business-rule validation on an extension, that
is a **governed code contribution** — a plugin/SPI keyed by RA, reviewed and
tested, shipped in the single codebase per release. That is acceptable precisely
because schema change is a slow, deliberate affair; it does not need to be
runtime-dynamic. **Registry = declarative data; codebase = imperative logic.**

### The core never "understands" extensions
This is why the core code keeps working. The core stays exactly as compiled
today. To the core, an extension block is **opaque, namespaced JSON**: it is
stored, passed through, served back, and subjected to *one* bounded declarative
validation pass against the RA's registered schema at ingest. The core never
interprets extension semantics, so every existing core validator, DTO and
mapping is untouched. The dynamic behaviour is quarantined to a validation gate
over a blob the core otherwise ignores.

### Integrators split along the same boundary
- **DataCite (and RDF / schema.org harvesting) is a *global* integrator** and
  consumes the **interoperable core only**. Extensions are not globally
  standardised, so they are **invisible to global integrators by design** — not
  a compromise, but because a global integrator has no business with
  agency-local metadata. The hand-coded core→DataCite mapping therefore stays
  core-only and centrally maintained.
- **RA-local integrators** (research management apps, a university RIMS) are the
  actual consumers of extension metadata. The serialisation need is
  **RA-scoped, not global**: those consumers know their RA's extension shape and
  read the namespaced JSON envelope directly (optionally via an RA-supplied
  JSON-LD `@context`). No global mapping is required or wanted.

### The architectural spine
Validation and integration decompose the **same way**: global, shared concerns
(business rules that protect the standard; global integrator mappings) stay in
the centrally-governed compiled core; local concerns (declarative extension
constraints; local integrator consumption) are RA-scoped. Anything that needs
imperative behaviour — core or extension — is a governed code contribution, not
registry data.

## Reframing "registry": build-time artefacts + runtime config

"Registry" is misleading if it implies a live service the API pulls schemas from
at runtime. Once business logic is in scope (previous section), that model can't
work — you cannot load executable code as data. The realistic mechanism splits
into two things that are usually conflated:

1. **A build-time artefact registry = Maven / AWS CodeArtifact.** This is
   already how the project ships `@raid-org` / `@raid-au` shared libraries. A
   per-RA module — its LinkML-generated model plus its `Validator`/mapper
   implementations conforming to core SPI contracts — is published here as a
   versioned jar, and the core API declares it as a Gradle dependency.
   **Build-time.** Adding/changing an RA = new artefact version + rebuild +
   release (acceptable given the slow governance cadence).
2. **A runtime configuration source.** External config, keyed by
   `registrationAgencyOrganisationId`, that *selects and parameterises* which
   contributed behaviours are active for each RA (e.g. active validator set,
   embargo-max-months, allowed `schemaUri`s). This is the only genuinely runtime
   part, and it is **configuration, not code**.

A **live** registry (pull a new schema and enforce it with no rebuild) is
feasible **only for the declarative-only tier** (types/patterns/enums via a
generic engine). It is optional and additive; everything requiring imperative
logic is build-time. So Matthias's "registry that builds artefacts the API pulls
in at runtime" resolves to: CodeArtifact holds artefacts (build-time) + config
selects behaviour (runtime), with an optional declarative-runtime layer on top.

### The enabling change (grounded)
The target pattern — `ValidationService` cycling a `List<Validator>` that
per-RA jars contribute to — **does not exist yet**:
- There is no shared `Validator` SPI. Only a narrow `UriValidator` interface
  exists (URI-shaped validators). The block-level validators (`TitleValidator`,
  `ContributorValidator`, … ~30 in `raid-api/.../validator/`) share no common
  contract; `ValidationService` names each as a distinct constructor-injected
  bean.
- Required work: (a) define a core `Validator` SPI (single `validate(context)`
  contract); (b) refactor `ValidationService` to inject `List<Validator>`
  (Spring populates it from all beans of the type); (c) let per-RA dependency
  jars contribute `Validator` beans via component scan or Spring
  auto-configuration (`AutoConfiguration.imports`) shipped in the jar.
This refactor is the real unit of work behind "different RAs tweak the API's
behaviour".

### Trade-off: all RA code is present in every deployment
Because per-RA behaviour is a build-time dependency, every RA's code ships in
every deployment. For a single global RAiD-AU deployment this is fine, but:
one RA's validator bug or transitive dependency can affect the shared process
(blast radius / isolation), and the build + test surface grows with agency
count. This is the concrete form of the "single codebase" tension — the honest
cost of choosing build-time safety over runtime dynamism. Mitigations: strict
SPI boundaries, per-RA module ownership, isolation tests.

## Alternatives considered (broader strategies)

Options A–E above are variations on one strategy: a single codebase with per-RA
behaviour compiled in. These are genuinely *different* strategies, varying on two
axes — how isolated each RA's code is, and how much is code vs data.

### LinkML-centric, config-driven (Matthias's preference) — leading alternative
Make LinkML the single source of truth and *generate* as much as possible:
schema, validation constraints, JSON-LD/RDF context, SQL, and — as far as
feasible — the DataCite/schema.org mappings. Per-RA variation is expressed as
LinkML models + transformation/config that the RA maintains.

**Correction to the earlier "generic model-driven store" framing:** LinkML is
*not* a schemaless EAV store. It is a modelling language designed to be the
interoperable contract and to generate typed, standards-aligned artefacts — the
build already uses `gen-json-schema`, `generateJSONLDContextV2`,
`generateSHACLV2`, `generateOWLContextV2`. So this approach does **not**
necessarily give up type safety or interoperability.

It splits into two sub-variants, and which one is intended is a crux decision:
- **(a) Build-time generation** — LinkML is the source of truth, everything is
  generated at build time (extends today's chain to cover mappings + per-RA
  models). Still produces typed artefacts; preserves interoperability; lower
  risk.
- **(b) Runtime interpretation** — LinkML models + transform specs interpreted
  at runtime as pure config. Maximum flexibility, but hits the imperative I/O
  floor and gives up compile-time type safety.

**This converges with Option E rather than competing.** LinkML generation is the
*declarative backbone* (schema, constraints, mappings as config); compiled SPI
modules remain the *escape hatch* only where imperative logic — external
ROR/ORCID/ISNI/GeoNames resolution, stateful rules — is unavoidable. The honest
synthesis is: maximise what LinkML/config expresses, fall back to compiled code
only at the floor.

- **Pros:** single declarative source of truth; removes hand-coded serialisation
  where possible; collapses the `SchemaValues.java`-vs-LinkML dual source of
  truth; aligns with RA-maintained models.
- **Cons/unknowns:** the (a)-vs-(b) decision; whether DataCite mapping survives
  as config (see next section — needs a PoC); a "declarative complexity cliff"
  where a transform DSL is harder than Java for gnarly cases.

### Other strategies (for completeness)
- **Separate deployment per RA (same codebase, N builds):** one codebase, one
  build+instance per RA. Buys real process isolation (fixes Option E's
  blast-radius weakness); costs N deployments to operate. Still "single
  codebase".
- **Runtime plugin modules (dynamic classloading — PF4J / OSGi / module
  layers):** per-RA jars loaded at runtime via isolated classloaders. Add an RA
  without a core rebuild; genuine module isolation; still compiled code (no
  RCE). Costs: classloader + Spring-integration complexity, version management.
- **Extension service / sidecar owned by each RA:** the core handles only the
  common core; each RA runs its own service for local validation/storage/
  serialisation over a defined contract. Cleanest separation and best fit for
  "the RA owns its schema"; heaviest operationally (distributed system, latency,
  each RA must run a service).
- **Declarative rules engine (CEL / JSONLogic / Drools / SHACL-AF):** express
  per-RA validation/mapping as sandboxed data, no per-RA code, no rebuild, no
  RCE. Does more than expected (cross-field, date arithmetic — the embargo rule
  *is* expressible). Hard ceiling at I/O (external resolution). Overlaps with
  LinkML-centric (b).
- **Union / superset core schema (no mechanism):** absorb every RA requirement
  into one shared schema as optional fields. Zero new architecture; may be
  adequate given few RAs and slow cadence. Costs: schema bloat; can't enforce
  "RA X requires field Y" without conditional logic; governance = everyone
  agrees on everything. The minimalist baseline to measure any proposal against.
- **Fork per RA (the null option):** each RA forks the codebase. Precisely what
  this ticket exists to avoid; named only to clarify *why* a mechanism is wanted.

## Mapping the external formats (schema.org vs DataCite)

Can the serialisation mappings be config/LinkML-driven too? They split, and behave
very differently. Note both are **core** (global-integrator) concerns, centrally
maintained — extensions do not map to them — so this is worthwhile independent of
the per-RA question, as a consolidation of hand-coded factories.

### schema.org / JSON-LD / RDF — yes, strongly
LinkML's sweet spot. External-vocabulary mapping is first-class in the metamodel
(`class_uri`, `slot_uri`, `exact_mappings`/`close_mappings`/`related_mappings`);
JSON-LD `@context`, SHACL and OWL fall out natively, and the build already emits
them. `researchproject.yaml` already maps RAiD to `schema:ResearchProject`. The
hand-coded Jena `RaidRdfService` / schema.org serialisation could therefore move
into LinkML-driven generation — a real consolidation win.

### DataCite — partially; needs a PoC
DataCite is a **different target schema**, not just a vocabulary to annotate
against, so it needs a *structural transformation* (hence the ~15 factories:
derived contributor types, URI→identifier+scheme splitting, relatedIdentifier
relationType logic, defaulting). Term-URI annotations alone cannot produce it.

LinkML's **`linkml-map` (LinkML Transformer)** is built for this — declarative
class-to-class / slot-to-slot transformation specs with an expression language:
- Structural + simple conditional/derived mapping is expressible as config.
- **Floor:** anything needing I/O or system state cannot be pure config.
- **Risk:** a "declarative complexity cliff" — DataCite's gnarlier rules may be
  *harder* to express and debug in a transform DSL than in Java.

Calibration: I'm confident LinkML handles the schema.org/RDF side and that
`linkml-map` is designed for schema-to-schema transformation; I am **not**
confident it cleanly handles *all* of RAiD's DataCite mapping. That is an
empirical question to settle with a small PoC before committing (see follow-ups).

## Recommendation

The two leading candidates — **Option E** (compiled per-RA modules behind SPI +
config) and the **LinkML-centric config-driven** approach (Matthias's
preference) — are **complementary, not rivals**. Recommended direction is their
synthesis: **LinkML as the single declarative source of truth generating as much
as possible (schema, constraints, JSON-LD/RDF, and — pending the DataCite PoC —
mappings), with compiled SPI modules as the escape hatch only for imperative
logic (I/O, stateful rules) that cannot be expressed as config.** Because schema
ownership sits with the RA and change is slow and deliberate, build-time
generation is the right default; runtime interpretation of schemas is a *later,
optional* step for the purely declarative tier.

Two decisions gate the design and should be settled first (with Matthias): the
**(a) build-time-generation vs (b) runtime-interpretation** fork, and the
**scope** crux (extensions vs divergent cores) below.

1. **Reframe & agree scope + (a)-vs-(b)** with Matthias/product: confirm
   "customised schemas" means **governed extensions to a common core**, bound to
   the **registration agency**, and whether LinkML is generated at build time or
   interpreted at runtime. Pivotal; settle before design.
2. **Phase 1 (Option A):** introduce a first-class, namespaced `extensions`
   block in LinkML, keyed by RA, compiled in for 1–2 pilot agencies. Proves the
   data model and serialisation-omission behaviour with low risk. Given the slow
   change cadence, **this may be sufficient on its own for a considerable time.**
3. **Phase 2 (LinkML-centric generation + Option E SPI):** extend the LinkML
   generation chain to cover per-RA models and (pending PoC) the external
   mappings; define the core `Validator`/mapper SPI contracts, refactor
   `ValidationService` to cycle a discovered `List<Validator>`, and move per-RA
   imperative behaviour into separate modules published to CodeArtifact, pulled
   in as build-time dependencies and activated by external config keyed by
   `registrationAgencyOrganisationId`.
4. **Phase 3 (declarative-runtime tier) — only if/when justified:** add a
   generic runtime JSON-Schema engine so RAs can change *purely declarative*
   constraints without a release. Trigger only when agency count or release
   friction warrants it. This is the only part that is a "live registry".
5. **Consolidate the dual source of truth** (`SchemaValues.java` vs LinkML
   enums) as part of this — the split will not survive multiple schemas. Moving
   the external mappings into LinkML consolidates further.
6. **Design the governance workflow first.** The RA-mediated,
   consult-the-service-points process is the genuinely hard part; the build/SPI
   plumbing is comparatively straightforward. A mechanism without an agreed
   review/versioning/adoption workflow is premature.

## Open questions / follow-up spikes

The first two are the crux — they must be resolved before any design work, and
are the primary items for discussion on this ticket:

- **[CRUX] Scope of "customised": extensions to a common core, or divergent
  cores?** This whole analysis assumes RAs may only *extend* a common,
  immutable core (to preserve interoperability, DataCite mapping and ISO 23527).
  Does Matthias/product intend that, or genuinely mean RAs could diverge on the
  *core* itself? Everything downstream depends on this answer.
- **[CRUX] Prior art / alignment:** is there an existing RAiD International or
  DataCite position on metadata schema extensibility we should align to, rather
  than coining our own model and terminology? Need to confirm before proposing a
  design so we don't diverge from an emerging standard.

Concrete follow-up spikes:

- **[SPIKE] `linkml-map` DataCite PoC:** take 2–3 existing DataCite factories
  (one simple, one gnarly — e.g. `DataciteContributorFactory` with its derived
  contributor-type logic) and attempt to reproduce them as a `linkml-map`
  (LinkML Transformer) specification. Deliverable: a verdict on whether
  config-driven DataCite mapping is realistic or hits the "declarative
  complexity cliff", plus an estimate of how much of the ~15 factories could
  move to config. This gates the "mappings as config" claim. (schema.org/RDF is
  low-risk and can be scoped separately or assumed feasible.)
- **[SPIKE] LinkML build-time vs runtime (a-vs-b):** prototype generating a
  per-RA extension model through the existing chain (build-time) and contrast
  with interpreting a LinkML schema at runtime, to make the trade-off concrete.

Design-level questions (once the crux items are settled):

- **Core vs extension boundary:** if extensions-only is confirmed, what exactly
  is the extensible surface? (Recommend: core immutable and common; extensions
  live only in a namespaced envelope.)
- **Registry ownership & governance (the hard part):** the RA owns the schema
  and drives a slow, consult-the-service-points adoption process. What is the
  concrete review/publishing workflow, and how are versioning + backward
  compatibility enforced across an RA's service points?
- **Build-time vs runtime artefacts:** "registry builds artefacts the API pulls
  in" collides with the current CI-has-no-Docker constraint and committed
  generated artefacts. Pulled at deploy or truly at request time?
- **Extension business logic:** what is the plugin/SPI contract for an RA that
  needs *imperative* validation on an extension (keyed by RA, compiled,
  per-release)? How is it isolated so one RA's logic can't affect another's?
- **Serialisation for RA-local integrators:** does the raw namespaced envelope
  suffice, or do we offer an RA-supplied JSON-LD `@context`? (Confirmed: global
  integrators such as DataCite consume the core only; extensions do not map to
  them.)
- **Storage:** JSON columns vs typed tables; impact on existing reference-data
  tables and search/harvest.
- **Search & harvest:** how do extensions surface (or not) in OAI-PMH / the
  federation work (RAID-740 Parquet+DuckDB)?
