

Intended to by a quick-start guide, focused on what a client needs to do to 
get up and running with the RAiD API and minting raids.


# Business onboarding

## Sign an agreement with the ARDC

* send an email to `contact@raid.org`
* you will be required to make an agreement with the ARDC about your usage 
  of RAiD

## Have your service-point created

As part of the business onboarding process, we will create a service-point 
that identifies your usage of RAiD for your institution.

A description of the relationship between raids, service-points and api-keys
can be found in the [permission-model](./permission-model.md).


# Technical onboarding

## Concepts

### Service Level Guide

See the [service-level-guide](/doc/service-level-guide.md) for details about
support, maintenance windows, etc.


### Environments:  DEMO vs PROD

Testing client-integration functionality should be done against the DEMO
environment.  The example commands in the [minting a raid](./minting-a-raid.md)
guide are hardcode for DEMO environment.


* DEMO - "pre-production" environment.  
  * There is only one - we don't currently maintain multiple pre-prod 
    environments.
  * The raid-agency-app UI is available at https://app.demo.raid.org.au
  * The API is available at https://api.demo.raid.org.au

* PROD - production environment
  * The raid-agency-app UI is available at https://app.prod.raid.org.au
  * The API is available at https://api.prod.raid.org.au


### Experimental vs Stable API

Every endpoint in the [RAiD OpenAPI](/api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml)
definition is tagged as either "stable" or "experimental", client integration
targeted at production deployment should use only stable APIs, the 
[service-level-guide](/doc/service-level-guide.md) talks explicitly about out
commitment to supporting stable API endpoints.


### Open vs Closed raid data

RAiD has the concept of "closed" and "open" raid data.
When you select that a raid should be "closed", it means that no metadata 
is made available to the public.

For closed raids, the landing page shows only that the raid handle exists, 
that it is closed and the accompanying "access statement" (required for closed 
raids) which should describe how to obtain more information about the research 
project.


### Future global raid.org infra

In the future, there will be a centralised set of functionality available at
raid.org that people around the world will be able to use (without signing in) 
to resolve and search raid data.

The global raid infrastructure should generally not be of too much concern to 
you as an integrator to RAiD.  Your agreement is with the ARDC for integration 
to the Oceania region raid-agency, which is currently maintained and operated 
by the ARDC.

---

Once a service-point has been created for you and you have a good grasp of the 
concepts, follow the [minting a raid](./minting-a-raid.md) guide to get started. 
