# Content Negotiation

## Description
Support RDF, Turtle and N-Triples as response formats in the API

## Requirements
* Do not break any exising functionality in the API
* Update the Open API schema to support 'text/turtle', 'application/n-triples' and 'application/rdf+xml' in responses
* Correct content-type should be returned depending on the Accept header sent by the client.
* The solution should use and existing Spring Boot starter available as a library or should be a custom `Converter` adhering to the interface...
```
package org.springframework.core.convert.converter;

public interface Converter<S, T> {

	T convert(S source);
}
```
* There should be unit tests to verify any new code
* Existing unit tests and integration tests should pass

## Other information
* The Open API schema can be found at api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml
* The controller affected is at api-svc/raid-api/src/main/java/au/org/raid/api/controller/RaidController.java
* Integration tests can be found at api-svc/raid-api/src/intTest