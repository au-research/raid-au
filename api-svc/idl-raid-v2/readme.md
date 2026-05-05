The OpenAPI definition files are in the [`/src directory`](./src).
The top-level spec file is [`src/raido-openapi-3.0.yaml`](./src/raido-openapi-3.0.yaml).

The OpenAPI definition is used to generate Java code for the 
`/api-svc/raid-api` project. Generated model and API classes are written to
`src/main/generated` within this module.

Swagger UI can be found at https://api.prod.raid.org.au/swagger-ui/index.html 

# Generating code from the spec

Generating code is done via [openapi-generator](https://github.com/OpenAPITools/openapi-generator)
using their [Gradle plugin](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-gradle-plugin/README.adoc).

Java code is generated from this project, via the Gradle 
[openApiGenerate](./build.gradle) task.
The `compileJava` task depends on `openApiGenerate`, so a normal build
will regenerate the code automatically.
