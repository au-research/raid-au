Avoid adding the `@Transactional` annotation for at the top level for 
high-throughput endpoints that make calls to external services 
(e.g. mint/update raids, etc.) or do any other long-running work.

We want to avoid having a DB connection checked out from the connection pool
for a long time. External system calls (ORCID, DOI, etc.) often take _seconds_.   
External calls can often get even slower than that (measuring into the
double-digit seconds) when the external system is having problem, responding
slowly or not at all. Consider what HTTP timeout you're using for those calls.


## Why blocking the connection pool is bad

A "long time" is anything expected take more than about 20ms. That 
includes DB queries that are poorly optimised (not using indexes) or poorly 
designed (returning too much data, etc.).  
If you have a query that "must" take a long time, you need to consider how that 
affects the connection availability of the rest of the system.

If we hold a DB connection open while we do long-running work, we will 
exhaust our connection pool very quickly.  This causes errors for all other 
endpoints that need a connection to do work with the DB - they'll all start 
throwing "could not acquire connection" type errors. Even though all the other 
methods in the system aren't doing long running work, they're all affected by
the lack of the shared resource (i.e. available DB connections from the pool).

Don't be tempted to work around this by issue by simply raising the connection 
count and wait timeout.
You're just causing unnecessary connection load on the DB (which is a weak area 
of postgres) - it hides the symptom of the problem and causes more 
issues down the line.  A large DB connection pool with long timeouts is really
just a poorly implemented, inefficient, inflexible internal task queue built 
out of pending threads blocking on the connection pool.

Large connection counts can also affect the DB performance even when they're
idle: https://web.archive.org/web/20230406110505/https://aws.amazon.com/blogs/database/resources-consumed-by-idle-postgresql-connections/
Though the connection pool should take care of shutting those down when they're 
not in use, if configured appropriately.


## Notes on max connections

`select * from pg_settings where name='max_connections'`

Running on my local machine, with PG 16 running in a docker container: 100.

As at 2023-06-08, PG 15 running on an AWS RDS `db.t4g.micro` instance: 81

db.t4g.small: 190

Note: as of 2026-04, the local docker-compose uses PostgreSQL 16.8
(`api-svc/raid-api/docker-compose.yaml`).