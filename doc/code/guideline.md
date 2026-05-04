
# Guidelines

> At least just think about it for a second.

## Imperative commit messages

Use the imperative mood for commit messages: 
https://git.kernel.org/pub/scm/git/git.git/tree/Documentation/SubmittingPatches?h=v2.36.1#n181

Try to stick to 50/72 format for lines, as per 
https://stackoverflow.com/q/2290016/924597

## Favour fail-fast config

Fail at start instead of later.

Means there's less smoke testing to do on a release, because if 
config/credentials are missing or malformed - the node won't start.   
i.e. less need to run around trying to exercise functionality to verify that 
the node is functional (which people always shortcut or skip entirely).

This also helps with container-orchestration tool deployments (i.e. AWS ECS or 
Kubernetes) - if the node fails on startup, then the orchestrator knows not to 
consider the deployment a success and can roll-back.

Also, the code isn't doing pointless validation on every invocation that only 
needs to be done once at startup.

## Configure for development by default

Configuration defaults are set in 
[application.yaml](/api-svc/raid-api/src/main/resources/application.yaml) 
with profile-specific overrides for deployed environments.

