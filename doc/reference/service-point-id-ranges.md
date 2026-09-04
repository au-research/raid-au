# Service Point ID ranges

Every RAiD Service instance assigns each of its Service Points a numeric id. That
id is published in RAiD metadata as `identifier.owner.servicePoint`, so it is
visible to anyone reading a RAiD.

Historically every instance started counting from the same value, `20000000`.
Two Registration Agencies could therefore mint Service Points with identical ids,
and nothing in the metadata distinguished them. Each Registration Agency is now
allocated its own range.

This guide is for anyone deploying or operating a RAiD Service instance.

## How a range is allocated

The RAiD Registration Authority allocates each Registration Agency a block of ten
million ids, and records it in the agency's agreement. Blocks are numbered, and a
block's first id is its number multiplied by ten million:

| Block | First Service Point ID |
| --- | --- |
| 2 | 20000000 |
| 3 | 30000000 |
| 4 | 40000000 |
| 10 | 100000000 |

There is nothing special about the number of digits. Block 10 simply starts at
`100000000`, and the scheme continues indefinitely. **Never assume a Service
Point ID has a fixed number of digits.**

Block 1 is reserved for local development, automated tests and fixtures, so test
data can never be mistaken for a real agency's Service Point.

## Configuring your instance

You do **not** configure the range directly. You configure your agency's
identity, and the software derives the range from it:

```
raid.identifier.registration-agency-identifier = https://ror.org/<your ROR>
```

This must be your own agency's ROR. It is already published as
`identifier.registrationAgency.id` on every RAiD your instance mints, so setting
it to another agency's ROR would attribute all your RAiDs to them.

Deriving the range from identity rather than configuring it separately means
there is no second value that can silently disagree with the first. See
[the ADR](../adr/2026-09-04_service-point-id-range-from-agency-identity.md) for
the reasoning.

If you deploy with the AWS CDK in `raido-v2-aws-private`, set
`RAID_REGISTRATION_AGENCY_IDENTIFIER` in your environment before running `cdk
synth` or `cdk deploy`. Synthesis fails if it is unset.

## What happens if it is not set

The application **refuses to start**. This is deliberate: an instance with no
allocated range would mint Service Point IDs that collide with another agency's.

Two distinct failures, both reported before any database migration runs, so your
database is left untouched and your previously deployed version keeps serving:

**The value is not set at all.**

```
raid.identifier.registration-agency-identifier is not set. Set it to the ROR of
the Registration Agency operating this instance. Its Service Point ID range is
derived from that ROR, and is assigned by the RAiD Registration Authority.
```

**The value is set, but that ROR has no allocated range.**

```
Registration Agency 'https://ror.org/...' has no Service Point ID range
allocated. Contact the RAiD Registration Authority to be allocated one, which is
then recorded in registration-agencies.yaml and released.
```

The second means your agency is not yet in the register. Allocations ship inside
the application artefact, so a new agency needs a release, not a configuration
change you can make yourself. Contact the Registration Authority.

Note that a failed start may not present as an obvious failure. On ECS the task
will crash, restart and crash again, while the deployment sits in progress and
the previous version continues to serve traffic. Check your container logs for
the messages above rather than waiting for the deployment to report an error.

## Upgrading an existing instance

**If your Service Points are not already in your allocated block, their ids will
change.** A database migration renumbers them on the first start after the
upgrade, and updates everything that refers to them, including the values
embedded in stored RAiD documents.

Gaps are preserved: if you had ids 20000000, 20000001 and 20000003, and your
block starts at 30000000, they become 30000000, 30000001 and 30000003.

Two consequences worth planning for:

- The Service Point IDs published in your existing RAiD metadata will change.
  Anything outside RAiD that has recorded those ids will need updating.
- Historical versions in `raid_history` are deliberately **not** rewritten. A
  reconstructed old version shows the id as it was at the time, which will not
  match the current record. This is an accepted trade-off: the history is an
  audit trail of what was recorded, not a restatement of it.

If your Service Points are already inside your allocated block, the migration
does nothing at all. No rows are modified.

The whole renumbering runs in a single transaction. If anything is inconsistent
it rolls back completely, leaving your database exactly as it was, rather than
committing a half-applied change.

## After the upgrade

The database enforces the range with a constraint, so a Service Point can no
longer be created outside your allocated block. If you see:

```
new row for relation "service_point" violates check constraint
"service_point_id_within_allocated_block"
```

then something has tried to create a Service Point outside your range. That
should not happen in normal operation.

Changing an agency's allocation after the fact requires a migration to drop and
recreate that constraint, so allocations are intended to be permanent.
