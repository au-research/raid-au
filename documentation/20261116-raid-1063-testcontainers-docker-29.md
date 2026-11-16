# RAID-1063: Upgrade Testcontainers to 1.21.4 for Docker 29

- JIRA: [RAID-1063](https://ardc.atlassian.net/browse/RAID-1063)
- Branch: `feature/RAID-1063` into `main`
- PR: to follow
- Status: in review

## What changed and why

GitHub's `ubuntu-latest` runner moved from Ubuntu 24.04 (Docker Server 28.0.4)
to Ubuntu 26.04 (Docker Server 29.4.2) between 13 and 16 November 2026.
Testcontainers 1.21.3 can't connect to Docker 29, so the PR Integration Tests
workflow failed in `:iam:intTest` with "Could not find a valid Docker
environment", even though `docker compose` worked earlier in the same job. The
API intTests never ran. Every PR that triggers the workflow was affected.

Testcontainers 1.21.4 makes the 1.21.x line work with recent Docker Engine
releases (its release note). The version in `gradle/libs.versions.toml` is now
`1.21.4`. It covers every module that uses Testcontainers.

## Evidence

- Passing run on 13 November: runner image `ubuntu-24.04`.
- Failing runs on 16 November (raid-au PR #787, run 43774835082, both attempts):
  runner image `ubuntu-26.04` 20261111.82.
- Docker versions from the runner images' READMEs: 24.04 has Docker Server
  28.0.4, 26.04 has Docker Server 29.4.2.

## Testing

- Locally (Docker 28.1.1): `:iam:intTest` and the API's Testcontainers
  migration tests pass with 1.21.4.
- Docker 29 isn't available locally, so the PR's own integration-tests run on
  `ubuntu-26.04` is the check that the fix works.
