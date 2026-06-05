List of things to consider in a code review.


# Security

## Are endpoints secure?

Endpoint implementations must check authorization biz rules.


## No secrets in code

No tokens or any kind of sensitive data in the repo


## Don't log sensitive data

Not even as "debug".


## Avoid SSRF issues - don't use un-validated data in external requests

See [ssrf.md](/doc/security/api-svc/ssrf.md).


## Avoid XSS issues - don't use  `dangerouslySetInnerHTML` in raid-agency-app

See [xss.md](/doc/security/api-svc/xss.md)


## Avoid SQL injection issues - don't create SQL strings dynamically

See [sql-injection.md](/doc/security/api-svc/sql-injection.md)


# Performance

## API Pagination

List-type endpoints must implement should generally implement a hard maximum 
limit on the number of items returned.

Prefer building user-specified search functionality in preference to paging
through many pages or implementing-client side searching.

If pagination is needed, prefer "key-set" style pagination over "offset-limit".  
The most important thing though is just to make sure we're not
creating endpoints that could end up unintentionally returning large sets
of data (for DB locking, performance, network and cost reasons).

https://use-the-index-luke.com/blog/2013-07/pagination-done-the-postgresql-way
https://medium.com/swlh/sql-pagination-you-are-probably-doing-it-wrong-d0f2719cc166

Where large data-sets *are* required, we should be using an 
asynchronous "export" rather than returning large amounts of data via endpoint. 


## Consider the SQL being issued

* avoid n+1 queries
* avoid returning too many unused columns


## Consider if queries will use an index

* will the query use an existing index? Does an index need to be modified/added?


# Compatibility

## Avoid incompatible changes to stable API endpoints 

If an incompatible change is needed, a new version of the data structure should
be published on a new version of the endpoint.


## Avoid incompatible changes to the DB schema

If an incompatible change is needed, it should be split up into multiple changes
so that a "zig zag" deployment flow can be implemented.


## Consider forward compatibility on stable API versions

Example: if creating a list, don't just return that list.
Return the list inside an object with a "list" field, so that other fields
can be added later (like pagination).


# Configuration

Configure defaults for dev environments.

Configuration defaults are set in 
[application.yaml](../../api-svc/raid-api/src/main/resources/application.yaml)
with profile-specific overrides for deployed environments.


# Logging

* Avoid string concatenation before the level is checked - use structured 
  logging api
* Are appropriate log levels used?
  * remember that `debug` is off by default in prod deployments
  * use debug for debug/developer stuff
  * limit the amount of info and above calls, consider how the info will be 
  used in prod

See [api-svc/doc/logging.md](../../api-svc/doc/logging.md)