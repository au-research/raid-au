See Gradle [buildSrc doco](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources).

# Custom tasks 

## Configuration 

Example of the configuration structure:

```yaml
  enumID: DescriptionTypeSchemaURIEnum
  table: description_type_schema
  source: prefLabel
  values:
    enumID: DescriptionTypeIdEnum
    table: description_type
    source: uri
```

`enumID` refers to the id of the enumeration in the data model (core-enums.yaml or extended-enums.yaml).

`table` is the name of the database table where the values will be stored. This is used by the `GenerateRefernceDataTask` task.

`source` must have either `prefLabel` or `uri` value. This values is used to determine whether the allowed values is the URI of the vocabulary concept or the preferred label. For example, organisation role schema uri vocabulary concept (e.g. https://vocabulary.raid.org/organisation.role.schemaUri/282) uses the prefLabels as the allowed values where as the organisation role id vocabulary concepts (e.g.https://vocabulary.raid.org/organisation.role.schema/182) use the value of the concept URI. For this reason, the enum2table configuration includes `source: prefLabel|uri` information. 

`values` is used with dependent vocabularies i.e. schemaURI and values in that schema. 

Configuration must include all enumerations described in the data model i.e. if new enumerations are added to the data model a corresponding configuration must also be added to the `enum2table` or `extended-enum2table` configuration. 

## AddStaticEnums

This task adds the vocabulary values as enums to the JSON Schemas. The task is run as part of the data model project to create an extended version of the json schema generated from the linkml specification.

Configuration:
```
linkMLEnumsFile = file("src/v2/core-enums.yaml")
enumInfoFile = file("../../buildSrc/enum2table.yaml")
generatedSchemaFile = file("generated/v2/raid-jsonschema.json")
outputFile = file("generated/v2/raid-internal-jsonschema.json")
```

## AssembleOpenAPI

This task builds OpenAPI specification by combining the OpenAPI template file with the JSON schema files. JSON schema are first transformed to yaml and them the structure is changed to match the structure of the OpenAPI specification for describing schemas. Finally the result is appended to the OpenAPI template file. 

The task is used in two occasions as part of the idl-raid project to generate the final RAiD OpenAPI specifications.

```
tasks.register('assembleOpenAPIV2Specs', AssembleOpenAPI) {
  openAPITemplateFile = file("src/template-raid-openapi-3.1.yaml")
  generatedSchemaFile = file("../datamodel/generated/v2/raid-jsonschema.json")
  outputFile = file("src/raid-openapi-3.1.yaml")
}

tasks.register('assembleOpenAPIV2InternalSpecs', AssembleOpenAPI) {
  openAPITemplateFile = file("src/template-raid-openapi-3.1.yaml")
  generatedSchemaFile = file("../datamodel/generated/v2/raid-internal-jsonschema.json")
  outputFile = file("src/raid-openapi-strict-3.1.yaml")
}
```

## GenerateReferenceDataTask

This task fetches the latest versions of the RAiD data model related vocabularies an generates two output versions:

* Flyway migration file that populates the vocabulary related tables
* Lists of allowed values for each enumeration to the `api-svc/datamodel/examples` directory. These are used by the linkml documentation generator.  

```
tasks.register('generateReferenceData', GenerateReferenceDataTask) {
dataModelPath = 'api-svc/datamodel/src/v2/core-enums.yaml'
outputFile = "api-svc/datamodel/generated/v2/referencedata.sql"
examplesDir = "api-svc/datamodel/examples/"
mappingFile = "buildSrc/enum2table.yaml"
schemaID = 3
}

tasks.register('generateExtendedReferenceData', GenerateReferenceDataTask) {
dataModelPath = 'api-svc/datamodel/src/v2/extended-enums.yaml'
outputFile = "api-svc/datamodel/generated/v2/referencedata-extended.sql"
examplesDir = "api-svc/datamodel/examples/"
mappingFile = "buildSrc/extended-enum2table.yaml"
schemaID = 3
}
```


