# RAID-998: Document `raid.orcid-integration.enabled` for deploying agencies

- JIRA: [RAID-998](https://ardc.atlassian.net/browse/RAID-998), sub-task of [RAID-993](https://ardc.atlassian.net/browse/RAID-993)
- Related: [RAID-886](https://ardc.atlassian.net/browse/RAID-886) (the deployment configuration reference this updates)
- PR: see the PR linked on RAID-998
- Status: PR open

## What changed and why

API 2.20.0 (RAID-993) added `raid.orcid-integration.enabled`, default
`false`. The deployment configuration reference still described the 2.19.0
behaviour, where the host defaulted to the local mock server and an agency had
no way to run without an integration.

`doc/reference/deployment-configuration.md` now:

- notes the 2.20.0 boundary at the top, alongside the 2.17.0 one, so an agency
  upgrading knows to set `enabled=true` first
- limits trap 1 (host defaults to the mock server) to 2.19.0 and earlier, and
  says the failure on an unreachable host still applies when the integration
  is enabled
- adds `raid.orcid-integration.enabled` to the ORCID integration table, and
  describes enabled and disabled behaviour, contributor statuses, the startup
  log lines and the startup check
- adds `enabled = true` to the RAiD AU demo worked example, which matches its
  running task definition since RAID-996
- adds the ORCID startup line to "Checking what a deployment actually loaded",
  with the log-level caveat

Documentation only. No code or configuration change.
