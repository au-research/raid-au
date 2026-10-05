### Verify a related object link when it is entered, not on every save

* Status: final
* Who: decided by RL
* When: 2026-10-05
* Related: RAID-935 (this change), RAID-885 (Web Archive check reworked to a
  HEAD on the playback link), HELP-3215 (Web Archive rate limiting on save),
  RAID-809 (resolver unavailable is a 503, not a validation failure)

# Context

Every create and update of a RAiD sent each `relatedObject` link to its
external resolver to confirm it exists. On an update that meant every link on
the RAiD was checked again, even when the user had only changed the title.

**Observed**

* HELP-3215 was a false "uri not found" for Web Archive links, fixed in RAID-885.
* On 2026-10-05 a probe of the Internet Archive from one office IP address was
  throttled, by refused TCP connections, after about 14 to 20 requests.

**Inferred, not tested against RAiD**

* A save that re-checks many Web Archive links could be throttled in the same
  way and so return a 503, for an edit that did not touch the links.

# Decision

1. **Skip unchanged links on update.** On `PUT`, a related object is not sent
   to its resolver when its exact `(schemaUri, id)` pair is on the stored
   version the client edited. The stored version is read with
   `RaidHistoryService.findByHandleAndVersion`, the same call
   `RaidService.update` uses to rebuild it. It is not read from
   `raid.metadata`, which can be null, and not with
   `RaidHistoryService.findByHandle`, which can write a new history version.
2. **Local checks still run on every link.** Format, the Web Archive capture
   year, type, category and the `schemaUri` allow-list are checked for every
   item, unchanged or not. Only the call to the external resolver is skipped.
3. **Any doubt means check everything.** If the handle check failed, the
   version or related objects are missing, the history is empty, or reading it
   throws, the stored set is empty and every link is checked, as before.
4. **Success cache.** A successful resolver check is remembered in a bounded,
   expiring, per-instance in-memory cache (Caffeine), on create and on update,
   for related objects only. A link in the cache is treated like an unchanged
   one. Failures and unavailable resolvers are never cached, so a link that is
   archived later is checked again. A save retried after a 503 therefore skips
   the links the first attempt confirmed, and each retry makes progress. The
   cache is not shared between API instances: a retry that reaches another
   instance starts empty but still makes progress there. Expiry and size are set
   by `raid.uri-validation.success-cache.expire-after-write` and `.maximum-size`.
   A zero value disables it.
5. **Stored links are accepted.** Every stored `(schemaUri, id)` pair is
   treated as accepted, including Web Archive links stored before the existence
   check (2.15.0). "Stored" does not mean "validated": links written by the
   legacy and upgrade endpoints, and by environment SQL migrations that edit
   `raid_history`, are skipped in the same way. There is no tracking column, no
   migration and no backfill.
6. **Exact match only.** "Unchanged" is an exact string match on `schemaUri`
   and `id`. A difference in case, `http` against `https` or a trailing slash
   counts as a change and costs one extra resolver call. The `schemaUri` is part
   of the key because the DOI pattern also accepts `web.archive.org` links.
7. **Every scheme in the dispatch map is covered**, not only DOI, Handle, RRID
   and Web Archive. Any validator added to `RelatedObjectValidator` later, such as
   ARK in RAID-793, must implement `validateLocally`.

# Consequences

* The guarantee for an accepted link changes from "verified on every save" to
  "verified when it was entered". This is the intended trade.
* A link can stop resolving after it was accepted (link rot) and the RAiD can
  still be saved with it. Nothing re-checks stored links on save. A periodic
  report of stored links is a possible later step and is not part of this
  decision.
* A bulk edit that adds many new Web Archive links in one save could still be
  rate limited, because every new link is checked. A retry no longer repeats
  the links that already passed.
* The loop over related objects is sequential. With the cache on, a duplicate
  valid link in one request is checked once, because the second copy is a cache
  hit. With the cache off, or when the link fails, it is checked each time.
  Deduplicating within a request regardless of the cache is deferred. The UI
  already blocks duplicates in a bulk upload.
* Contributors and organisations are unchanged and are still checked on every
  save.
* The cache's default expiry (30 minutes) and size (10,000 entries) are chosen,
  not measured. Revisit them with real retry and rate limit data.
* Adding Caffeine to the classpath would make Spring Boot swap the `@Cacheable`
  schema lookups to Caffeine, so `spring.cache.type` is pinned to `simple` to
  leave them as they were.
* The cache is shared across users and service points on an instance, so a
  negligible timing signal exists: a faster save can reveal that someone else
  recently submitted the same link.
* Each `PUT` with related objects now rebuilds the stored version from history
  one more time, to read its related objects.
