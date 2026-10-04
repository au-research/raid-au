# RAID-812: mock API server for RAiD test environments (proposal)

- JIRA: [RAID-812](https://ardc.atlassian.net/browse/RAID-812) (Task)
- Related: [RAID-892](https://ardc.atlassian.net/browse/RAID-892), [RAID-893](https://ardc.atlassian.net/browse/RAID-893), [RAID-835](https://ardc.atlassian.net/browse/RAID-835), [RAID-809](https://ardc.atlassian.net/browse/RAID-809)
- Status: draft for review, 5 October 2026
- PR: [#698](https://github.com/au-research/raid-au/pull/698)

Labels used below: **Verified** means checked against `origin/main` source on 2 October 2026 or a primary web source. **Unverified** means an assumption that still needs confirming.

## Recommendation

Run a WireMock 3.x container as a sidecar in every branch API task, and point the API's DataCite repository and DOI endpoints at it. Branch deploys then stop creating real DataCite repositories and stop minting real DOIs on DataCite's test instance. Move the existing local and CI MockServer fixtures onto the same WireMock mappings so every test environment shares one set of fixtures. Keep exactly one deliberate, live contract check against DataCite test so the mocks cannot drift unnoticed.

The ticket suggested Imposter. It is a capable tool, and its Go engine (v5) can run on AWS Lambda, which would keep the mock off the branch ECS hosts. But v5 publishes no licence, and it has no request journal. WireMock is the better fit today. See [Tool comparison](#5-tool-comparison) and [Imposter v5 on AWS Lambda](#7-alternative-imposter-v5-on-aws-lambda).

## 1. The problem

RAiD's test environments call live third-party services. The most costly case is DataCite.

- **Verified.** Each branch deploy runs the `Configure-ServicePoints` pipeline action, which makes two `POST /service-point/` calls: "RAiD AU (branch-…)" and "RAiD AU Test Registry 2" (`branch-configure-service-points-project.ts`, raido-v2-aws-private).
- **Verified.** Every service point creation calls DataCite's `/repositories` API through `ServicePointService.create` → `RepositoryService` → `DataciteRepositoryClient`. The default URL is `https://api.test.datacite.org/repositories`, and the branch stack does not override it.
- **Verified.** Branch-Cleanup has no DataCite step, so these repositories are left behind (RAID-892 is addressing this).
- **Verified.** DataCite reports that each new repository reserves a prefix from its shared pool. Deleting the repository does not return the prefix to the general pool (email from Cody Ross, DataCite, 9 September 2026, recorded on RAID-892).
- **Verified.** Branch intTests and e2e tests mint RAiDs against the branch API, which POSTs to `https://api.test.datacite.org/dois` (`branch-api-stack.ts`) with `publish` or `register` events. These are not drafts. A repository that contains a DOI cannot be deleted.

RAID-892 cleans up after the fact. This proposal stops branch environments from calling DataCite at all, so there is nothing to clean up.

## 2. Current state

RAiD already uses three test-double mechanisms, and each environment combines them differently.

### In-memory stubs inside the API

`ExternalPidService` swaps in stub beans when `raid.stub.<name>.enabled=true`. Stubs exist for DOI, Handle, RRID, web archive, GeoNames, OpenStreetMap, ORCID, ROR and ISNI. Every stub defaults to `false`, and the CDK enables all of them in the test and branch environments. The stub classes ship in the production JAR, and RAID-809 showed how easily that toggle can be misconfigured.

### MockServer 5.15.0 in docker-compose

`api-svc/raid-api/docker-compose.yaml` runs MockServer on port 1080, preloaded from `docker-compose/mockserver/expectations.json`. It covers ISNI, ROR, ORCID, DataCite `/dois` (POST and PUT, matched on Basic-auth credentials) and the ORCID-integration service. It has **no `/repositories` expectation**. Local development and the GitHub Actions PR-Integration-Tests and PR-E2E-Tests jobs use it. Deployed environments do not.

### What each environment reaches live

| Service | Local and GitHub Actions | Branch envs | Test env |
|---|---|---|---|
| DataCite `/dois` | MockServer | **Live** (DataCite test) | **Live** (DataCite test) |
| DataCite `/repositories` | Live default, but no test triggers it | **Live, 2 per deploy** | Live when an operator creates a service point |
| ORCID-integration service | MockServer | **Live** (`orcid.demo.raid.org`) | Not checked |
| ORCID, ROR, ISNI | MockServer | In-memory stub | In-memory stub |
| DOI, Handle, RRID, OSM, GeoNames, web archive | In-memory stub | In-memory stub | In-memory stub |
| ROR and ORCID lookups from the browser | Live | Live | Live |

Sources: `application.yaml`, `application-dev.yaml`, `environment-properties.ts`, `branch-api-stack.ts` and the frontend ROR and ORCID components, all on `origin/main`.

## 3. Goals and non-goals

### Goals

- Branch environments make no write calls (repositories or DOIs) to DataCite's live test instance.
- One set of mock fixtures, shared by local development, GitHub Actions and branch environments.
- Tests can simulate failures (429, 5xx, timeouts) for the resolver-unavailable behaviour added in RAID-809.
- Tests can assert on the requests the API sent. `DataciteRelatedRaidMockIntegrationTest` already does this through MockServer's `/retrieve` API.
- No manual deployment steps. The mock and its fixtures deploy with the branch stack.

### Non-goals

- Changing demo, stage or prod. They keep their real integrations.
- Mocking calls the browser makes directly. Playwright's `page.route` is the right tool for those, and `service-point-group-id-error.spec.ts` already uses it.
- Removing the in-memory stubs in this phase. See [Deferred](#9-deferred-with-revisit-triggers).

## 4. Requirements for the tool

| Requirement | Why |
|---|---|
| Runs as a container on ECS and in docker-compose | Branch envs run on ECS. Local development and CI use compose. |
| Templated responses | `POST /repositories` must return a unique symbol and prefix for each call. `POST /dois` must echo back the DOI from the request. |
| Some state (desirable) | Branch envs set `raid.datacite.resync.enabled=true`. If resync reads DOIs back, the mock must return what was created. **Unverified:** whether resync issues GETs. |
| Request journal and verification API | Lets intTests assert on the payloads sent to DataCite. |
| Fault and latency injection | Tests the RAID-809 503 paths and the RAID-731 429 handling. |
| Small footprint | Branch Api ASG `MaxSize` is 3, and capacity has deadlocked deploys before. |
| Permissive licence and active maintenance | The mock becomes long-lived shared test infrastructure. |

## 5. Tool comparison

| | MockServer (current) | WireMock 3.x | Imposter |
|---|---|---|---|
| Licence | Apache-2.0 | Apache-2.0 | v4: LGPL-3.0 with the "Commons Clause" condition. v5: no licence file in the repository (checked 5 October 2026) |
| Latest stable release | 5.15.0, 11 January 2023 | 3.13.2, 14 November 2025. 4.0 in beta (beta.39, 24 September 2026) | JVM v4.7.0, 14 July 2025. Go-based v5 in a separate repo (v5.21.3, 30 July 2026) |
| Adoption (GitHub stars, October 2026) | Not checked | About 7,400 | About 415 (v4); 6 (v5 repo) |
| Templated responses | Velocity, Mustache, JavaScript | Handlebars, with random-value helpers | Templates plus JavaScript or Groovy scripts |
| State | None built in | Scenarios built in. Richer state through an extension | Stores built in (in-memory, Redis, DynamoDB) |
| OpenAPI-driven mocks | Can initialise from a spec | Not in the open-source edition | First-class plugin |
| Request verification API | Yes (already used) | Yes (`/__admin/requests`) | Partial (store API, capture) |
| Fault injection | Yes | Yes, including connection faults | Yes |
| Footprint | JVM | JVM | v5 is a single Go binary |
| Runs on AWS Lambda | No | Not natively | Yes, v5 detects Lambda at runtime. See section 7 |
| Migration from current fixtures | None | Rewrite about 600 lines of JSON | Rewrite about 600 lines of JSON |

### Assessment

- **MockServer** needs no migration, but its last release was nearly four years ago. Extending it into deployed environments would mean depending further on a dormant project.
- **Imposter** has the best built-in state and OpenAPI support, the v5 Go binary is the lightest option, and v5 can run on AWS Lambda, which takes it off the branch ECS hosts entirely (section 7). Against it: v5 publishes no licence, v4's Commons Clause means it is not open source in the usual sense, the user base is small, and the engine change from JVM v4 to Go v5 is still settling. RAiD would be an early v5 adopter.
- **WireMock** is the most widely used and most actively maintained, uses a permissive licence, and covers every requirement except rich state. Its built-in scenarios are enough for create-then-read. If resync turns out to need more, the state extension or a small response transformer covers it.

WireMock is the recommendation. Imposter v5 on Lambda is the strongest alternative if sidecar memory turns out to be a problem, but only once its licence position is clear.

## 6. Proposed design

### Where the mock runs

- **Branch envs:** a sidecar container in the `BranchApi` ECS task definition. The API reaches it on `localhost`, so it needs no ALB rule, DNS record or security group change. It starts and stops with the branch, so it needs no cleanup and no shared state between branches. Give it a capped journal (`--max-request-journal-entries`) to bound memory.
- **Local and GitHub Actions:** replace the `mockserver` service in `api-svc/raid-api/docker-compose.yaml` with WireMock on the same port 1080, so `application-dev.yaml` keeps its current URLs.
- **Fixtures:** keep a single `mappings/` directory in raid-au, baked into a small image (`wiremock/wiremock` plus mappings) built by the same pipeline that builds the API image. Fixtures then version with the code that depends on them. Branch environments pull their CDK from raido-v2 main, so this keeps fixture changes out of the CDK repo.

### What the API is pointed at, in branch envs

| Property | Today | Proposed |
|---|---|---|
| `raid.repository-client.url` | Default, `api.test.datacite.org/repositories` | `http://localhost:1080/repositories` |
| `datacite.endpoint` | `https://api.test.datacite.org/dois` | `http://localhost:1080/dois` |
| `raid.orcid-integration.host` | `https://orcid.demo.raid.org` | `http://localhost:1080` (Phase 2) |

### Keeping the mock honest

A mock that never meets the real service drifts. Two existing mechanisms keep the contract checked against DataCite:

- The test environment (`api.test.raid.org.au`) keeps the real DataCite test instance for DOIs.
- `DataciteLiveRelatedRaidIntegrationTest` already runs against DataCite test when `DATACITE_LIVE_TEST=true`, creating a draft and deleting it. Running it on a schedule, and not on every branch, keeps live traffic small and deliberate.

## 7. Alternative: Imposter v5 on AWS Lambda

Imposter v5 is the only candidate that runs on Lambda without extra work. On Lambda the mock uses no ECS capacity at all, which removes the sidecar's main risk. This section is based on the v5 source and examples on GitHub (v5.21.3, 30 July 2026). Nobody has deployed it in RAiD's accounts yet.

### How Imposter runs on Lambda

- **Verified. Runtime detection.** The same binary serves HTTP or Lambda. `cmd/imposter/main.go` picks the Lambda adapter when it detects Lambda (`AWS_LAMBDA_FUNCTION_NAME` is set), so no separate build or wrapper is needed.
- **Verified. Runtime.** The official example deploys to the `provided.al2023` custom runtime, with the binary named `bootstrap` and handler `bootstrap`. It builds from source with `GOOS=linux GOARCH=amd64 go build -tags lambda.norpc`. Releases ship `linux_amd64` and `linux_arm64` tarballs, but no Lambda-specific package. **Unverified:** whether the released binary works on Lambda as-is or must be rebuilt with that tag.
- **Verified. Configuration.** Mock config is bundled in the deployment zip. If `IMPOSTER_CONFIG_DIR` is not set, the Lambda adapter defaults to `/var/task/config`, so a zip containing `bootstrap` and a `config/` folder works with no settings. Config files can read environment variables with `${env.NAME}`.
- **Verified. Accepted event types.** API Gateway REST proxy (v1) events and Function URL or API Gateway HTTP API (v2) events. Anything else gets a 400 "Unsupported request type". **Unverified:** ALB target events carry `httpMethod`, so they would take the v1 path, but nobody has tested whether ALB accepts the response shape.
- **Verified. Published performance.** The maintainer's own test at 128 MB reports an average cold start of 124 ms (P99 139 ms) and an average warm duration of 10.8 ms. RAiD has not reproduced these figures.

### State: in-memory stores do not work on Lambda

Each Lambda execution environment has its own memory. Concurrent requests can land on different environments, and environments are recycled, so data saved in memory by one request may be missing on the next. Any flow that creates something and later reads it back must use the DynamoDB store:

| Setting | Value |
|---|---|
| `IMPOSTER_STORE_DRIVER` | `store-dynamodb` |
| `IMPOSTER_STORE_DYNAMODB_TABLE` | Table name (required) |
| `IMPOSTER_STORE_DYNAMODB_TTL` | Item lifetime in seconds, for example `86400` |
| `IMPOSTER_STORE_DYNAMODB_TTL_ATTRIBUTE` | Defaults to `ttl` |
| `IMPOSTER_STORE_KEY_PREFIX` | Optional. Only needed if branches share one table |
| Table key schema | `StoreName` (partition, string), `Key` (sort, string), pay per request, TTL on `ttl` |

These names come from `internal/store/dynamodb_store.go`. One example script in the repository uses the older `IMPOSTER_DYNAMODB_*` names, which the current code does not read.

### Request verification is weaker

**Verified.** v5 exposes only `/system/status` and `/system/store`. It has no request journal. A test that checks what the API sent to DataCite (today, `DataciteRelatedRaidMockIntegrationTest`) needs the mock config to save each request body into a store explicitly, and the test then reads it back through `/system/store`. That works, but every assertion costs extra config, whereas WireMock records every request automatically.

### How the branch API would reach it

The API calls DataCite with a plain `RestTemplate` and Basic auth, so whatever sits in front of the Lambda must accept unsigned HTTP calls.

| Option | For | Against |
|---|---|---|
| Function URL, auth `NONE` (the official example) | Simplest. No API code change, only a URL property. | Public internet endpoint. RAiD has no rate limiting anywhere, so the only limit is a reserved-concurrency cap on the function. |
| Function URL, auth `AWS_IAM` | Not public. | The API would have to SigV4-sign its DataCite calls, which puts test-only code in the production client. Not recommended. |
| Private API Gateway (REST) through an `execute-api` VPC endpoint | Not public, and no API code change. | Adds an interface VPC endpoint to the test VPC, plus API Gateway resources for each branch. **Unverified:** whether the test VPC already has this endpoint. |
| Internal ALB with a Lambda target | Not public, and reuses familiar ALB patterns. | Event compatibility untested (see above). |

### What the CDK would look like

- A `lambda.Function` in the BranchApi stack: `Runtime.PROVIDED_AL2023`, handler `bootstrap`, code from an asset folder holding `bootstrap` and `config/`, ARM64 to match the release binary.
- A pay-per-request DynamoDB table in the same stack with `RemovalPolicy.DESTROY`, so Branch-Cleanup's existing stack destroy removes both. No new cleanup phase is needed.
- The chosen front door from the table above, and the two URL overrides from section 6 pointing at it.
- **Fixture delivery is the awkward part.** Branch pipelines take their CDK from raido-v2 `main`, while the fixtures should version with raid-au. The pipeline would need to package the fixtures from the raid-au source artefact (for example, into S3 per branch build) for the function code to use. The sidecar design avoids this by baking fixtures into an image built alongside the API image.

### Lambda compared with the ECS sidecar

| | WireMock sidecar (recommended) | Imposter v5 on Lambda |
|---|---|---|
| Branch ECS capacity | Adds JVM memory to every branch task | None |
| Network exposure | `localhost` only | Public URL, or extra private plumbing |
| Extra AWS resources per branch | None | Function, table, front door |
| State across requests | In memory, one process | Needs DynamoDB |
| Request verification | Automatic journal | Explicit capture into stores |
| Same fixtures in local and CI | Yes (WireMock container) | Yes (Imposter container) |
| Cleanup | Dies with the task | Destroyed with the stack |
| Licence | Apache-2.0 | None published |

> **Licence gap.** The imposter-go repository has no LICENSE file, its README states no licence, and GitHub reports none. Without a published licence, ARDC's right to use the software is unclear. This is a question for ARDC's legal or procurement contacts, not one this proposal can answer. Asking the maintainer to publish a licence is a sensible first step. Until it is resolved, the Lambda option should not go ahead.

### When to choose Lambda

Choose Imposter on Lambda if both of these hold: the Phase 1 measurement shows the WireMock sidecar pushes branch deploys into the capacity limit, and imposter-go has a licence ARDC accepts. Otherwise the sidecar is simpler, needs fewer resources and keeps the mock off the internet.

## 8. Phased delivery

Each phase can be its own sub-task under a delivery story. The order matters: Phase 1 removes the external harm, and the later phases are consolidation.

1. **DataCite in branch envs.** Add WireMock mappings for `/repositories` and `/dois`, the sidecar in `branch-api-stack.ts`, and the two URL overrides. Measure sidecar memory on a real branch deploy. Acceptance: a branch deploy creates no repository in DataCite account ATHH, and branch intTests and e2e pass. Needs a raido-v2 CDK change merged to main before branch pipelines pick it up.
2. **Converge local, CI and branch fixtures.** Port `expectations.json` to WireMock mappings, swap the compose service, and move the two intTests that read MockServer's `/retrieve` onto WireMock's request API. Point the branch ORCID-integration host at the sidecar.
3. **Optional: ORCID, ROR and ISNI through the mock in branch envs.** This turns off those three in-memory stubs in branch envs, so branch matches CI. Only worth doing if Phase 2 shows the fixtures are easy to maintain.

### Relationship to RAID-892

RAID-892 still needs to delete the repositories already left in ATHH and stop more from accumulating until Phase 1 lands. Once Phase 1 is live, branch deploys create no repositories, so the RAID-892 teardown step only has to cover environments created before that point. The two tickets should agree on that boundary so the teardown step is not built for a case that no longer exists.

## 9. Deferred, with revisit triggers

- **Removing the in-memory stubs from the production JAR.** DOI, Handle, RRID and OpenStreetMap validators send HEAD requests to whatever URI the user submitted, so an external mock can only intercept them as a forward HTTPS proxy, which means trusting a mock CA in the JVM. GeoNames goes through a library with no base-URL property. Revisit if another RAID-809-style stub misconfiguration reaches stage or prod.
- **A shared mock service for all branches** in place of per-task sidecars. Revisit if sidecar memory pushes branch deploys into the ASG capacity limit.
- **Mocking browser-side ROR and ORCID calls in e2e.** Revisit if those external calls cause e2e failures.

## 10. Risks

- **Fixture drift.** DataCite changes its API and the mock does not. Mitigated by the scheduled live contract test in section 6.
- **Branch capacity.** A JVM sidecar adds memory to every branch task. Mitigated by measuring in Phase 1, with Imposter v5 on Lambda (section 7) or a shared service as fallbacks.
- **Hidden dependence on real DOIs.** **Unverified:** whether any branch test resolves a minted DOI through doi.org or reads it back from DataCite. Phase 1 must check this before switching.
- **Fixture and seed coupling.** Today the `/dois` expectations match Basic-auth credentials that the `db/env/dev` Flyway seeds also contain (`V32.1`, `V40.1`), so changing one side silently breaks the other. The new mappings should match on path and body, not credentials.

## 11. Separate findings, not in scope

These came up during the survey and need their own tickets:

- **Verified.** `expectations.json` contains Base64 Basic-auth headers, including one for `ATHH.KFOHZB`, a real DataCite test repository symbol. Whether the password with it is still valid has not been checked. This needs a security review and possibly a credential rotation.
- **Verified.** Branch envs send ORCID-integration traffic to the shared demo service `orcid.demo.raid.org`. **Unverified:** what that service does with branch traffic, for example whether it writes to demo data stores.
- **Unverified.** The CDK spells `raid.stub.geonames.enabled` and `raid.stub.openstreetmap.enabled`, while the Java fields are `geoNames` and `openStreetMap`. Spring relaxed binding may not map these, which would be another RAID-809-style inert flag. Check the startup log for "using the in-memory" lines.

## Sources

- raid-au and raido-v2-aws-private, `origin/main`, read 2 October 2026
- JIRA [RAID-892](https://ardc.atlassian.net/browse/RAID-892) and [RAID-893](https://ardc.atlassian.net/browse/RAID-893), which record the DataCite prefix-pool email
- MockServer release history: [Maven Central, org.mock-server:mockserver-netty](https://search.maven.org/artifact/org.mock-server/mockserver-netty)
- WireMock: [github.com/wiremock/wiremock/releases](https://github.com/wiremock/wiremock/releases)
- Imposter: [docs.imposter.sh](https://docs.imposter.sh/), [Stores](https://docs.imposter.sh/stores/), [github.com/outofcoffee/imposter](https://github.com/outofcoffee/imposter), [github.com/imposter-project/imposter-go](https://github.com/imposter-project/imposter-go)
- Imposter v5 on Lambda: [examples/lambda](https://github.com/imposter-project/imposter-go/tree/main/examples/lambda) (README, `build-deploy.sh`, `perf-tests`), [docs/env_vars.md](https://github.com/imposter-project/imposter-go/blob/main/docs/env_vars.md), `internal/adapter/awslambda/awslambda.go`, `internal/store/dynamodb_store.go`, `cmd/imposter/main.go`, read 5 October 2026
- Imposter v4 licence: [outofcoffee/imposter LICENSE](https://github.com/outofcoffee/imposter/blob/main/LICENSE)
