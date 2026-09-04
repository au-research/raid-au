# RAID-806: Per-agency Service Point ID ranges

- **JIRA:** [RAID-806](https://ardc.atlassian.net/browse/RAID-806) (Story)
- **Parent:** [RAID-1](https://ardc.atlassian.net/browse/RAID-1)
- **Integration branch:** `feature/RAID-806`
- **Date:** 2026-09-05

## Sub-tasks

| Ticket | Summary | PR |
| --- | --- | --- |
| [RAID-862](https://ardc.atlassian.net/browse/RAID-862) | Registration Agency register and required agency identifier | [raid-au#641](https://github.com/au-research/raid-au/pull/641) |
| [RAID-863](https://ardc.atlassian.net/browse/RAID-863) | V46 Service Point renumbering migration | [raid-au#642](https://github.com/au-research/raid-au/pull/642) |
| [RAID-864](https://ardc.atlassian.net/browse/RAID-864) | One-shot Flyway repair token | [raid-au#646](https://github.com/au-research/raid-au/pull/646) |
| [RAID-865](https://ardc.atlassian.net/browse/RAID-865) | Move test fixtures onto the reserved block | [raid-au#644](https://github.com/au-research/raid-au/pull/644) |
| [RAID-866](https://ardc.atlassian.net/browse/RAID-866) | Registration Agency identifier in both CDK trees | [raido-v2-aws-private#47](https://github.com/au-research/raido-v2-aws-private/pull/47) |
| [RAID-867](https://ardc.atlassian.net/browse/RAID-867) | Operator documentation and ADR | [raid-au#645](https://github.com/au-research/raid-au/pull/645) |
| [RAID-868](https://ardc.atlassian.net/browse/RAID-868) | ECS deployment circuit breaker | [raido-v2-aws-private#48](https://github.com/au-research/raido-v2-aws-private/pull/48) |
| [RAID-872](https://ardc.atlassian.net/browse/RAID-872) | Drop the obsolete `app_user` and `user_authz_request` tables | [raid-au#643](https://github.com/au-research/raid-au/pull/643) |

Raised along the way, not part of this story:
[RAID-873](https://ardc.atlassian.net/browse/RAID-873) — `generateJooq` fails on
foreign key metadata and deletes `AccessType.java`.

## What changed and why

`service_point.id` started at a hardcoded `20000000` on every RAiD Service
instance, set in `V4__sign_in_tables.sql` and again in the `B25__baseline.sql`
used to bootstrap a new database. That id is published in RAiD metadata as
`identifier.owner.servicePoint`, with no name and no resolution.

Every Registration Agency therefore began counting from the same number, so two
agencies could mint Service Points that were indistinguishable in federated
metadata. The Federation API keys on `(registrationAgency.id, servicePoint)`,
which depends on that pair being unique.

Each agency is now allocated a block of ten million ids, and an instance derives
its block from its own identity rather than being told the number.

### The range is derived, not configured

The obvious design — a configuration value naming the starting id — was
rejected. Nothing in a deployment identifies it as SURF's, so a mis-set range
would pass every check that could be built around it, because every such check
derives from the number it is meant to be checking.

Worse, a containment check comparing existing data against the configured range
gives exactly the wrong answer during an upgrade. An instance still holding
pre-renumbering data at `20000000` would *reject* the correct configuration and
*accept* the incorrect one.

Instead the range is derived from `raid.identifier.registration-agency-identifier`,
the agency's ROR, which already flows into `identifier.registrationAgency.id` on
every RAiD minted. A register of ROR to block allocations ships inside the
artefact, read straight from the classpath rather than bound as Spring
configuration so that no environment variable can reassign an agency's block.

This ties an invisible failure to a glaring one: an agency configuring another
agency's ROR to obtain their block would publish every RAiD under that agency's
name.

The ARDC default for that property was removed and the property made required.
Left in place, another agency's deployment that failed to override it would have
silently inherited ARDC's identity, resolved to ARDC's block, passed every check
and minted into ARDC's range.

Full reasoning is in
[the ADR](../doc/adr/2026-09-04_service-point-id-range-from-agency-identity.md).

### Renumbering

V46 renumbers on upgrade, anchoring the lowest id at the start of the block so
gaps survive and referenced values stay consistent. Data already inside the block
is left completely untouched. A `CHECK` constraint bounds the column afterwards;
adding it validates every existing row, so it serves as both the future guard and
the assertion that the renumbering landed.

`V4` and `B25` were not edited. Changing an applied migration alters its checksum
and forces a Flyway repair on every existing instance, which is the
no-manual-intervention requirement this story exists to respect.

### Repair without manual SQL

`raid.db.repair-token` triggers a one-off Flyway repair through reviewed
configuration. A repair cannot be delivered as a migration: `migrate()` validates
first, so a checksum mismatch aborts before any migration runs, and repair also
rewrites the history table the surrounding migrate is writing to. So the trigger
is configuration and the disarm is the database — the same token can never fire
twice, and the log doubles as an audit trail.

### Deployment failure is visible

No ECS circuit breaker was configured anywhere, so a task that could not start
crash-looped while the deployment sat in progress and `minHealthyPercent` kept
part of the old service serving. All seven services now fail the deployment
instead, with rollback disabled everywhere: restoring the previous image is
unsafe for anything that owns a schema.

## Findings worth keeping

Several assumptions turned out to be wrong, and were corrected against evidence
rather than reasoning:

- **`service_point.id` is `GENERATED ALWAYS AS IDENTITY`**, which rejects
  `UPDATE` outright (`column "id" can only be updated to DEFAULT`). The planned
  two-statement migration could never have worked. V46 relaxes the identity for
  the renumbering and restores it afterwards.
- **Flyway leaves no failed schema-history row** when a migration rolls back
  transactionally on PostgreSQL. Recovering from a misconfigured range therefore
  needs no repair, which removed a dependency between RAID-863 and RAID-864.
- **B25 is applied on an empty schema**, and seeds its own Service Point at
  `20000000`.
- **`raid.identifier.registration-agency-identifier` was set nowhere.** Neither
  CDK tree configured it, so instances relied on the ARDC default. There was a
  third configuration site beyond the two environment-properties files — the
  branch API stack — found only when RAID-862's branch deploy failed.
- **A failed start does not look like a failure.** That branch deploy sat in
  `CREATE_IN_PROGRESS` for an hour before CodeBuild timed out; the cause was one
  line in the container log. This is what RAID-868 addresses.
- **Creating the repair log on a never-migrated database breaks Flyway**, which
  then refuses to migrate a non-empty schema with no history table. Arming a
  token on a new instance would have stopped it starting.
- **Most `20000000` literals are written `20_000_000`** and are invisible to a
  search for the plain digits.

## Decisions recorded

- `raid_history` diffs are left exactly as originally recorded. A reconstructed
  historical version therefore shows the pre-renumbering id. The history is an
  audit trail of what was captured, not a restatement of it.
- Deployed row counts for `app_user` and `user_authz_request` were not checked
  before dropping them (RAID-872). No write path existed in the code, so any rows
  present were stale data nothing read.
- The JOOQ classes affected by RAID-872 were hand-edited, because `generateJooq`
  is broken (RAID-873) and regenerating would have committed unrelated damage.

## Testing

- Unit tests across the register, the Flyway placeholder wiring and the repair
  strategy's unarmed path.
- Migration and repair behaviour exercised against a throwaway PostgreSQL via
  Testcontainers, scoped to the `intTest` source set so `./gradlew build` does not
  start requiring Docker. The branch pipeline runs `intTest` under privileged
  CodeBuild, so they gate there too.
- Full `intTest` green at each step, finishing at 230 tests.
- V46 applied to a local development database holding 296 real RAiDs, correctly as
  a no-op since ARDC already occupies its block.
- RAID-862 verified end to end in a deployed branch environment: every pipeline
  stage green, including `Api-Integration-Test` and `E2e-Test`.

## Outstanding before this reaches main

**The register holds ARDC and SDSC only.** SURF, DRAC and TIB have allocated
blocks but their RORs are not yet known, so they are not in
`registration-agencies.yaml`. SURF is deployed, and merging without its entry
would stop their instance starting.

**Merging RAID-866 was not sufficient for test, demo, stage and prod.** Those
`environment-properties.ts` changes take effect only on a manual
`npx cdk deploy` of the Api stack per environment, which must happen *before* a
RAID-862 image reaches them. Only the branch API stack updates automatically,
via InfraSource from `main`.

**The live rehearsal has not been done.** RAID-806 requires rehearsing the
renumbering against a full copy of a real Registration Agency database before
running it for real. projectpid.org is the subject: the only instance that both
renumbers and sits on infrastructure ARDC controls.

**Agencies have not been told.** Informing each agency of its allocated range and
capturing it in their agreements is Matthias's action, and SURF needs warning
that their Service Point IDs will change.
