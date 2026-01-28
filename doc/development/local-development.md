
# Local build/development pre-requisites


* [JDK 17](../adr/2022-07-21_jdk-platform.md)
  * The specific version we use for deployment is declared in the api-svc
  [dockerfile](https://github.com/au-research/raido/blob/main/api-svc/docker/ecs/src/main/docker/Dockerfile#L19)
  * We use the Amazon Corretto JDK, but any version 17+ JDK should work
  * There are many ways to install JDK's depending on your environment
    * one simple way is to download a simple zip file and keep it local to your
    environment.  You can also download installation packages, or use your 
    local system's package deployment method.
* [Node.js 16](../adr/2022-07-21_nodejs-platform.md)
  * https://nodejs.org/en/download/


* Postgres 15
  * The database doesn't *have* to be installed locally, if you already have a
    postgres DB instance somewhere - you could use that if you wanted.
  * For Windows dev environments, we generally run Postgres in a Docker
    container because it's just easier to install Docker-desktop and then
    run a container.


* Docker (optional)
  * To do basic development, you might not need Docker.
  * On Windows, the simplest approach is to use Docker-desktop:
    https://www.docker.com/products/docker-desktop/
  * It's often installed on dev environments for convenience though, e.g.
    running a postgres container.
  * It is necessary to have a local docker installation if you're developing
    /deploying Dockerfile scripts (api-svc, etc.)

# Local development setup

## Create database

Database [readme](/api-svc/db/readme.md) section "Running a local DB for 
development".

## Configure build scripts to know about your database

Database [readme](/api-svc/db/readme.md) section "Configuring 
`api-svc-db.gradle`".

## Create Database schema

Follow the api-svc [readme](/api-svc/readme.md) section "Running for local 
development".

## Configure api-svc

Follow the spring [readme](/api-svc/spring/readme.md) section "Configuring 
`api-svc-env.properties`".

## Run the api-svc

Follow the spring [readme](/api-svc/spring/readme.md) sections
"Running `Api.main()` from IDE" or "Running `Api.main()` from command line".

## Run the development Node.js web server

Follow the raid-agency-app [readme](/raid-agency-app/README.md) sections
"Running a local Node.js server in IDEA" or 
"Running a local Node.js server from command line".








