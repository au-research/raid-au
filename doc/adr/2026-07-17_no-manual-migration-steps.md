### No manual migration steps between versions

* Status: proposed
* Who:  proposed by ML
* When: proposed on 2026-07-17
* Related: no related ADRs


# Decision

No migration step between versions may exist only as written instructions
for an operator to carry out by hand. Every step (config generation, data
backfill, cleanup, etc.) must be a script committed to the repo alongside
the change that needs it.

Scripts may still be manually triggered — they just can't be manual in what
they do. This covers forward migrations only; rollback/downgrade is a
separate discussion.

Flyway-managed database schema migrations already work this way — scripted,
ordered, tracked — and count as a partial existing implementation of this
decision, not something new to invent.


# Context

An instance several versions behind was upgraded using a manually-written
procedure. It omitted a step an interim version required: creating a config
file. That step existed only in someone's memory, not in code. The instance
stayed down until the person who remembered it could respond, delayed by
timezone differences with whoever was doing the upgrade.

The instance was a demo, so the downtime was harmless. On production it
would have been a real incident.

Manual steps in prose get missed, drift out of sync with new versions, and
make upgrades depend on one person's availability.

This repo is public and used by every RAiD Registration Agency, not just
RAiD AU. Operators running other agencies' instances won't all have the
same ops sophistication or repo familiarity as this team, so migration
scripts need to be usable without insider knowledge.


# Consequences

* Manual steps become scripts, committed with the change that needs them.
* Upgrades reduce to "run these scripts," not "read and do these things."
* Slightly more upfront work per migration, for reliable upgrades that
  don't depend on anyone's memory.
* Until an automated check exists, `doc/development/release-process.md`
  should gain a checklist item to verify no manual step is required. This
  is a stopgap, not real enforcement — it's still a human remembering to
  check.


# Open questions

Decided as requirements; mechanism to be proposed by devs:

* It must be obvious to an operator which scripts apply to their upgrade —
  devs to propose how (e.g. a manifest/registry).
* Migrations should recover gracefully from a partial failure or re-run,
  rather than leaving an instance inconsistent.
* An upgrade spanning multiple versions should run each interim version's
  script automatically, in order. Feasibility needs confirming.

Not yet decided — for discussion with the devs:

* Scope — does this cover only app/DB-level migrations (config files, data
  backfills, Flyway), or also infra-adjacent config such as Keycloak realm
  or SATOSA/SSO config?
* How to handle steps that need environment-specific secrets a script
  can't derive on its own.
* Whether/how to prevent multiple instances racing to run the same
  migration under a rolling deployment.


# Links

* doc/adr/2022-07-21_adr.md — the ADR process this record follows
* doc/development/release-process.md — candidate home for an interim
  enforcement checklist item
