
# Broad things 

## Validation of raid data via integrations

For all the blocks marked "complete" where the values point to things like 
ORCID, ROR, etc. - ORCID and ISNI contributor identifiers are now validated
via integration clients (`OrcidClient`, `IsniClient`). ROR organisation
identifiers are validated against the ROR API.

For example, incoming ORCID urls in a raid are validated to be conformant
to the format and check-digit level, and the ORCID integration service
can verify the contributor exists.


---

# Metadata Blocks 

## 1. Identifier - Complete

Complete.

## 2. / 3. Date  - Complete

Complete.

## 4. Title - Complete

Complete, including language field.

## 5. Description - Complete

Complete, including language field.

## 6. Contributor - Complete

Complete. Supports ORCID and ISNI identifiers.

## 7. Organisation - Complete

Complete. Supports RoR identifiers.
Schema is extensible to other identifier types via `organisation_schema`.

## 8. RelatedObject - Complete

Supports any identifier via `id` + `schemaUri`. Does not validate object exists.

## 9. AlternateIdentifier - Complete

Complete.

## 10. AlternateURL - Complete

Complete.

## 11. RelatedRaid - Complete

Complete. Needs extra validation for other Raid agencies.

## 12. Subject - Complete

Complete, including language field on keywords.

## 13. TraditionalKnowledgeLabel - Complete

Complete.

## 14. SpatialCoverage - Complete

Complete, including language field on place.

## 15. Access - Complete

Complete, including language field on statement.







