Contains classes used for test-doubles, usually stubbing out services 
"in-memory".

Using [this definition](https://en.wikipedia.org/wiki/Test_double), these are 
somewhere between a test-stub, and a fake-object, with a pinch of "mock" and a  
dash of fairy-dust. Precise terminology is good, but try to avoid the 
bike-shedding that usually happens if you want to get into this.  

The point here is that these components aren't really meant for validating the 
outputs, they're mostly about:
* avoiding putting load on the external system
  * especially not crashing people's systems 
  * not using their resources and making them mad at us
  * not having to coordinate load-tests with various external organisations
    * fun fact: the AWS terms-of-use, require us to coordinate load-testing 
    with them 
* making the int tests run fast
* allowing us to focus on internal timings of Raido, instead of swamping the 
  load-tests with pauses (or failures) forced by the other end being slow
  * this is why sometimes you DO want to load-test against a low-fidelity 
    system, it can make life easier when you're looking at something specific


Currently used for int-test and load-test.


Ideally, we don't want this code to be in the "production" source tree - 
we never want it to run in production (they best way to avoid running specific 
code in production is to never deploy it there in the first place).

This would mean for load-tests - we'd need a separate deployment package just 
for any environment that needs to run a load-test workload.  We don't have time
to build that right now, and there's a better long-term option anyway (see 
below).

Short-term: we're just gonna add logic that fails on startup if you try to run 
stubs in an `isProd=true` environment.

As of RAID-892 that startup check exists only for the DataCite stub
(`raid.stub.datacite.enabled`). It is the only stub that drops writes rather
than standing in for a read-only lookup, so it refuses to start when
`raid.environment=prod`. The other stubs have no such check.


## DataCite stub (RAID-892)

`DataciteServiceStub` and `DataciteRepositoryClientStub` are enabled in the
test and branch environments, so that they don't create DataCite repositories
or mint DOIs. Neither action can be undone in DataCite. See "Test environments
don't create DataCite repositories or mint DOIs" in
[operational-environment.md](/doc/architecture/environment/operational-environment.md).
The RAID-812 mock server will replace this stub.


## Why in-memory stubs are not appropriate for load-testing

Test doubles almost always lower the test "fidelity" of whatever type of 
testing you're doing.

Test fidelity must means how "realistic" your system-under-test is (given 
that you're not doing your testing in production).  It's how "similar" the test
system is to a production system.  

### Test fidelity

Test fidelity covers lots of things, not just concerns about test doubles.

It's stuff like: 
* hardware 
  * EC2 instances 
    * what size, what architecture
* configuration
  * i.e. test runs on an ECS cluster with one t2.micro, prod is on a cluster 
    with 10 t5.large
* external systems
  * not just if you're running against test doubles
  * are your running against someone's "test" system
  * even if they're ok with it - does it perform the same?
    * example: APIDs service 
      * TEST env *always* takes 1500-2000ms per call
      * PROD env *usually* takes 100-200ms per call, but sometimes 1500-2000ms 
* networking
  * real world example: 
    * test runs on a *shared* 1Gb connection to the DB 
    * prod runs on a *dedicated* 10Gb connection to the DB
* database
  * what size, what type (i.e. unit testing on H2, prod on Postgres)
  * configuration 
    * i.e testing on a t2.small, while prod is on a 
      multiple-machine cluster with synchronous commit to a stand-by (that's a )
  * data
    * shape/cardinality: test entities with no children
    * size: how many root entities
    * data-fidelity: just all strings like "xxx"/"999", versus "faker" data, 
      versus real prod data


### Load-testing _specifically_ needs network calls

For load-testing, in-memory mocking diverges too much from a production 
configuration - it means that we're load-testing against a "low fidelity" 
replica of the production system.

If we're not making the network calls (using up file handles, network 
connections, etc.) - then we're not really testing the system that's 
deploying to prod. 
We're just testing an imaginary system that nobody uses. 
Without integration to the other systems, our system isn't very useful - nobody 
cares how well the imaginary load-test system performs if the production system
keeps falling over. 
With the obvious exception of generating marketing numbers 😈.

As we climb up the hierarchy of test types, from unit through integration, load 
functional, end-2-end - we want an increasing level of "test fidelity."

While even for functional tests, it may be acceptable to use in-memory 
mocking - it's not good enough for load-testing.  This is because 
*systems-integration boundaries are where performance problems live*.
So if the point of load-testing is to find and mitigate performance problems 
early - you need to hit them where they live.

Performance is a system hygiene issue, and like all hygiene issues - there's no 
"one and done" solution - it's a constant maintenance effort. 


## Long term load-testing plans

Long-term, the "in-memory" mocks should only be used by integration tests,
and can thus be moved to intTest source tree or test-shared sub-project.

Load-testing will eventually be done on a set of mock services that are on the 
other side of a network boundary.  I'm thinking we might be able to do this very 
easily with just a simple Typescript Lambda, accessed via a lambda function URL.

The DevOps team don't want us to use cloud-stuff (especially AWS-specific) for
baseline production functionality - but I think this weill be an acceptable 
trade-off. 

