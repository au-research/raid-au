### Provision realm state at Keycloak boot via a PostMigrationEvent listener in the IAM SPI

* Status: final
* Who: proposed and finalised by RL
* When: 2026-09-11
* Related: RAID-884 (this ADR and its implementation), RAID-711 (epic),
  RAID-712 (scoped service-point-admin roles), RAID-721 (the migration endpoint
  this replaces as a mechanism), RAID-827 (the feature left inert by the gap),
  RAID-836 (spike that proposed this mechanism)


# Context

RAiD has a hard non-functional requirement, set by the product owner, that a
deployment must need no manual steps during or after it. Anything a deployment
requires in order to function must provision itself automatically and
idempotently.

Registration Agencies deploy the RAiD Service onto infrastructure ARDC does not
control and cannot inspect: AWS, plain Docker, Kubernetes, or something else per
agency. ARDC controls exactly one artifact that every agency runs regardless of
platform: `raid-iam.jar`, loaded into their Keycloak container.

Until now the scoped `service-point-admin:<groupId>` realm roles were provisioned
in deployed environments by an operator calling
`POST /realms/raid/group/migrate-service-point-admins` by hand, a step written
down in `iam/doc/role-permissions.md`. Documenting a manual step does not satisfy
the NFR, and the predictable happened: the step was never performed in ARDC's own
production, so the RAID-827 self-serve credential feature was inert there from the
day it shipped. Zero of 18 production clients carried a scoped credential.

Realm import is not an answer. `iam/realms/raid-realm.json` is imported on first
boot of a local dev stack only; it is not a deployment mechanism for any
environment and must not be cited as one.

The RAID-836 spike proposed Keycloak's `PostMigrationEvent`, registered from the
`postInit` hook that every `RealmResourceProviderFactory` in the SPI already has
and none of them used. It flagged one open risk: nobody had confirmed the event
actually fires in this Keycloak version and configuration, and in particular
whether it fires on a boot with no pending schema migration - which the name
suggests it might not.


# Decision

Provision boot-time realm state from a `PostMigrationEvent` listener registered in
the IAM SPI's `postInit` hook. The logic lives in the app codebase, in the jar
every agency already runs, not in deployment tooling.

The open risk was closed empirically before building on it, against
`quay.io/keycloak/keycloak:26.6.2` (Quarkus) on Postgres. The event:

* fires on first boot, and
* fires again on every subsequent restart with no schema migration pending, and
* fires **after** realm import completes and before the server accepts traffic.

The third property matters as much as the first two: a hook that ran before
import would find no realm to provision into.

Three constraints on anything using this hook:

1. **Idempotent.** It runs on every boot, not once. Create-if-absent, grant-if-absent.
2. **Failures are contained and swallowed, per realm.** Boot-time provisioning must
   never stop Keycloak starting. A realm that fails is logged and left exactly as it
   was; other realms still run, each in its own transaction.
3. **No realm name is configured.** Every realm is visited, and a realm without the
   state the provisioner keys off (here, the flat `group-admin` role) is a no-op.
   Hard-coding `raid` would be one more thing an agency could get wrong.

The `migrate-service-point-admins` endpoint is retained, delegating to the same
shared code, but demoted to a redundant safety net: a lever for an operator to
re-run the backfill without a restart. It is deliberately not part of any
deployment procedure.

Provisioning breadth is unchanged from the endpoint it replaces. A flat
`group-admin` who is an approved member of several groups still gains the scoped
role for all of them. That over-grant is real and tracked separately; narrowing it
is a decision about who should administer what, and must not ride along
unannounced inside a mechanism change.


# Consequences

Good:

* The scoped roles exist wherever the RAiD Service runs, with no operator action,
  on any platform. Verified end to end: scoped roles deleted from a seeded realm
  were restored exactly - same roles, same holders - by a restart alone, and a
  third boot changed nothing.
* Any future "must exist before this feature works" realm state has an established,
  verified home. RAID-836's federation-harvest client is the next candidate, and
  can reuse the mechanism rather than re-litigating it.
* The endpoint and the boot hook share one implementation
  (`ServicePointAdminRoleProvisioner`), so they cannot drift.

Costs and risks:

* Every boot does a small amount of realm work. It is a no-op read when there is
  nothing to do, but it is not free, and it scales with the number of flat
  `group-admin` holders.
* Provisioning that fails at boot fails quietly, by design. Its log line is the
  only signal, so anything added here needs a log message an operator can actually
  act on.
* `PostMigrationEvent` is verified for Keycloak 26.6.2. A Keycloak major upgrade
  should re-verify it rather than assume it, since the whole mechanism rests on it.
