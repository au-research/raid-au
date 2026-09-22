# RAID-884: Provision scoped service-point-admin roles at boot, not by a manual operator call

- **Ticket:** [RAID-884](https://ardc.atlassian.net/browse/RAID-884) (Bug)
- **Related:** [RAID-827](https://ardc.atlassian.net/browse/RAID-827) (feature left inert), [RAID-721](https://ardc.atlassian.net/browse/RAID-721) (introduced the migration endpoint), [RAID-712](https://ardc.atlassian.net/browse/RAID-712) (scoped roles), [RAID-836](https://ardc.atlassian.net/browse/RAID-836) (spike that proposed the mechanism), [RAID-711](https://ardc.atlassian.net/browse/RAID-711) (epic)
- **ADR:** [`doc/adr/2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md`](../doc/adr/2026-09-11_boot-time-role-provisioning-via-postmigrationevent.md)
- **Date:** 11 September 2026
- **Author:** Rob Leney

## Why

The scoped `service-point-admin:<groupId>` realm roles that authorise the RAID-827 self-serve
credential feature were provisioned in deployed environments only by an operator calling
`POST /realms/raid/group/migrate-service-point-admins` by hand. That step was documented in
`iam/doc/role-permissions.md`, which does not satisfy the hard no-manual-deployment-steps NFR, and
the predictable happened: it was never performed in ARDC's production, so the feature was inert
there from the day it shipped.

Registration Agencies deploy the RAiD Service onto platforms ARDC does not control, so any human
step in the sequence will be skipped somewhere. The one artifact every agency runs regardless of
platform is `raid-iam.jar`, so the provisioning has to live there.

## What changed

**`ServicePointAdminRoleProvisioner`** (new) holds the backfill logic lifted verbatim out of
`GroupController.migrateServicePointAdmins`, plus the role-name, create-if-absent and
approved-membership helpers. Both callers now share it, so they cannot drift.

**`ServicePointAdminRoleBootstrapper`** (new) registers a `PostMigrationEvent` listener from
`GroupControllerResourceProviderFactory.postInit`, which was an empty no-op. On the event it runs
the backfill against every realm, each in its own transaction. Per-realm failures are logged and
swallowed: boot-time provisioning must never stop Keycloak starting.

No realm name is configured. A realm with no flat `group-admin` role (`master`, or anything an
agency runs alongside RAiD) is a no-op, so nothing has to be told which realm is the RAiD one.

**`GroupController.migrateServicePointAdmins`** keeps its operator-only authorisation and its
response shape, but its body is now a delegation to the shared provisioner. It is demoted in the
docs to a redundant safety net, for an operator who needs to re-run the backfill without a restart.
It is deliberately not part of any deployment procedure.

**`iam/doc/role-permissions.md`** section 9's deployment note no longer describes a manual step as
the deployed-environment mechanism.

## The open risk, closed

RAID-836 proposed this mechanism but flagged that nobody had confirmed `PostMigrationEvent`
actually fires in this Keycloak version and configuration - in particular whether it fires on a
boot with no pending schema migration, which the name suggests it might not.

Verified empirically against `quay.io/keycloak/keycloak:26.6.2` (Quarkus) on Postgres, with a
throwaway probe before any real code was written. The event fires on first boot; fires again on
every restart with no migration pending; and fires **after** realm import completes and before the
server accepts traffic. The last property matters as much as the others - a hook running before
import would find no realm to provision into.

## Verification

Unit tests: 190 pass, 0 fail (`./gradlew :iam:test`). Six new tests cover the bootstrapper
(non-migration events ignored, grants applied across realms, no-op for a realm without the flat
role, idempotency, one failing realm neither aborting the others nor propagating, and a realm-listing
failure not propagating). One new test asserts `postInit` registers the listener. The 30 pre-existing
`GroupController` migration tests pass unchanged over the refactored code, which is the regression
evidence for the extraction.

End to end, in an isolated Keycloak container seeded from `raid-realm.json`, reproducing the
production state:

1. Deleted both scoped roles from the `raid` realm - 0 remaining, matching production.
2. Restarted Keycloak. No operator action of any kind.
3. Both roles were recreated and re-granted to exactly their previous holders
   (`raid-au-group-admin`, `rob`). Log: `realm 'raid' - 3 flat group-admin user(s), 2 scoped
   role(s) created, 3 grant(s) added, 0 already present`.
4. `raid-au-unapproved-admin`, a flat `group-admin` who was never approved for their group,
   correctly received nothing - the RAiD-608 / HELP-2844 exclusion survives the move.
5. A third restart changed nothing: `already up to date`, still exactly 2 roles.
6. The `master` realm was a no-op on every boot.

## Deliberately not done

**Roles are not created for groups with no flat `group-admin` member.** The ticket's AC 1 reads
"roles exist for every service point group". Such a role would have no holders and grant nothing,
and groups created through the SPI get theirs at creation time, so this was left alone rather than
widening the change. Flagged rather than silently decided.

**The backfill's breadth is unchanged.** A flat `group-admin` who is an approved member of several
groups still gains the scoped role for all of them, exactly as the endpoint did. Narrowing it is a
decision about who should administer what and must not ride along inside a mechanism change.
Pruning is still untracked: the code comment that deferred it pointed at RAID-712, which is closed
as Done while its AC 1 is not satisfied for three production users
(`simon.gallant@dcceew.gov.au`, `kate.croker@uwa.edu.au`, `katina.toufexis@uwa.edu.au`). That needs
a reopen of RAID-712 or a new ticket.

## Deployment note

No deployment step. The behaviour ships with the IAM image and takes effect the next time Keycloak
starts in each environment. Production already had the backfill run manually on 2026-09-11, so the
first boot there will report `already up to date`.

## PR

[PR #665](https://github.com/au-research/raid-au/pull/665)
