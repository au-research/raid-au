# Task: Implement JSON-LD Format Support in RAID API

**IMPORTANT: Update the OpenAPI schema at `api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml` with NO BREAKING CHANGES**

## Overview
We need to implement content negotiation in our Spring Boot RAID API to support Schema.org metadata via JSON-LD. This will allow clients to request data in JSON-LD format by using the Accept header.

## Requirements
1. Update the OpenAPI specification at `api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml` to include the new JSON-LD content type while ensuring **NO BREAKING CHANGES** to existing API contracts
2. Support the format:
   - application/ld+json (JSON-LD format for Schema.org)
3. Implement using Spring's content negotiation features and `HttpMessageConverter` pattern
4. Maintain clean controller code by moving format conversion logic to separate classes
5. Use Schema.org vocabulary for the JSON-LD format with direct mapping to appropriate Schema.org types

## Schema.org Mapping Requirements

Use the following Schema.org mappings for RAID data:

### Core Entity Type
- Map RAIDs to `schema:ResearchProject` (not Dataset) as this better represents the nature of RAIDs

### Property Mappings
| RAID Data Element | Schema.org Property |
|-------------------|---------------------|
| Handle/ID | `@id` and `schema:identifier` |
| Title | `schema:name` (primary) and `schema:alternateName` (additional) |
| Description | `schema:description` (primary) and `schema:abstract` (additional) |
| Start/End Date | `schema:startDate` and `schema:endDate` |
| Contributors | `schema:contributor` as `schema:Person` objects |
| Lead Contributors | Include as `schema:principalInvestigator` |
| Organizations | `schema:sponsor` |
| Funding Organizations | `schema:funder` |
| Access Information | `schema:contentAccessMode` |
| Related RAIDs | `schema:isRelatedTo` |
| Related Objects | `schema:isPartOf` |
| Subjects/Keywords | `schema:about` and `schema:keywords` |
| Spatial Coverage | `schema:spatialCoverage` |
| Alternate URLs | `schema:url` (single) or `schema:sameAs` (multiple) |
| License | `schema:license` |
| Registration Agency | `schema:publisher` |

### JSON-LD Structure Requirements
- Include a complete `@context` section with schema.org vocabulary
- Use nested objects for complex properties (contributors, organizations, etc.)
- Include proper type information with `@type` properties
- Map temporal information (dates) correctly
- Provide both human-readable values and URIs where appropriate

## Implementation Details

### 1. Update OpenAPI Schema
Modify the OpenAPI schema at `api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml` to include the application/ld+json content type in relevant endpoints, ensuring these changes don't break existing API contracts.

### 2. Create JSON-LD Converter
Create a `RaidJsonLdConverter` class that implements `HttpMessageConverter<RaidDto>` with the following core features:
- Direct mapping to Schema.org using Jackson ObjectMapper
- Proper @context declaration
- Full structured mapping of all RAID properties

### 3. Configure Content Negotiation
Update the WebConfig to:
- Register the new converter
- Configure content negotiation for the application/ld+json media type

### 4. Implementation Approach
For the JSON-LD converter:
1. Create a structured, type-safe converter rather than using a general-purpose RDF library
2. Build the JSON-LD structure directly using Jackson's ObjectNode/ArrayNode
3. Create helper methods for mapping complex properties
4. Include proper contextual information for Schema.org compatibility

## Example JSON-LD Output
The output should follow this structure:

```json
{
  "@context": {
    "@vocab": "https://schema.org/",
    "raid": "https://raid.org/schema#",
    "dcterms": "http://purl.org/dc/terms/",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "skos": "http://www.w3.org/2004/02/skos/core#"
  },
  "@type": "ResearchProject",
  "@id": "https://raid.org/10.25.1.1/abcde",
  "identifier": "https://raid.org/10.25.1.1/abcde",
  "name": "Primary RAID Title",
  "description": "Primary description of the RAID",
  "startDate": "2023-01-01",
  "endDate": "2025-12-31",
  "contributor": [
    {
      "@type": "Person",
      "@id": "https://orcid.org/0000-0001-2345-6789",
      "identifier": "https://orcid.org/0000-0001-2345-6789",
      "email": "researcher@example.com",
      "roleName": ["https://credit.niso.org/contributor-roles/investigation/"]
    }
  ],
  "sponsor": [
    {
      "@type": "Organization",
      "@id": "https://ror.org/12345abcde",
      "identifier": "https://ror.org/12345abcde"
    }
  ],
  "about": [
    {
      "@type": "DefinedTerm",
      "termCode": "https://subject-id.org/123",
      "inDefinedTermSet": "https://vocabulary.example.org/"
    }
  ],
  "license": "CC-BY-4.0"
}
```

## Implementation Strategy
1. Implement a createJsonLdFromRaid method that builds the JSON-LD structure directly
2. Create helper methods for each major component (contributors, organizations, etc.)
3. Add proper type information with @type properties
4. Build a comprehensive @context section
5. Include detailed Schema.org mappings
6. Implement unit tests to verify structure and content

## Testing
Create thorough tests that verify:
1. The converter produces valid JSON-LD
2. The output includes proper Schema.org mappings
3. All required properties are correctly mapped
4. The @context properly defines all used terms
5. The structure meets Schema.org expectations

Also test the API endpoint with the Accept header:

```
GET /raid/10.25.1.1/abcde
Accept: application/ld+json
```

## Deliverables
1. Updated OpenAPI schema in `api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml`
2. RaidJsonLdConverter implementation with complete Schema.org mapping
3. WebConfig for content negotiation configuration
4. Unit tests for the JSON-LD converter
5. Integration tests for the API endpoint