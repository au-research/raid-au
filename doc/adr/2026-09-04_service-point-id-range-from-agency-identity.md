# Derive an instance's Service Point ID range from its Registration Agency identity

- Status: accepted
- Date: 2026-09-04
- Ticket: RAID-806 (implemented by RAID-862, RAID-863)

## Context

`service_point.id` started at a hardcoded `20000000` on every instance, set in
`V4__sign_in_tables.sql` and again in the `B25__baseline.sql` used to bootstrap a
new database. The value is published in RAiD metadata as
`identifier.owner.servicePoint`, with no name and no resolution.

So every Registration Agency — ARDC, SURF, SDSC, DRAC, TIB — began counting from
the same number, and two agencies could mint Service Points that were
indistinguishable in federated metadata. The Federation API keys on
`(registrationAgency.id, servicePoint)`, which relies on that pair being unique.

The Registration Authority allocates each agency a block of ten million ids,
formalised in the agency's agreement. The question was how software should learn
which block it is in.

## Decision

### The range is derived from agency identity, not configured

The obvious approach is a configuration value naming the starting id. It was
rejected.

Nothing in a deployment identifies it as SURF's. A mis-set range would therefore
pass every check that could be built, because every such check derives from the
same number it is meant to be checking. Worse, a containment check comparing
existing data against the configured range gives exactly the wrong answer during
an upgrade: an instance still holding pre-renumbering data at `20000000` would
reject the correct configuration and accept the incorrect one.

Instead the range is derived from `raid.identifier.registration-agency-identifier`,
the agency's ROR, which already flows into `identifier.registrationAgency.id` on
every RAiD minted. A register of ROR to block allocations ships inside the
artefact, and the instance looks itself up at startup.

This ties an invisible failure to a glaring one. An agency configuring another
agency's ROR to obtain their block would publish every RAiD under that agency's
name.

### The register ships in the artefact, not in configuration

`registration-agencies.yaml` is read straight from the classpath rather than
bound as Spring configuration, because anything on the Spring `Environment` can
be overridden by a deployment. Allocation is the Registration Authority's to
make, so it travels with the release and is auditable in version control.

Onboarding an agency is therefore a pull request plus a release, not a value an
agency sets for itself. A unit test asserts that blocks and RORs are unique,
which makes the review a real gate rather than a formality.

Blocks are stored as an index, with the first id computed as
`block * blockSize`. A missing zero is not expressible.

### The property becomes required, with no default

`raid.identifier.registration-agency-identifier` defaulted to ARDC's ROR. That
default had to go: another agency's deployment that failed to override it would
silently inherit ARDC's identity, resolve to ARDC's block, pass every check and
mint into ARDC's range.

### Failure happens before any migration runs

Resolution happens in a `FlywayConfigurationCustomizer`, while the Flyway bean is
being built. An unallocated instance therefore fails with the database untouched
and the previously deployed version still serving, rather than committing a
partial renumbering. The same check is repeated in `afterPropertiesSet` to cover
deployments that build no Flyway bean at all, such as one with
`spring.flyway.enabled: false`.

### Renumbering is a versioned migration, and the existing ones are never edited

V46 renumbers on upgrade. `V4` and `B25` are left exactly as they are: editing an
applied migration changes its checksum and forces a Flyway repair on every
existing instance, which would breach the project's no-manual-intervention
requirement — the very thing this work is trying to respect.

The lowest id is anchored at the start of the block, so differences between ids
are preserved and existing gaps survive. Data already inside the block is left
untouched rather than rewritten.

A `CHECK` constraint bounds the column to the allocated block. Adding it
validates every existing row, so it serves as both the future guard and the
assertion that the renumbering landed correctly.

## Consequences

- Onboarding a new Registration Agency requires a release, not just
  configuration. This is deliberate; the cost is a slower onboarding step in
  exchange for allocation being controlled and auditable.
- An agency whose ROR is not yet in the register cannot start. For an agency
  running its own infrastructure the startup error message and the
  [operations guide](../reference/service-point-id-ranges.md) are the entire
  interface, so both are worded for an operator with no access to this codebase.
- Existing Service Point IDs change on upgrade for any agency not already in its
  block, and those ids are already published in RAiD metadata.
- `raid_history` is deliberately not rewritten, so a reconstructed historical
  version shows the pre-renumbering id. The history records what was captured at
  the time rather than restating it.
- Reallocating an agency's block later needs a migration to drop and recreate the
  check constraint. Allocations are intended to be permanent.
- Namespacing `identifier.owner.servicePoint` itself, and the Federation API's
  use of the composite key, remain out of scope and are tracked separately.

## Notes

Two behaviours were verified rather than assumed while implementing this:

- `service_point.id` is `GENERATED ALWAYS AS IDENTITY`, which rejects `UPDATE`
  outright. V46 relaxes the identity for the renumbering and restores it after.
- Flyway leaves no failed schema-history row when a migration rolls back
  transactionally on PostgreSQL, so a misconfigured range needs no repair to
  recover from — correcting the configuration and redeploying is sufficient.
