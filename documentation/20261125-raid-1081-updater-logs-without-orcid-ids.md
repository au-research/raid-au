# RAID-1081: ORCID updater logs without ORCID iDs

- JIRA: [RAID-1081](https://ardc.atlassian.net/browse/RAID-1081), sub-task of
  [RAID-976](https://ardc.atlassian.net/browse/RAID-976) (epic
  [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Branch: `feature/RAID-1081` into `main`
- Status: in review
- Related: the infrastructure half of RAID-1081 (test's API pointed at test's updater) is
  raido-v2-aws-private #89, merged 2026-11-25

## Why

The first real sync in test showed the updater writing contributors' ORCID iDs to its log, for
example `EntryOutcome[orcidId=https://sandbox.orcid.org/…, outcome=NOT_AUTHENTICATED]`. Logs
record what happened, with counts, not whose record it was.

## What changed

- **Work queue:** a RAiD sync logs how many contributors had each outcome
  (`{NOT_AUTHENTICATED=2}`). Status pushes and the retry, give-up and unexpected-failure lines
  for a STATUS item say "STATUS for a contributor" instead of the iD, which is that item's key.
  RAID_SYNC items still name their RAiD handle.
- **RAiD API client:** failure messages say "setting a contributor's status" and "listing a
  contributor's RAiDs"; they are logged and stored with the work item.
- **ORCID writes and researcher deletion:** log lines keep the RAiD handle and put code and drop
  the iD. Deletion errors that the error handler logs no longer name the iD.
- **Invitations and sign-in:** log lines keep the invitation id and RAiD handle. Sign-in logs
  whether the authenticated iD matched the typed one, not the two iDs.
- **ORCID error text:** a new `OrcidIdRedactor` replaces any ORCID iD with `[ORCID iD]` in
  ORCID's developer message, the Location header and connection errors, whose URLs carry the iD.

Requests and responses are unchanged. The iD is still stored where the updater needs it (work
items, entries, researchers), so an operator can look a case up in the database.

## Testing

- `:orcid-updater:test`: 257 tests, 0 failures (10 new: work queue, invitation and sign-in log
  assertions that the iD is absent, ORCID error redaction, `OrcidIdRedactor`).
- `:orcid-updater:intTest`: 54 tests, 0 failures, 1 skipped (as before).
