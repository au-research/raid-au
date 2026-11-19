FROM orcid-updater-base:latest

# Needed to perform health check as curl is not available in container
# and brings a load of vulnerabilities with it
COPY ./scripts/Status.class /opt/
