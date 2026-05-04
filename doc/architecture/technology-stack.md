
# The main stack

* TypeScript
* Java 
* SQL 


# Infrastructure

AWS, CDK - TypeScript

For CI/CD, we use a lot of AWS: codebuild, codepipeline, lambda, ECR.

The AWS code is in a separate repo, there's no credentials in there but we're
still reluctant to make it publicly available, especially early during 
development.

https://github.com/au-research/raido-v2-aws-private

We would be open to working on a simple "example" AWS CDK infrastructure. 


# Database

## Postgres

Managed via AWS RDS. 

No cross-database support, we take full advantage of Postgres features without 
needing to worry about cross-DB compatibility

##  Flyway 

For Schema migration.

Developers are encouraged to understand SQL and use it to full potential, 
with jOOQ helping us take advantage of static typing for our queries.

##  jOOQ

After a new schema is defined and migrated, we generate
[jOOQ](https://www.jooq.org/) code for accessing
the database, see [db/raido/build.gradle](../api-svc/db/raido/build.gradle).

## API

Currently, the API is defined as an OpenAPI 3.0 `yaml` document.

We plan to use [OpenAPI 3.1](https://www.openapis.org/blog/2021/02/18/openapi-specification-3-1-released), 
but the tooling hasn't caught up with the specification yet (seems like the 
complexity of the change to json-schema surprised most people with how much 
work it turned out to be).

We define the API as a `yaml` file, then the Java interfaces for serving
the API from the api-svc (Spring) are generated via the openapi-generator,
see [idl-raid-v2/build.gradle](../api-svc/idl-raid-v2/build.gradle).

The client-side TypeScript mappings are also generated via openapi-generator,
see [app-client/build.gradle](../app-client/build.gradle).

# Backend

* Docker 
  * very simple setup running on EC2 via shell script, 
  see [/api-svc/docker](/api-svc/docker)
    * unfortunately, the actual shell script to install and run the container 
      is in the private rep 
* Java
  * JDK 17 - Corretto
* Spring Boot

# Frontend

* TypeScript
* React
  * With `react-query`, but no Redux or similar Flux implementation
* Material UI


# REST

We use OpenApi with an “API first” approach, Java and TypeScript code is 
generated from the OpenApi definition.

* [/api-svc/idl-raid-v2](/api-svc/idl-raid-v2)
* [/raid-agency-app](/raid-agency-app)


# Miscellaneous

Little bits of scripting stuff, the usual glue code.

* Docker
  * i.e. `Dockerfile`
* Groovy
  * Gradle build scripts
  * legacy data migration
* Shell scripts
  * `codebuild` definitions
* JavaScript
  * little bits of glue, like in NPM scripts, etc.


