
## Running a local DB

See [api-svc/readme.md](../readme.md) for instructions on running a postgres 
container and creating the schemata.


## Configuring `api-svc-env.properties`

Once you've got a DB, the `api-svc` needs to be configured.
Local dev uses a separate file like this because this is how the service is 
run - there's no Gradle files or build involved when running the service in 
a real deployment environment. 

`~/.config/raido/api-svc-env.properties`
```properties
# so you can see errors more easily in network responses
redactErrorDetails=false


```

## Running `Api.main()` from IDE

If you want to start the app from your IDE, remember to specify JVM options:

`-Duser.timezone=UTC -Dfile.encoding=UTF-8 -Duser.language= -Duser.country= -Duser.variant=`

The properties when running via main method, either from IDE or gradle (as
opposed to integration or functional test) are loaded from
`~/.config/raido/api-svc-env.properties`, see
[ApiConfig](./src/main/java/raido/apisvc/spring/config/ApiConfig.java).


## Running `Api.main()` from command line

* `cd /repository/api-svc/spring/build/libs`
* `$JAVA_HOME/bin/java  \
  -Duser.timezone=UTC -Dfile.encoding=UTF-8 \
  -Duser.language= -Duser.country= -Duser.variant= \
  -Dlogback.configurationFile=docker-logback.xml \
  -jar raido-api-svc.jar`
  * this was taken from [api-svc-start.sh](../docker/ecs/src/main/docker/script/api-svc-start.sh)

## Running integration tests

See [intTest/readme.md](./src/intTest/readme.md)


