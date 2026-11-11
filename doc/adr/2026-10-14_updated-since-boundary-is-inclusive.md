### Make the Member API `updatedSince` boundary inclusive (`>=`)

* Status: final
* Who: decided by ML (2026-10-06), implemented by RL
* When: 2026-10-14
* Related: RAID-939 (story), RAID-837 (original filter),
  [2026-09-29 ADR](2026-09-29_updated-since-filter-on-materialised-metadata.md)
  (superseded in part by this record)

# Context

RAID-837 added `updatedSince` to `/raid/all-public` and
`/raid/all-embargoed` with a strictly-after (`>`) boundary. The value it
filters on, `metadata.updated`, has one-second resolution. With a strict
boundary, a RAiD updated in the same second as a consumer's last sync is
missed unless the consumer remembers to subtract one second from its
cursor. The Federation API's own `updatedSince` is already inclusive, so
the two APIs behaved differently for the same parameter name.

# Decision

The boundary is inclusive: a RAiD whose `metadata.updated` equals the
supplied `updatedSince` (truncated to whole seconds) is returned. Both
endpoints change from `gt` to `ge` on
`RaidRepository.UPDATED_SINCE_EPOCH_SECONDS`. The filter expression is
unchanged, so the V49 index still applies.

Other Registration Agencies are not notified separately, because none is
in production yet. The change ships as part of the normal software stack,
as RAID-837 did.

# Consequences

Good:

* A consumer that passes its last-seen `updated` value as the next
  `updatedSince` can no longer miss a same-second write.
* The Member API and Federation API now agree on what `updatedSince`
  means.

Costs and risks:

* A RAiD at the boundary is returned again on the next sync. Consumers
  must de-duplicate by identifier (handle and version). This replaces the
  previous guidance to subtract one second from the cursor.
* No consumer depended on the strict boundary when this was decided. The
  Federation ingest and the static landing page build both fetch
  `/raid/all-public` (and the static build `/raid/all-embargoed`) in full,
  without `updatedSince`, so this change does not affect them.
