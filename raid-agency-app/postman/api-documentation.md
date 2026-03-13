# RAID Agency API Documentation

This documentation provides details on the available endpoints for the RAID Agency Application.

## Authentication

Most endpoints require authentication via OAuth 2.0 bearer tokens. Include the token in your request headers:

```
Authorization: Bearer <your_access_token>
```

You can use the following script as collection / scripts / Pre-request script to get a new authentication token before each request

```
const postRequest = {
  url: `${pm.environment.get("kc_url")}/realms/${pm.environment.get("kc_realm")}/protocol/openid-connect/token`,
  method: "POST",
  header: {
    "Content-Type": "application/x-www-form-urlencoded",
  },
  body: {
    mode: "urlencoded",
    urlencoded: [
      { key: "grant_type", value: "client_credentials" },
      { key: "client_id", value: pm.environment.get("kc_client_id") },
      { key: "client_secret", value: pm.environment.get("kc_client_secret") }
    ],
  },
};

pm.sendRequest(postRequest, (error, response) => {
  if (error) {
    console.log(error);
  }

  pm.environment.set("access_token", null);

  pm.test("response should be okay to process", () => {
    pm.expect(error).to.equal(null);
    pm.expect(response).to.have.property("code", 200);
    pm.expect(response).to.have.property("status", "OK");
    pm.environment.set("access_token", response.json().access_token);
  });
});
```

### Obtaining Tokens

```
POST {{kc_url}}/realms/{{kc_realm}}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&client_id={{kc_client_id}}&refresh_token={{refresh_token}}
```

## RAID Service API

Endpoints for managing Research Activity Identifier (RAID) records.

### Fetch All RAIDs

Retrieves a list of all RAID records with optional field filtering.

```
GET {{raid_api_url}}/raid/
Content-Type: application/json
```

### Fetch One RAID

Retrieves a single RAID record by its handle.

```
GET {{raid_api_url}}/raid/{prefix}/{suffix}
Content-Type: application/json
```

Example: `GET {{raid_api_url}}/raid/10.82841/04f8a2ed`

### Fetch RAID History

Retrieves the revision history of a RAID record.

```
GET {{raid_api_url}}/raid/{prefix}/{suffix}/history
Content-Type: application/json
Authorization: Bearer {{token}}
```

Example: `GET {{raid_api_url}}/raid/10.82841/04f8a2ed/history`

### Create RAID

Creates a new RAID record.

```
POST {{raid_api_url}}/raid/
Content-Type: application/json
Authorization: Bearer {{token}}
```

Request Body Example:
```json
{
  "title": [
    {
      "text": "Sample RAID Title",
      "type": {
        "id": "https://vocabulary.raid.org/title.type.schema/5",
        "schemaUri": "https://vocabulary.raid.org/title.type.schema/376"
      },
      "startDate": "2023-01-01"
    }
  ],
  "contributor": [
    {
      "id": "https://orcid.org/0009-0000-9306-3120",
      "schemaUri": "https://orcid.org/",
      "position": [
        {
          "schemaUri": "https://vocabulary.raid.org/contributor.position.schema/305",
          "id": "https://vocabulary.raid.org/contributor.position.schema/307",
          "startDate": "2025-03-25"
        }
      ],
      "role": [
        {
          "schemaUri": "https://credit.niso.org/",
          "id": "https://credit.niso.org/contributor-roles/software/"
        }
      ],
      "leader": true,
      "contact": true
    }
  ],
  "description": [
    {
      "text": "Sample RAID Description",
      "type": {
        "id": "https://vocabulary.raid.org/description.type.schema/318",
        "schemaUri": "https://vocabulary.raid.org/description.type.schema/320"
      }
    }
  ],
  "date": {
    "startDate": "2023-01-01"
  },
  "access": {
    "type": {
      "id": "https://vocabularies.coar-repositories.org/access_rights/c_abf2/",
      "schemaUri": "https://vocabularies.coar-repositories.org/access_rights/"
    },
    "statement": null,
    "embargoExpiry": ""
  }
}
```

### Update RAID

Updates an existing RAID record. Make sure the `version` value is updated accordingly

```
PUT {{raid_api_url}}/raid/{prefix}/{suffix}
Content-Type: application/json
Authorization: Bearer {{token}}
```

Example: `PUT {{raid_api_url}}/raid/10.82841/04f8a2ed`

Request Body Example:
```json
{
  "identifier": {
    "id": "http://localhost:8080/10.82841/04f8a2ed",
    "schemaUri": "https://raid.org/",
    "registrationAgency": {
      "id": "https://ror.org/038sjwq14",
      "schemaUri": "https://ror.org/"
    },
    "owner": {
      "id": "https://ror.org/038sjwq14",
      "schemaUri": "https://ror.org/",
      "servicePoint": 20000000
    },
    "raidAgencyUrl": "http://test.static.raid.org.au/10.82841/04f8a2ed",
    "license": "Creative Commons CC-0",
    "version": 4
  },
  "date": {
    "startDate": "2023-01-01"
  },
  "title": [
    {
      "text": "Updated RAID Title",
      "type": {
        "id": "https://vocabulary.raid.org/title.type.schema/5",
        "schemaUri": "https://vocabulary.raid.org/title.type.schema/376"
      },
      "startDate": "2023-01-01"
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
      "id": "https://orcid.org/0009-0000-9306-3120",
      "schemaUri": "https://orcid.org/",
      "status": "AUTHENTICATED",
      "uuid": "59a67c6b-8685-4854-bae5-0514b076be74",
      "position": [
        {
          "schemaUri": "https://vocabulary.raid.org/contributor.position.schema/305",
          "id": "https://vocabulary.raid.org/contributor.position.schema/307",
          "startDate": "2025-03-25"
        }
      ],
      "role": [
        {
          "schemaUri": "https://credit.niso.org/",
          "id": "https://credit.niso.org/contributor-roles/software/"
        }
      ],
      "leader": true,
      "contact": true
    }
  ]
}
```

## Invite Service API

Endpoints for managing RAID collaboration invitations.

### Send Invite (Email)

Sends an invitation to collaborate on a RAID via email.

```
POST https://invite.{{raid_env}}.raid.org.au/invite
Content-Type: application/json
Authorization: Bearer {{token}}
```

Request Body:
```json
{
  "inviteeEmail": "john.doe@example.com",
  "handle": "{{handle}}"
}
```

### Send Invite (ORCID)

Sends an invitation to collaborate on a RAID via ORCID.

```
POST https://invite.{{raid_env}}.raid.org.au/invite
Content-Type: application/json
```

Request Body:
```json
{
  "inviteeOrcid": "0000-0000-0000-0000",
  "title": "Sample Project Title",
  "handle": "{{handle}}"
}
```

### Fetch Invites

Retrieves invitations for the current user.

```
GET https://invite.{{raid_env}}.raid.org.au/invite/fetch
Content-Type: application/json
Authorization: Bearer {{token}}
```

### Accept Invite

Accepts a collaboration invitation.

```
POST https://invite.{{raid_env}}.raid.org.au/invite/accept
Content-Type: application/json
Authorization: Bearer {{token}}
```

Request Body:
```json
{
  "code": "{{invite_code}}",
  "handle": "{{handle}}"
}
```

### Reject Invite

Rejects a collaboration invitation.

```
POST https://invite.{{raid_env}}.raid.org.au/invite/reject
Content-Type: application/json
Authorization: Bearer {{token}}
```

Request Body:
```json
{
  "code": "{{invite_code}}",
  "handle": "{{handle}}"
}
```

## Contributor Service API

Endpoints for managing RAID contributors.

### Fetch ORCID Contributors

Retrieves contributors associated with a RAID via ORCID.

```
POST https://orcid.{{raid_env}}.raid.org.au/contributors
Content-Type: application/json
```

Request Body:
```json
{
  "handle": "{{handle}}"
}
```

## Service Points API

Endpoints for managing service points.

### Fetch Service Points

Retrieves all service points.

```
GET {{raid_api_url}}/service-point/
Content-Type: application/json
Authorization: Bearer {{token}}
```

### Fetch Service Point

Retrieves a specific service point by ID.

```
GET {{raid_api_url}}/service-point/{{servicepoint_id}}
Content-Type: application/json
Authorization: Bearer {{token}}
```

### Create Service Point

Creates a new service point.

```
POST {{raid_api_url}}/service-point/
Content-Type: application/json
```

Request Body:
```json
{
  "name": "Test Service Point",
  "identifierPrefix": "test",
  "identifierOwner": "https://ror.org/example",
  "contactEmail": "contact@example.com",
  "contactName": "Contact Person",
  "repositoryId": "repository123",
  "adminEmail": "admin@example.com"
}
```

### Update Service Point

Updates an existing service point.

```
PUT {{raid_api_url}}/service-point/{{service_point_id}}
Content-Type: application/json
```

Request Body:
```json
{
  "name": "Updated Service Point",
  "contactEmail": "updated@example.com",
  "contactName": "Updated Contact Person"
}
```

### Grant User Service Point Role

Grants a user role in a service point.

```
PUT {{kc_url}}/realms/{{kc_realm}}/group/grant
Content-Type: application/json
Authorization: Bearer {{token}}
```

Request Body:
```json
{
  "userId": "{{user_id}}",
  "groupId": "{{group_id}}"
}
```

### Revoke User Service Point Role

Revokes a user role in a service point.

```
PUT {{kc_url}}/realms/{{kc_realm}}/group/revoke
Content-Type: application/json
```

Request Body:
```json
{
  "userId": "{{user_id}}",
  "groupId": "{{group_id}}"
}
```

### Add User To Group Admins

Promotes a user to group admin for a service point.

```
PUT {{kc_url}}/realms/{{kc_realm}}/group/group-admin
Content-Type: application/json
```

Request Body:
```json
{
  "userId": "{{user_id}}",
  "groupId": "{{group_id}}"
}
```

### Remove User From Group Admins

Removes a user from group admins for a service point.

```
DELETE {{kc_url}}/realms/{{kc_realm}}/group/group-admin
Content-Type: application/json
```

Request Body:
```json
{
  "userId": "{{user_id}}",
  "groupId": "{{group_id}}"
}
```

### Remove User From Service Point (Clear Active Group)

Removes the active group attribute for a user.

```
DELETE {{kc_url}}/realms/{{kc_realm}}/group/active-group
Content-Type: application/json
```

Request Body:
```json
{
  "userId": "{{user_id}}"
}
```

### Remove User From Service Point (Leave Group)

Removes a user from a service point group.

```
PUT {{kc_url}}/realms/{{kc_realm}}/group/leave
Content-Type: application/json
```

Request Body:
```json
{
  "userId": "{{user_id}}",
  "groupId": "{{group_id}}"
}
```

## Keycloak Groups API

Endpoints for managing Keycloak groups.

### Join Keycloak Group

Adds the current user to a Keycloak group.

```
PUT {{kc_url}}/realms/{{kc_realm}}/group/join
Content-Type: application/json
```

Request Body:
```json
{
  "groupId": "{{group_id}}"
}
```

### Fetch All Keycloak Groups

Retrieves all Keycloak groups.

```
GET {{kc_url}}/realms/{{kc_realm}}/group/all
Content-Type: application/json
```

### Fetch Current User Keycloak Groups

Retrieves groups for the current user.

```
GET {{kc_url}}/realms/{{kc_realm}}/group/user-groups
Content-Type: application/json
```

### Set Keycloak User Attribute

Sets the active service point for the current user.

```
PUT {{kc_url}}/realms/{{kc_realm}}/group/active-group
Content-Type: application/json
```

Request Body:
```json
{
  "activeGroupId": "{{group_id}}"
}
```

## External APIs

External APIs for DOI resolution and RAID lookup.

### Fetch DOI Registration Agency

Identifies the registration agency for a DOI.

```
GET https://doi.org/doiRA/{{doi_handle}}
```

### Fetch Crossref Metadata

Retrieves metadata for DOIs registered with Crossref.

```
GET https://api.crossref.org/works/{{doi_handle}}
```

### Fetch Datacite Metadata

Retrieves metadata for DOIs registered with Datacite.

```
GET https://api.datacite.org/dois/{{doi_handle}}
```

### Fetch Related RAID Metadata

Retrieves information about related RAID records.

```
GET https://static.{{raid_env}}.raid.org.au/raids/{{handle}}.json
Content-Type: application/json
```