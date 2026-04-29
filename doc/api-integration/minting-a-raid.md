# Minting a raid in the DEMO environment

# Pre-requisites

As outlined in the [onboarding-guide](./api-client-onboarding-guide.md);
before you can mint a raid, you must have an agreement with the ARDC and have
had a service-point created for your usage.

See the [permission-model](./permission-model.md) to gain an understanding
of service-points and other important concepts.


# Get your access token

In order for your client to talk to the RAiD API, you need an access token
issued by Keycloak (the Identity and Access Management server).

## Human users

* sign in via the raid-agency-app at `https://app.demo.raid.org.au`
  * Keycloak redirects you to your chosen identity provider (AAF or ORCID)
* an operator must assign you to a service-point group in Keycloak and grant
  the `service-point-user` role before you can mint raids
  * contact `contact@raid.org` or notify us via email, Slack, or GitHub
    discussions that you need access
* once approved, the access token is obtained automatically by the
  raid-agency-app when you sign in

## Machine-to-machine (API clients)

For server-side integration, use the OAuth2 **client credentials** grant
to obtain an access token directly from Keycloak:

1. An operator creates a Keycloak client for your application, assigns the
   appropriate roles (e.g. `service-point-user`), and configures a
   service-point group
2. You authenticate with Keycloak using your client ID and secret:
```bash
curl -X POST https://iam.demo.raid.org.au/realms/raid/protocol/openid-connect/token \
  -d "grant_type=client_credentials" \
  -d "client_id=YOUR_CLIENT_ID" \
  -d "client_secret=YOUR_CLIENT_SECRET"
```
3. The response contains an `access_token` (a Keycloak-issued RS256 JWT)

### Example

For the examples outlined below, we're going to do basic `curl` commands to
mint and read a raid.

To make these commands readable, we're going to store the access token into an
environment variable that will be used by the example commands:
```
export DEMO_TOKEN="xxx.yyy.zzz"
```

Note that we _don't_ prepend the `Bearer ` prefix, since we do that below in
the header specification of the example commands.


### Access token security

$${\color{red}**WARNING**}$$

The access token is to be considered sensitive, non-public information.

Access tokens must be kept secret and should never be accessible to
end-users:
  * do not embed the token in front-end client applications or web-sites
  * the access token is the only thing necessary to use the API and can be
    used to mint/edit raids and see closed raid data
  * access tokens expire (typically within minutes); use the client credentials
    grant to obtain a fresh token as needed


## Mint a raid
* use the stable "mint" endpoint
([`POST /raid/`](/api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml))
to create a raid
* you must set your access token in the `Authorization` header (don't forget
to prefix with `Bearer ` in the value)
* use the OpenAPI definitions as a guide to what fields are required
  * the metadata schema guide is available at
  [RAiD Metadata Schema](https://metadata.raid.org/en/latest/index.html)

### Example
```bash
curl -v -X POST https://api.demo.raid.org.au/raid/ \
  -H "Authorization: Bearer $DEMO_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "title": [
    {
      "text": "Client Integration Test RAID No. 1",
      "type": {
        "id": "https://vocabulary.raid.org/title.type.schema/5",
        "schemaUri": "https://vocabulary.raid.org/title.type.schema/376"
      },
      "startDate": "2023-02-01"
    }
  ],
  "date": {
    "startDate": "2023-02-01"
  },
  "description": [
    {
      "text": "Test Description",
      "type": {
        "id": "https://vocabulary.raid.org/description.type.schema/318",
        "schemaUri": "https://vocabulary.raid.org/description.type.schema/320"
      }
    }
  ],
  "access": {
    "type": {
      "id": "https://vocabularies.coar-repositories.org/access_rights/c_abf2/",
      "schemaUri": "https://vocabularies.coar-repositories.org/access_rights/"
    }
  },
  "contributor": [
    {
      "id": "https://orcid.org/0000-0002-6492-9025",
      "schemaUri": "https://orcid.org/",
      "position": [
        {
          "id": "https://vocabulary.raid.org/contributor.position.schema/307",
          "schemaUri": "https://vocabulary.raid.org/contributor.position.schema/305",
          "startDate": "2023-02-01"
        }
      ],
      "role": [
        {
          "id": "https://credit.niso.org/contributor-roles/supervision",
          "schemaUri": "https://credit.niso.org/"
        }
      ]
    }
  ]
}'
```

## Read a raid

* use the stable "read" endpoint
([`GET /raid/{prefix}/{suffix}`](/api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml))
to read the minted raid

### Example
```bash
curl -v https://api.demo.raid.org.au/raid/10378.1/1709242 \
  -H "Authorization: Bearer $DEMO_TOKEN" \
  -H 'Accept: application/json'
```
