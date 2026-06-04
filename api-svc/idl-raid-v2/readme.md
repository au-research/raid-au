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

# OpenAPI specifications

OpenAPI specification is generated using the json schemas from the data model project. The final specification is assembled using a custom java Gradle task that parses and transforms the RAiD data model schemas generated from the LinkML specification and appends them to the `template-raid-openapi-3.1.yaml` file. 

Changes to the endpoints and api related schemas (e.g. ValidationFailure) should be done to the `template-raid-openapi-3.1.yaml` whereas as changed to the RAiD data model must be done to the data model project. Any changes to the data model will be reflected to the OpenAPI specification through generated json schemas. 

There are two versions of the OpenAPI specification: one with enumeration specifications and one without. The "strict" version with the enums is used as the basis for java class generation.
