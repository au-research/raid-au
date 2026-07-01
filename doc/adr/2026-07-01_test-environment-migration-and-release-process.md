### Migrate test environment to raid.org and introduce RC-based release process

* Status: proposed
* Who: proposed by RL
* When: proposed on 2026-07-01
* Related: per-branch test environment work (branch schema cloning)


# Decision

Migrate the RAiD test environment from the raid.org.au account (576003565477,
managed via `raido-v2-aws-private`) to a new test account under the raid.org
organisation (managed via `raid-aws-private`). At the same time, rename the
`raid-au` application repository to reflect that the codebase is not specific
to the Australian registration agency.

The test pipeline currently running in the raido-root account (382051401658)
will move to the raid-root account (380617297661).

Introduce a release candidate (RC) workflow: code that passes the test
environment receives a semantic version with an RC suffix (e.g. `2.10.0-RC1`)
and is published as a container image to a public GitHub Container Registry
(GHCR) repository. Registration agencies can then pull and deploy the RC to
their own environments for further testing. Once the RC is approved for general
availability, an AWS CodeBuild project re-tags the image without the RC suffix
(e.g. `2.10.0`), promoting the exact same image digest.


# Context

The current infrastructure conflates two distinct roles:

* **raid.org** -- the registration authority responsible for the RAiD
  specification, global infrastructure, and the canonical test/release pipeline.
* **RAiD AU** -- the Australian registration agency that operates a production
  deployment of the RAiD software.

Both currently share AWS accounts, pipelines, and the `raid-au` repository name,
making it unclear where the registration authority ends and the Australian
registration agency begins. As other registration agencies come online, this
separation becomes necessary so that:

1. The test environment and release process are owned by raid.org (the
   authority), not by any single registration agency.
2. Registration agencies can independently evaluate release candidates before
   adopting them.
3. The codebase and its public container images are clearly identified as the
   registration authority's product, not an Australian-specific artefact.
4. External contributors (developers not employed by the ARDC) can observe the
   test pipeline and environment, supporting open development of an open source
   project.

The per-branch test environment work has already removed hardcoded schema names
from Flyway migrations, which removes one of the larger obstacles to this move.


# Release workflow

```
  developer pushes to main
        |
        v
  raid-root pipeline builds and deploys to raid.org test environment
        |
        v
  test environment validation (automated + manual)
        |
        v
  image tagged with RC version (e.g. 2.10.0-RC1) and pushed to GHCR
        |
        v
  registration agencies pull RC and test in their own environments
        |
        v
  approval to promote
        |
        v
  CodeBuild re-tags the same image digest without the RC suffix (2.10.0)
        |
        v
  GA image available on GHCR for all registration agencies
```

**Image promotion** is a tag operation on the existing manifest, not a rebuild.
This guarantees the GA image is byte-identical to the tested RC. The CodeBuild
project authenticates to GHCR via a personal access token (or GitHub App token)
stored in Secrets Manager and runs:

```
docker buildx imagetools create \
  --tag ghcr.io/<org>/<repo>:2.10.0 \
  ghcr.io/<org>/<repo>:2.10.0-RC1
```

Tag immutability is not enforced on the GHCR repository. In future the
promotion mechanism may move outside AWS, but the physical act of re-tagging
will remain the same.


# Consequences

## Infrastructure

* A new AWS account is created under the raid.org organisation for the test
  environment.
* CDK stacks for the test environment move from `raido-v2-aws-private`
  (ap-southeast-2) to `raid-aws-private`. The target region for the new test
  account is a separate decision.
* The raido-root CodePipeline for test deployments is decommissioned once the
  raid-root pipeline is operational.
* DNS records and ACM certificates for the test environment are provisioned via
  CDK in the new account. The existing test environment remains operational
  until the new one is verified.

## Repository

* The `raid-au` repository is renamed. The new name should reflect its role as
  the registration authority's application codebase (exact name TBD).
* GitHub automatically redirects the old URL, but CI workflows, CDK source
  actions, documentation links, and developer clones will need updating.

## Contributor access

* The test deployment pipeline and environment must be accessible to all
  developers contributing to RAiD, not only ARDC staff. RAiD is an open source
  project and external contributors need visibility into build and deployment
  status.
* A `RaidContributor` IAM role is created in the raid-root account with
  read-only access to the Branch-Build-Deploy CodePipeline project. This
  allows contributors to view pipeline executions, build logs, and deployment
  status without granting write access to infrastructure or secrets.
* Access to the `RaidContributor` role is granted via IAM Identity Center or
  direct federation, depending on how external contributor identity is managed
  (a separate decision).

## Container registry

* A public GHCR repository is created under the raid.org GitHub organisation.
* The CodeBuild project for GA promotion needs cross-service credentials
  (AWS Secrets Manager holding a GHCR token).

## Release process

* RC versions follow SemVer pre-release format: `MAJOR.MINOR.PATCH-RC<n>`.
* Multiple RCs may exist for a given version (RC1, RC2, ...) if issues are
  found during registration agency testing.
* The promotion from RC to GA is a deliberate, auditable action (CodeBuild
  execution with logs).
* Registration agencies are responsible for deploying and validating RCs in
  their own environments before signalling approval.

## Migration risks

* Hardcoded references to `raid.org.au` test URLs (Keycloak realm endpoints,
  OAuth callback URIs, API base URLs) must be updated. A sweep of CDK code,
  application configuration, and SSO/SAML metadata is required before cutover.
* The existing test environment stays live in parallel until the new one is
  confirmed working, avoiding a gap in test coverage.
* IAM trust relationships in the raid-root account must permit the pipeline to
  deploy to the new test account and authenticate to GHCR.


# Links

* GitHub Container Registry documentation: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry
* SemVer pre-release specification: https://semver.org/#spec-item-9
* `docker buildx imagetools create` reference: https://docs.docker.com/reference/cli/docker/buildx/imagetools/create/
