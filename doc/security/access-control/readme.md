The terminology used here generally aligns with the wikipedia topic:
https://en.wikipedia.org/wiki/Computer_access_control

# `authentication`

* refers to "the act of proving an assertion, such as the
  identity of a computer system user."
* authentication is handled by Keycloak, which federates to external identity
  providers (AAF via SATOSA/SAML, ORCID)
* see [authentication](./authentication)


# `authorization`

* refers to "the function of specifying access rights/privileges to  
  resources"
  * see [authorization](./authorization)
* authorization is enforced by Spring Security in the api-svc, using
  Keycloak realm roles extracted from the JWT `realm_access.roles` claim


# `accountability` AKA `access authorization` 

User access is managed through Keycloak. An `operator` assigns users to
service-point groups and grants appropriate realm roles.


## Humans

* a human user authenticates via Keycloak (which federates to AAF, ORCID,
  etc.)
* an `operator` assigns the user to a service-point group in Keycloak
  and grants the appropriate roles (e.g. `service-point-user`)
* the user can then use the system to mint/edit/view raids associated with
  that service-point

See [unapproved-user-authz-request-flow.md](./authorization/unapproved-user-authz-request-flow.md).


## Machines

* machine-to-machine access uses Keycloak service accounts
  (OAuth2 client credentials grant)
* an `operator` configures a Keycloak client with appropriate roles and
  a service-point group assignment
* the client authenticates directly with Keycloak to obtain an access token
* the access token must be sent in the `Authorization` header of each API
  request (prepended with "Bearer ")

See the [authorization](./authorization/readme.md) section for details about how
roles and other authorization mechanisms work.

# `access auditing`

RAiD maintains a version history of all raid edits via the `raid_history`
table. Each change records a version number and the full raid state at
that point.

## Identification of editing user

API requests are authenticated via Keycloak JWTs, which contain the
user's `sub` (subject) claim identifying the Keycloak user. For
machine-to-machine clients, the subject identifies the service account.

## Auditing of data access

RAiD does no auditing of view actions and there is no plan to implement 
any - this includes "closed" and "embargoed" data.


