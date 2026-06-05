You have to be careful with "DEMO only" migrations.

They need to take into account what database schema you're starting from.
The schema you're starting from changes when you restore a PROD DB into the 
DEMO env (we expect to do that from now on, rather than start from an empty DB).

Generally speaking, when you decided to "bump" the baseline version of the DB
used in DEMO - you need to consider the files in this directory.

You need to bump the versions along  - otherwise migration will fail because of 
out-of-order migrations.  The out-of-order detection is a good thing - don't 
just get the demo migration working by enabling out-of-order migration.

Generally, you want to add the DEMO specific files as "minor" versions following
on from the the version your schema is using as a baseline.

For example:
* PROD V1.0 was up to V8 for the flyway migration.
* so I renamed `V4_1__surf-operators.sql` to `V8_1__DEMO_surf_operators.sql`
  * this means the file will execute the next time flywayMigrate is run on DEMO
  env after any refresh of the DB to the V8 schema (i.e. V1.0 of raido).

We do the DEMO stuff as a minor version so that actual PROD migrations follow 
on from the PROD baseline and we don't end up with "gaps" in our schema 
history.

---
