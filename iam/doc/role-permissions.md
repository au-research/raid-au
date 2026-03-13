# RAID Role-Based Access Control (RBAC) Documentation

This document outlines the role-based permissions system for the RAID API and IAM components, detailing what actions various roles can perform.

## System Overview

The RAID system uses a multi-layered authorization approach combining:
- **Role-based access control** via JWT tokens
- **Service point ownership** validation
- **Specific RAID permissions** via JWT claims
- **Embargo protection** for restricted content

## Role Definitions and Permissions

### 1. **operator** (OPERATOR_ROLE)
**Highest privilege level - Full system access**

**Permissions:**
- Full read access to all RAIDs (including embargoed content)
- Complete service point management (GET, PUT, POST on `/service-point/**`)
- Can perform any operation without additional authorization checks
- Not subject to embargo restrictions
- Bypasses service point ownership requirements

### 2. **raid-admin** (RAID_ADMIN_ROLE)  
**Administrative access to specific RAIDs**

**Permissions:**
- Create new RAIDs (`POST /raid/**`)
- Read access to specific RAIDs listed in `admin_raids` JWT claim
- Write access to specific RAIDs listed in `admin_raids` JWT claim
- Subject to embargo restrictions (cannot access embargoed RAIDs unless via service point ownership)

**Managed by:** Service point users can grant/revoke raid-admin role to users

### 3. **contributor-writer** (CONTRIBUTOR_WRITER_ROLE)
**Special role for contributor data management**

**Permissions:**
- Full read access to all RAIDs (including embargoed content)
- Patch access to all RAIDs (`PATCH /raid/**`)
- Not subject to embargo restrictions
- Can modify contributor information across all RAIDs

### 4. **service-point-user** (SERVICE_POINT_USER_ROLE)
**Service point-based access control**

**Permissions:**
- Create new RAIDs (`POST /raid/**`)
- Read access to RAIDs owned by their service point (via `service_point_group_id` JWT claim)
- Write access to RAIDs owned by their service point
- Patch access to RAIDs owned by their service point
- Read access to service point information (`GET /service-point/**`)
- Can grant/revoke `raid-admin` role to other users
- **Cannot access embargoed RAIDs** (even from own service point)

**Authorization mechanism:** JWT token must contain `service_point_group_id` claim matching the service point that owns the RAID

### 5. **raid-user** (RAID_USER_ROLE)
**Limited access to specific RAIDs**

**Permissions:**
- Read access to specific RAIDs listed in `user_raids` JWT claim
- Write access to specific RAIDs listed in `user_raids` JWT claim
- Subject to embargo restrictions (cannot access embargoed RAIDs)

**Managed by:** Users with `raid-permissions-admin` client role can add/remove RAID access

### 6. **pid-searcher** (PID_SEARCHER_ROLE)
**Specialized role for identifier searches**

**Permissions:**
- Read access to RAIDs only when performing PID searches (queries with `contributor.id` or `organisation.id` parameters)
- Cannot access general RAID endpoints without search parameters
- Subject to embargo restrictions

### 7. **raid-dumper** (RAID_DUMPER_ROLE)
**Bulk data access for system operations**

**Permissions:**
- Access to bulk public RAID data (`GET /raid/all-public`)
- Typically used for data export/backup operations

### 8. **raid-upgrader** (RAID_UPGRADER_ROLE)
**System maintenance and upgrade operations**

**Permissions:**
- Access to upgrade-related endpoints:
  - `GET /upgradable/all` - List upgradable RAIDs
  - `POST /upgrade` - Perform upgrades
  - `POST /raid/post-to-datacite` - DataCite operations
- Access to bulk public RAID data (`GET /raid/all-public`)

## Special Authorization Cases

### Embargo Protection
- **Embargoed RAIDs** (access type = embargoed) have restricted access
- Only `operator` and `contributor-writer` roles can access embargoed content
- Service point users cannot access embargoed RAIDs even from their own service point
- Embargo status is checked via `SchemaValues.ACCESS_TYPE_EMBARGOED`

### Service Point Ownership
- Authorization based on matching `service_point_group_id` JWT claim with RAID's owning service point
- Service point ownership is verified through the ServicePointService
- Users must have appropriate service point user role

### JWT Claims Used
- `service_point_group_id` - Links user to a specific service point
- `admin_raids` - Array of RAID handles user can administer
- `user_raids` - Array of RAID handles user can access
- `realm_access.roles` - User's assigned roles

## Public Endpoints (No Authentication Required)
- Swagger UI (`/swagger-ui*/**`)
- Documentation (`/docs/**`)  
- Health checks (`/actuator/**`)
- Error pages (`/error`)
- OPTIONS requests for CORS

## IAM Permission Management

### Client Role: `raid-permissions-admin`
**Can manage user RAID permissions:**
- Add users to specific RAIDs (`POST /raid-user`)
- Remove users from specific RAIDs (`DELETE /raid-user`)
- Add admin permissions to specific RAIDs (`POST /admin-raids`)

### User Attributes Managed
- `userRaids` - List of RAID handles the user can access
- `adminRaids` - List of RAID handles the user can administer

## Security Features

1. **Path-based authorization** - RAID handles extracted from URL paths
2. **JWT token validation** - All authenticated endpoints require valid JWT
3. **Role hierarchy enforcement** - Higher privilege roles can perform lower privilege actions
4. **CORS protection** - Configured allowed origins for web access
5. **Exception handling** - Graceful handling of missing resources/permissions

## Access Control Matrix

| Role | Create RAID | Read RAID | Write RAID | Patch RAID | Service Point Mgmt | Bulk Operations | Upgrade Ops |
|------|-------------|-----------|------------|------------|-------------------|----------------|-------------|
| operator | ✓ | ✓ (all) | ✓ (all) | ✓ (all) | ✓ | ✓ | ✓ |
| contributor-writer | ✗ | ✓ (all) | ✗ | ✓ (all) | ✗ | ✗ | ✗ |
| service-point-user | ✓ | ✓ (owned, non-embargoed) | ✓ (owned) | ✓ (owned) | ✓ (read) | ✗ | ✗ |
| raid-admin | ✓ | ✓ (specific, non-embargoed) | ✓ (specific) | ✗ | ✗ | ✗ | ✗ |
| raid-user | ✗ | ✓ (specific, non-embargoed) | ✓ (specific) | ✗ | ✗ | ✗ | ✗ |
| pid-searcher | ✗ | ✓ (search only) | ✗ | ✗ | ✗ | ✗ | ✗ |
| raid-dumper | ✗ | ✓ (bulk public) | ✗ | ✗ | ✗ | ✓ | ✗ |
| raid-upgrader | ✗ | ✓ (bulk public) | ✗ | ✗ | ✗ | ✓ | ✓ |

*Note: "specific" means only RAIDs listed in user's JWT claims; "owned" means RAIDs belonging to user's service point*