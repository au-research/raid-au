### Filter `updatedSince` on the materialised `metadata.updated` value, not a new `raid.updated` column

* Status: final
* Who: proposed and finalised by RL
* When: 2026-09-29
* Related: RAID-739 (epic), RAID-837 (story), RAID-897 (OpenAPI parameter),
  RAID-898 (V49 index), RAID-899 (repository/service/controller filtering),
  RAID-900 (malformed-parameter 400 handling), RAID-901 (local-dev realm
  role for intTest), RAID-902 (integration tests), RAID-903 (this ADR)

# Context

RAID-739 asked for an incremental sync mechanism on `/raid/all-public` and
`/raid/all-embargoed` so Federation API consumers can pull only the RAiDs
that changed since their last call, instead of re-fetching the entire
corpus every time. The natural parameter is an ISO 8601 `updatedSince`
timestamp, but the `raid` table has no `updated` column to filter on. The
only place a RAiD's last-updated time exists today is inside the
materialised JSONB metadata blob, as `metadata -> 'metadata' ->> 'updated'`,
stored as Unix epoch seconds and written by `RaidService.update` via
`RaidHistoryService.save` on every mutation.

Three options were considered:

1. Add a `raid.updated` column, backfilled and kept in sync on every write.
2. Filter on `raid_history.created`, the database transaction timestamp of
   the most recent history row for a handle.
3. Filter on the existing `metadata.updated` value already inside the
   JSONB column, with a fallback for rows that predate it.

# Decision

Filter on `metadata.updated` (option 3), via a jOOQ field expression
(`RaidRepository.UPDATED_SINCE_EPOCH_SECONDS`) backed by a matching
Postgres expression index (`V49__updated_since_index.sql`,
`idx_raid_updated_since`), falling back to `extract(epoch from
date_created)` for rows with no `metadata.updated` value (legacy rows
predating the field, or a null-metadata edge case). The boundary is
strictly-after (`>`, not `>=`): a RAiD updated at exactly the cursor
timestamp is excluded, so a client can safely re-poll with its last-seen
`updated` value as the next `updatedSince` without receiving the same
record twice on that exact boundary.

### Why not a new `raid.updated` column

Adding a column would have needed a JOOQ regeneration. Regenerating JOOQ
classes on this project has previously corrupted generated sources —
`generateJooq` has silently dropped foreign-key join paths from
`Raid.java` and deleted `AccessType.java` outright on unrelated schema
edits, requiring hand-repair of generated code (a known, recurring risk
on this codebase, not specific to this change). Avoiding a schema change
that only a handful of query sites need, when the value already exists
elsewhere, avoids that risk entirely.

Filtering on the same value already serialised into the response body
also keeps the filter and the payload consistent by construction. A
consumer using the RAiD's own `updated` field as its next cursor is
filtering on exactly the value it will receive back — there is no
possibility of the filter and the exposed timestamp drifting apart, and no
gap where a record could be updated between "the column" and "the
metadata" being written.

### Why not `raid_history.created`

`raid_history.created` is a database transaction timestamp with
microsecond precision. `metadata.updated` is set from
`LocalDateTime.now()` truncated to whole seconds. The two are never
exactly equal for the same update. Filtering on `raid_history.created`
while reporting `metadata.updated` in the response would make the
strictly-after exclusion boundary untestable in practice (a client's
cursor, taken from a returned `updated` value, would never exactly match
any `raid_history.created`), and would risk systematic duplicate
deliveries at the boundary rather than a clean cut.

### Coupling risk: index and predicate must stay textually identical

The V49 index expression and `RaidRepository`'s
`UPDATED_SINCE_EPOCH_SECONDS` field must match Postgres-expression-for-
expression. If they diverge, Postgres will not use the index and the
query silently falls back to a full table scan — no error, just degraded
performance. Both the migration and the repository field carry comments
cross-referencing each other and calling this out explicitly, since
nothing else enforces the match.

### Fan-out join fix included in scope

While implementing the filter (RAID-899), `findAllPublic` and
`findAllEmbargoed` were found to `join(RAID_HISTORY)` and
`distinctOn(RAID.HANDLE)` to get the latest revision per raid, which
fetched every `raid_history.diff` CLOB for every revision of every raid
before discarding all but one row per handle. This was replaced with
`selectFrom(RAID).andExists(...)`, which is semantics-preserving (since
`raid.handle` is the table's primary key, no fan-out or de-duplication is
needed) and drops the CLOB fetch entirely. This was folded into RAID-899
rather than split into its own ticket, because the new index would not
meaningfully help performance while the surrounding query still paid for
a full history cross product per raid.

### One-second granularity is an accepted limitation

`metadata.updated` is whole-second precision. A write landing in the same
second as a client's cursor value may be missed by that client's next
call. This is not fixed here; it is documented as a known limitation.
Client guidance (to be reflected in Federation API consumer docs): subtract
one second from the last-seen cursor before the next call, and de-duplicate
results by handle + version.

### Embargo-to-open transition needed no separate fix

It was confirmed, not assumed, that a service point's embargo-to-open
transition (the access-handler Lambda's `PUT`) already bumps
`metadata.updated`, because it goes through the ordinary
`RaidService.update` → `RaidHistoryService.save` path used by every other
mutation. RAID-902's integration test suite includes a dedicated
embargo-to-open case that exercises this end to end. No additional code
was needed to make a RAiD's transition to open access visible to an
`updatedSince` poller.

# Consequences

Good:

* No schema column added, no JOOQ regeneration risk taken.
* The filter and the exposed `updated` value can never skew apart, since
  they are the same value.
* The expensive `RAID_HISTORY` CLOB fan-out on both list endpoints is gone,
  independent of whether `updatedSince` is even supplied.

Costs and risks:

* One-second granularity means a client polling faster than once per
  second on the same cursor can miss a same-second write; this is
  documented client guidance (subtract one second, de-duplicate), not a
  code fix.
* The index and the jOOQ predicate are two independently-editable pieces
  of text that must stay identical. A future edit to one without the
  other degrades silently (full table scan, no error) rather than failing
  loudly. Both carry cross-referencing comments as the only real mitigation.
* Rows with no `metadata.updated` (pre-existing rows, or a null-metadata
  edge case) fall back to `date_created`, which is coarser and does not
  reflect subsequent updates to those specific rows if their metadata was
  never rewritten to include the field. This only affects legacy data and
  errs towards over-inclusion (returned once extra), never towards
  silently dropping a changed record.
