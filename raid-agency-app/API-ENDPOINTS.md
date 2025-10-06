# RAID Agency App API Endpoints

This document provides an overview of all API endpoints used by the RAID Agency application, based on analysis of the services directory.

## Common Patterns

- Base URLs constructed dynamically based on environment: 
  - Development: `http://localhost:8080`
  - Other environments: `https://api.{environment}.raid.org.au`
- Authentication using Bearer tokens for secured endpoints
- Service-specific subdomains: `orcid.{env}.raid.org.au`, `invite.{env}.raid.org.au`, etc.
- RESTful API structure with resource-based URLs

## RAID Service

### Base URL: `${apiEndpoint}/raid`

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `/` | GET | Fetch all RAIDs | Query: `includeFields` (comma-separated list)<br>Headers: Authorization | Array of `RaidDto` |
| `/${handle}` | GET | Fetch a single RAID by handle | Path: `handle`<br>Headers: Authorization | `RaidDto` |
| `/${handle}/history` | GET | Fetch revision history of a RAID | Path: `handle`<br>Headers: Authorization | Array of `RaidHistoryType` |
| `/` | POST | Create a new RAID | Body: `RaidDto`<br>Headers: Authorization | `RaidDto` |
| `/${handle}` | PUT | Update an existing RAID | Path: `handle`<br>Body: `RaidDto`<br>Headers: Authorization | `RaidDto` |

## Service Points

### Base URL: `${apiEndpoint}/service-point`

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `/` | GET | Fetch all service points | Headers: Authorization | Array of `ServicePoint` |
| `/${id}` | GET | Fetch a single service point | Path: `id`<br>Headers: Authorization | `ServicePoint` |
| `/` | POST | Create a new service point | Body: `ServicePointCreateRequest`<br>Headers: Authorization | `ServicePoint` |
| `/${id}` | PUT | Update a service point | Path: `id`<br>Body: `ServicePointUpdateRequest`<br>Headers: Authorization | `ServicePoint` |

## Keycloak Service

### Base URL: `${kcUrl}/realms/${kcRealm}`

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `/protocol/openid-connect/token` | POST | Refresh authentication token | Body: form data with grant_type, client_id, refresh_token | `RequestTokenResponse` |
| `/group` | GET | Get service point members | Query: `groupId`<br>Headers: Authorization | Group members data |
| `/group/grant` | PUT | Grant user access to service point | Body: `userId`, `groupId`<br>Headers: Authorization | Service point data |
| `/group/revoke` | PUT | Revoke user access from service point | Body: `userId`, `groupId`<br>Headers: Authorization | Service point data |
| `/group/group-admin` | PUT | Add user to group admins | Body: `userId`, `groupId`<br>Headers: Authorization | Service point data |
| `/group/group-admin` | DELETE | Remove user from group admins | Body: `userId`, `groupId`<br>Headers: Authorization | Service point data |
| `/group/active-group` | PUT | Add active group attribute | Body: `activeGroupId` <br>Headers: Authorization | No content |
| `/group/active-group` | DELETE | Remove active group attribute | Body: `userId`<br>Headers: Authorization | No content |
| `/group/leave` | PUT | Remove user from service point | Body: `userId`, `groupId`<br>Headers: Authorization | No content |

## Invite Service

### Base URL: `https://invite.${environment}.raid.org.au`

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `/invite` | POST | Send invite to user | Body: `inviteeEmail`, `inviteeOrcid`, `title`, `handle`<br>Headers: Authorization | Invitation data |
| `/invite/fetch` | GET | Fetch invites for user | Headers: Authorization | Array of invitations |
| `/invite/accept` | POST | Accept an invitation | Body: `code`, `handle`<br>Headers: Authorization | Acceptance response |
| `/invite/reject` | POST | Reject an invitation | Body: `code`, `handle`<br>Headers: Authorization | Rejection response |

## Contributor Service

### Base URL: `https://orcid.${environment}.raid.org.au`

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `/contributors` | POST | Fetch ORCID contributors | Body: `handle`<br>Headers: Content-Type | Contributors data |

## Related Object Service

### External DOI APIs

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `https://doi.org/doiRA/${handle}` | GET | Get DOI registration agency | Path: `handle` | `DoiRaData` |
| `https://api.crossref.org/works/${handle}` | GET | Get Crossref metadata | Path: `handle` | `CrossrefData` |
| `https://api.datacite.org/dois/${handle}` | GET | Get Datacite metadata | Path: `handle` | `DataciteData` |
| `https://doi.org/api/handles/${handle}?type=url` | GET | Get DOI handle data | Path: `handle`<br>Query: `type=url` | `DoiUrlData` |

## Related RAID Service

| Endpoint | Method | Purpose | Parameters | Response |
|----------|--------|---------|------------|----------|
| `https://static.demo.raid.org.au/api/handles.json` | GET | Get environment mapping for handles | None | Map of handles to environments |
| `https://static.${environment}.raid.org.au/raids/${handle}.json` | GET | Get RAID metadata | Path: `handle`<br>Headers: Content-Type | RAID data |

## Utility Functions

- `getApiEndpoint()`: Determines the appropriate API endpoint based on hostname
- `getEnv()`: Gets the current environment (dev, test, demo, prod, stage)
- `getDefaultHeaders()`: Generates default headers with Authorization for API requests
- `transformBeforeUpdate()`: Transforms RAID data before updates (handling empty dates)