# ORCID updater

A service each registration agency runs to authenticate researchers' ORCID
iDs and write RAiDs to their ORCID records with their consent (RAID-975).

* API contract: [idl/orcid-updater-openapi-3.1.yaml](idl/orcid-updater-openapi-3.1.yaml)
* Design decisions: [ADR 2026-10-23](../doc/adr/2026-10-23_per-ra-orcid-updater.md)

The application module is added in RAID-1001.

Validate the contract from the repository root:

```bash
./gradlew :api-svc:idl-raid-v2:openApiValidate --input="$PWD/orcid-updater/idl/orcid-updater-openapi-3.1.yaml"
```
