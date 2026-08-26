# RAID-849: Cap client credentials per service point, defer rate limiting

- Ticket: [RAID-849](https://ardc.atlassian.net/browse/RAID-849) (parent
  [RAID-827](https://ardc.atlassian.net/browse/RAID-827), epic
  [RAID-711](https://ardc.atlassian.net/browse/RAID-711))
- Sibling sub-tasks: [RAID-845](https://ardc.atlassian.net/browse/RAID-845),
  [RAID-846](https://ardc.atlassian.net/browse/RAID-846),
  [RAID-847](https://ardc.atlassian.net/browse/RAID-847),
  [RAID-848](https://ardc.atlassian.net/browse/RAID-848)
- Follow-up raised: [RAID-851](https://ardc.atlassian.net/browse/RAID-851)
- Source investigation: [RAID-767](https://ardc.atlassian.net/browse/RAID-767)
- PR: [au-research/raid-au#624](https://github.com/au-research/raid-au/pull/624)
- ADR: `doc/adr/2026-08-26_scoped-client-credential-quota.md`

## What changed and why

RAID-827 adds Keycloak SPI endpoints letting a Service Point Admin self-serve
their own service point's API client credentials (create, list, rotate, revoke,
get-secret) through a broker, since a Service Point Admin's own token carries
none of the `realm-management` permissions needed to create a Keycloak client.
The story listed "decide and document rate-limiting behaviour for these
endpoints" as an open task. RAID-849 is that decision.

This is a documentation-only change: an ADR. No code or configuration was
touched. The implementation consequence lands in RAID-846 and RAID-848.

## The decision

**Defer rate limiting until there is a demonstrated need.** Not because the
risk is nil, but because the cost is real and the need is unproven. Capping the
number of clients is markedly cheaper than limiting request rate, so do that
instead: at most 10 active non-revoked credentials per service point, enforced
in application code beside the isolation checks, rejecting excess creates with
409 Conflict. Revoking frees a slot.

409 rather than 429 because the cap is a standing limit on stored objects, not
a rate, and overloading 429 would leave no clean status code if a real rate
limit is ever added.

The ADR records what would constitute demonstrated need, so the deferral stays
reviewable: credentials repeatedly hitting the cap at automation-like rates,
rotate or get-secret volume beyond what human self-service explains, an agency
operator reporting abuse, or the endpoints being opened to a broader caller
set. The RAID-847 audit records are already the instrument that would show the
first three, so no separate monitoring work is needed.

## Deployment portability drove this

Most RAiD registration agencies are not deployed on AWS. Only controls living
in the application and its shipped configuration reach every deployment;
anything built in the AU account protects the AU deployment alone.

That is the main argument for putting the cap in application code, and it is
also why edge rate limiting is explicitly **not** RAiD's answer here. For the
record, the AU deployment has no WAFv2 WebACL anywhere in
`raido-v2-aws-private`, no rate-based rules, no API Gateway throttling, and no
CloudFront or ALB request limiting, with `iam.<zone>` reached directly through
the internet-facing ALB with no CloudFront hop where a WebACL could attach.
That gap could be closed for AU but cannot be closed for anyone else, so the
software must not depend on an edge layer existing.

## Why building it in the SPI is not cheap

- The `iam` module has no filter infrastructure at all: no
  `ContainerRequestFilter`, no `ContainerResponseFilter`, no servlet filters,
  only `@Provider`-annotated JAX-RS resources.
- There is no working per-endpoint SPI configuration mechanism. Every
  `RealmResourceProviderFactory` has an empty `init(Config.Scope)` body and
  `getResource()` builds a fresh controller per request, so `Config.Scope`
  never reaches the resource. `GroupController` works around this by reading
  `System.getenv("RAID_FLAT_GROUP_ADMIN_FALLBACK")`.
- Controller-per-request means any counter must be `static`, bringing its own
  eviction design.

## Impact on sibling sub-tasks

- **RAID-846** gains the cap check and the 409 response.
- **RAID-848** gains positive and negative coverage: creating up to the cap
  succeeds, the create that would exceed it returns 409, and revoking frees a
  slot so a subsequent create succeeds.

## Follow-up

**[RAID-851](https://ardc.atlassian.net/browse/RAID-851) (High)** — enable
Keycloak realm brute-force protection. This survives the "defer until
demonstrated need" test because its implementation cost is approximately zero:
the settings already exist in the realm JSON and are simply switched off
(`bruteForceProtected: false`, which makes the accompanying `failureFactor: 30`
and wait settings inert). It is also portable, since realm configuration ships
with the software. It matters because `client_credentials` token attempts
against the very credentials this feature mints are currently unthrottled at
Keycloak's token endpoint. Note the realm config is duplicated, with the
authority environments' copy in raid-au and the agency deployment shipping its
own in raido-v2-aws-private, also with brute-force disabled.

RAID-850, originally raised here to add a WAFv2 rate-based rule, was descoped.
It is rate limiting, for the same unproven need, benefiting one deployment, so
it fails the same test as the main decision.

## Left unverified

Deployment topology and instance counts, for AU and for agency deployments.
This only matters if rate limiting is revisited: a `static` in-memory counter
is sound on a single Keycloak instance and only becomes per-node theatre across
several, so the right implementation would depend on facts not yet gathered.
Also unestablished is how a non-AWS agency deployment picks up realm
configuration changes, which bears on RAID-851.
