package au.org.raid.api.converter;

import au.org.raid.api.service.rdf.RaidRdfService;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Converter for RaidDto to JSON-LD format using Schema.org vocabulary
 * This implementation provides direct mapping to Schema.org types and properties
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaidJsonLdConverter implements HttpMessageConverter<RaidDto> {
    private static final MediaType JSON_LD_MEDIA_TYPE = MediaType.valueOf("application/ld+json");
    
    private final RaidRdfService raidRdfService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false; // We only support writing, not reading
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return RaidDto.class.isAssignableFrom(clazz) && 
               JSON_LD_MEDIA_TYPE.isCompatibleWith(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return List.of(JSON_LD_MEDIA_TYPE);
    }

    @Override
    public RaidDto read(Class<? extends RaidDto> clazz, HttpInputMessage inputMessage) 
            throws HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Reading JSON-LD format is not supported");
    }

    @Override
    @SneakyThrows
    public void write(RaidDto raidDto, MediaType contentType, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        // Set the JSON-LD content type in the response
        outputMessage.getHeaders().setContentType(JSON_LD_MEDIA_TYPE);
        
        // Create JSON-LD structure directly using Schema.org vocabulary
        ObjectNode jsonLd = createJsonLdFromRaid(raidDto);
        
        // Write the result to the response body
        outputMessage.getBody().write(objectMapper.writeValueAsBytes(jsonLd));
    }
    
    /**
     * Convert RaidDto to a JSON-LD structure with Schema.org vocabulary
     * @param raidDto The RaidDto to convert
     * @return ObjectNode containing the JSON-LD representation
     */
    private ObjectNode createJsonLdFromRaid(RaidDto raidDto) {
        ObjectNode root = objectMapper.createObjectNode();
        
        // Set the context that maps to Schema.org
        root.set("@context", createContext());
        
        // Set the type to ResearchProject (closest Schema.org type to a RAID)
        root.put("@type", "ResearchProject");
        
        // Set the ID of the RAID as the @id
        if (raidDto.getIdentifier() != null && raidDto.getIdentifier().getId() != null) {
            root.put("@id", raidDto.getIdentifier().getId());
            root.put("identifier", raidDto.getIdentifier().getId());
        }
        
        // Add titles
        if (raidDto.getTitle() != null && !raidDto.getTitle().isEmpty()) {
            ArrayNode names = objectMapper.createArrayNode();
            for (Title title : raidDto.getTitle()) {
                ObjectNode nameObj = objectMapper.createObjectNode();
                nameObj.put("@type", "Name");
                nameObj.put("name", title.getText());
                
                // Add type as alternateType if available
                if (title.getType() != null && title.getType().getId() != null) {
                    nameObj.put("alternateType", title.getType().getId());
                }
                
                // Add language if available
                if (title.getLanguage() != null && title.getLanguage().getId() != null) {
                    nameObj.put("inLanguage", title.getLanguage().getId());
                }
                
                // Add temporal coverage for start/end dates
                ObjectNode temporalCoverage = objectMapper.createObjectNode();
                if (title.getStartDate() != null) {
                    temporalCoverage.put("startDate", title.getStartDate());
                }
                if (title.getEndDate() != null) {
                    temporalCoverage.put("endDate", title.getEndDate());
                }
                
                if (temporalCoverage.size() > 0) {
                    nameObj.set("temporalCoverage", temporalCoverage);
                }
                
                names.add(nameObj);
            }
            
            // Use the first title as the primary name
            if (names.size() > 0) {
                JsonNode primaryName = names.get(0);
                if (primaryName.has("name")) {
                    root.put("name", primaryName.get("name").asText());
                }
            }
            
            // Add all titles as alternate names
            if (names.size() > 1) {
                root.set("alternateName", names);
            }
        }
        
        // Add descriptions
        if (raidDto.getDescription() != null && !raidDto.getDescription().isEmpty()) {
            ArrayNode descriptions = objectMapper.createArrayNode();
            for (Description desc : raidDto.getDescription()) {
                ObjectNode descObj = objectMapper.createObjectNode();
                descObj.put("@type", "Description");
                descObj.put("description", desc.getText());
                
                // Add type as alternateType if available
                if (desc.getType() != null && desc.getType().getId() != null) {
                    descObj.put("alternateType", desc.getType().getId());
                }
                
                // Add language if available
                if (desc.getLanguage() != null && desc.getLanguage().getId() != null) {
                    descObj.put("inLanguage", desc.getLanguage().getId());
                }
                
                descriptions.add(descObj);
            }
            
            // Use the first description as the primary description
            if (descriptions.size() > 0) {
                JsonNode primaryDesc = descriptions.get(0);
                if (primaryDesc.has("description")) {
                    root.put("description", primaryDesc.get("description").asText());
                }
            }
            
            // Add additional descriptions
            if (descriptions.size() > 1) {
                root.set("abstract", descriptions);
            }
        }
        
        // Add dates
        if (raidDto.getDate() != null) {
            if (raidDto.getDate().getStartDate() != null) {
                root.put("startDate", raidDto.getDate().getStartDate());
            }
            if (raidDto.getDate().getEndDate() != null) {
                root.put("endDate", raidDto.getDate().getEndDate());
            }
        }
        
        // Add access information
        if (raidDto.getAccess() != null) {
            ObjectNode accessObj = objectMapper.createObjectNode();
            
            if (raidDto.getAccess().getType() != null && raidDto.getAccess().getType().getId() != null) {
                accessObj.put("@type", "CreativeWork");
                accessObj.put("conditionsOfAccess", raidDto.getAccess().getType().getId());
            }
            
            if (raidDto.getAccess().getStatement() != null && raidDto.getAccess().getStatement().getText() != null) {
                accessObj.put("accessibilitySummary", raidDto.getAccess().getStatement().getText());
            }
            
            if (raidDto.getAccess().getEmbargoExpiry() != null) {
                accessObj.put("embargo", raidDto.getAccess().getEmbargoExpiry().toString());
            }
            
            if (accessObj.size() > 0) {
                root.set("contentAccessMode", accessObj);
            }
        }
        
        // Add contributors
        if (raidDto.getContributor() != null && !raidDto.getContributor().isEmpty()) {
            ArrayNode contributors = objectMapper.createArrayNode();
            
            for (Contributor contributor : raidDto.getContributor()) {
                ObjectNode contributorObj = objectMapper.createObjectNode();
                contributorObj.put("@type", "Person");
                
                if (contributor.getId() != null) {
                    contributorObj.put("@id", contributor.getId());
                    contributorObj.put("identifier", contributor.getId());
                }
                
                // Add email if available
                if (contributor.getEmail() != null) {
                    contributorObj.put("email", contributor.getEmail());
                }
                
                // Add roles
                if (contributor.getRole() != null && !contributor.getRole().isEmpty()) {
                    ArrayNode roles = objectMapper.createArrayNode();
                    for (ContributorRole role : contributor.getRole()) {
                        if (role.getId() != null) {
                            roles.add(role.getId());
                        }
                    }
                    if (roles.size() > 0) {
                        contributorObj.set("roleName", roles);
                    }
                }
                
                // Add positions
                if (contributor.getPosition() != null && !contributor.getPosition().isEmpty()) {
                    ArrayNode positions = objectMapper.createArrayNode();
                    for (ContributorPosition position : contributor.getPosition()) {
                        ObjectNode positionObj = objectMapper.createObjectNode();
                        positionObj.put("@type", "OrganizationRole");
                        
                        if (position.getId() != null) {
                            positionObj.put("roleName", position.getId());
                        }
                        
                        // Add temporal coverage for start/end dates
                        if (position.getStartDate() != null || position.getEndDate() != null) {
                            ObjectNode temporalCoverage = objectMapper.createObjectNode();
                            if (position.getStartDate() != null) {
                                temporalCoverage.put("startDate", position.getStartDate());
                            }
                            if (position.getEndDate() != null) {
                                temporalCoverage.put("endDate", position.getEndDate());
                            }
                            positionObj.set("temporalCoverage", temporalCoverage);
                        }
                        
                        positions.add(positionObj);
                    }
                    if (positions.size() > 0) {
                        contributorObj.set("hasOccupation", positions);
                    }
                }
                
                // Set leader flag
                Optional.ofNullable(contributor.getLeader())
                        .ifPresent(isLeader -> {
                            if (isLeader) {
                                contributorObj.put("leadOrSupervisor", true);
                            }
                        });
                
                // Set contact flag
                Optional.ofNullable(contributor.getContact())
                        .ifPresent(isContact -> {
                            if (isContact) {
                                contributorObj.put("contactPoint", true);
                            }
                        });
                
                contributors.add(contributorObj);
            }
            
            if (contributors.size() > 0) {
                root.set("contributor", contributors);
                
                // Set the first contributor as the principal investigator if they are a leader
                for (JsonNode contributor : contributors) {
                    if (contributor.has("leadOrSupervisor") && contributor.get("leadOrSupervisor").asBoolean()) {
                        root.set("principalInvestigator", contributor);
                        break;
                    }
                }
            }
        }
        
        // Add organizations
        if (raidDto.getOrganisation() != null && !raidDto.getOrganisation().isEmpty()) {
            ArrayNode organizations = objectMapper.createArrayNode();
            ArrayNode funders = objectMapper.createArrayNode();
            
            for (Organisation org : raidDto.getOrganisation()) {
                ObjectNode orgObj = objectMapper.createObjectNode();
                orgObj.put("@type", "Organization");
                
                if (org.getId() != null) {
                    orgObj.put("@id", org.getId());
                    orgObj.put("identifier", org.getId());
                }
                
                // Add roles
                boolean isFunder = false;
                if (org.getRole() != null && !org.getRole().isEmpty()) {
                    ArrayNode roles = objectMapper.createArrayNode();
                    for (OrganisationRole role : org.getRole()) {
                        ObjectNode roleObj = objectMapper.createObjectNode();
                        roleObj.put("@type", "OrganizationRole");
                        
                        if (role.getId() != null) {
                            roleObj.put("roleName", role.getId());
                            
                            // Check if this is a funding role
                            if (role.getId().toLowerCase().contains("fund") || 
                                role.getId().toLowerCase().contains("sponsor")) {
                                isFunder = true;
                            }
                        }
                        
                        // Add temporal coverage for start/end dates
                        if (role.getStartDate() != null || role.getEndDate() != null) {
                            ObjectNode temporalCoverage = objectMapper.createObjectNode();
                            if (role.getStartDate() != null) {
                                temporalCoverage.put("startDate", role.getStartDate());
                            }
                            if (role.getEndDate() != null) {
                                temporalCoverage.put("endDate", role.getEndDate());
                            }
                            roleObj.set("temporalCoverage", temporalCoverage);
                        }
                        
                        roles.add(roleObj);
                    }
                    if (roles.size() > 0) {
                        orgObj.set("roleOccupation", roles);
                    }
                }
                
                organizations.add(orgObj);
                
                // If this is a funding organization, add it to funders
                if (isFunder) {
                    funders.add(orgObj);
                }
            }
            
            if (organizations.size() > 0) {
                root.set("sponsor", organizations);
            }
            
            if (funders.size() > 0) {
                root.set("funder", funders);
            }
        }
        
        // Add related objects
        if (raidDto.getRelatedObject() != null && !raidDto.getRelatedObject().isEmpty()) {
            ArrayNode relatedItems = objectMapper.createArrayNode();
            
            for (RelatedObject relObj : raidDto.getRelatedObject()) {
                ObjectNode itemObj = objectMapper.createObjectNode();
                itemObj.put("@type", "CreativeWork");
                
                if (relObj.getId() != null) {
                    itemObj.put("@id", relObj.getId());
                    itemObj.put("identifier", relObj.getId());
                }
                
                // Add schema URI as URL
                if (relObj.getSchemaUri() != null) {
                    itemObj.put("url", relObj.getSchemaUri());
                }
                
                // Add object type
                if (relObj.getType() != null && relObj.getType().getId() != null) {
                    itemObj.put("additionalType", relObj.getType().getId());
                }
                
                // Add categories
                if (relObj.getCategory() != null && !relObj.getCategory().isEmpty()) {
                    ArrayNode categories = objectMapper.createArrayNode();
                    for (RelatedObjectCategory category : relObj.getCategory()) {
                        if (category.getId() != null) {
                            categories.add(category.getId());
                        }
                    }
                    if (categories.size() > 0) {
                        itemObj.set("category", categories);
                    }
                }
                
                relatedItems.add(itemObj);
            }
            
            if (relatedItems.size() > 0) {
                root.set("isPartOf", relatedItems);
            }
        }
        
        // Add related RAIDs
        if (raidDto.getRelatedRaid() != null && !raidDto.getRelatedRaid().isEmpty()) {
            ArrayNode relatedRaids = objectMapper.createArrayNode();
            
            for (RelatedRaid relRaid : raidDto.getRelatedRaid()) {
                ObjectNode raidObj = objectMapper.createObjectNode();
                raidObj.put("@type", "ResearchProject");
                
                if (relRaid.getId() != null) {
                    raidObj.put("@id", relRaid.getId());
                    raidObj.put("identifier", relRaid.getId());
                }
                
                // Add relation type
                if (relRaid.getType() != null && relRaid.getType().getId() != null) {
                    raidObj.put("relationshipType", relRaid.getType().getId());
                }
                
                relatedRaids.add(raidObj);
            }
            
            if (relatedRaids.size() > 0) {
                root.set("isRelatedTo", relatedRaids);
            }
        }
        
        // Add subjects
        if (raidDto.getSubject() != null && !raidDto.getSubject().isEmpty()) {
            ArrayNode subjects = objectMapper.createArrayNode();
            
            for (Subject subject : raidDto.getSubject()) {
                ObjectNode subjectObj = objectMapper.createObjectNode();
                subjectObj.put("@type", "DefinedTerm");
                
                if (subject.getId() != null) {
                    subjectObj.put("termCode", subject.getId());
                }
                
                if (subject.getSchemaUri() != null) {
                    subjectObj.put("inDefinedTermSet", subject.getSchemaUri());
                }
                
                // Add keywords
                if (subject.getKeyword() != null && !subject.getKeyword().isEmpty()) {
                    ArrayNode keywords = objectMapper.createArrayNode();
                    for (SubjectKeyword keyword : subject.getKeyword()) {
                        if (keyword.getText() != null) {
                            ObjectNode keywordObj = objectMapper.createObjectNode();
                            keywordObj.put("@type", "DefinedTerm");
                            keywordObj.put("name", keyword.getText());
                            
                            // Add language if available
                            if (keyword.getLanguage() != null && keyword.getLanguage().getId() != null) {
                                keywordObj.put("inLanguage", keyword.getLanguage().getId());
                            }
                            
                            keywords.add(keywordObj);
                        }
                    }
                    if (keywords.size() > 0) {
                        subjectObj.set("hasDefinedTerm", keywords);
                        
                        // Extract plain keywords for Schema.org keywords field
                        ArrayNode plainKeywords = objectMapper.createArrayNode();
                        for (SubjectKeyword keyword : subject.getKeyword()) {
                            if (keyword.getText() != null) {
                                plainKeywords.add(keyword.getText());
                            }
                        }
                        if (plainKeywords.size() > 0) {
                            root.set("keywords", plainKeywords);
                        }
                    }
                }
                
                subjects.add(subjectObj);
            }
            
            if (subjects.size() > 0) {
                root.set("about", subjects);
            }
        }
        
        // Add spatial coverage
        if (raidDto.getSpatialCoverage() != null && !raidDto.getSpatialCoverage().isEmpty()) {
            ArrayNode locations = objectMapper.createArrayNode();
            
            for (SpatialCoverage coverage : raidDto.getSpatialCoverage()) {
                // Add places
                if (coverage.getPlace() != null && !coverage.getPlace().isEmpty()) {
                    for (SpatialCoveragePlace place : coverage.getPlace()) {
                        ObjectNode placeObj = objectMapper.createObjectNode();
                        placeObj.put("@type", "Place");
                        
                        if (place.getText() != null) {
                            placeObj.put("name", place.getText());
                        }
                        
                        // Add language if available
                        if (place.getLanguage() != null && place.getLanguage().getId() != null) {
                            placeObj.put("inLanguage", place.getLanguage().getId());
                        }
                        
                        locations.add(placeObj);
                    }
                }
            }
            
            if (locations.size() > 0) {
                root.set("spatialCoverage", locations);
            }
        }
        
        // Add alternate URLs
        if (raidDto.getAlternateUrl() != null && !raidDto.getAlternateUrl().isEmpty()) {
            ArrayNode urls = objectMapper.createArrayNode();
            
            for (AlternateUrl url : raidDto.getAlternateUrl()) {
                if (url.getUrl() != null) {
                    urls.add(url.getUrl());
                }
            }
            
            if (urls.size() > 0) {
                if (urls.size() == 1) {
                    root.put("url", urls.get(0).asText());
                } else {
                    root.set("sameAs", urls);
                }
            }
        }
        
        // Add alternate identifiers
        if (raidDto.getAlternateIdentifier() != null && !raidDto.getAlternateIdentifier().isEmpty()) {
            ArrayNode identifiers = objectMapper.createArrayNode();
            
            for (AlternateIdentifier altId : raidDto.getAlternateIdentifier()) {
                ObjectNode idObj = objectMapper.createObjectNode();
                idObj.put("@type", "PropertyValue");
                
                if (altId.getId() != null) {
                    idObj.put("value", altId.getId());
                }
                
                if (altId.getType() != null) {
                    idObj.put("propertyID", altId.getType());
                }
                
                identifiers.add(idObj);
            }
            
            if (identifiers.size() > 0) {
                root.set("identifier", identifiers);
            }
        }
        
        // Add license information
        if (raidDto.getIdentifier() != null && raidDto.getIdentifier().getLicense() != null) {
            root.put("license", raidDto.getIdentifier().getLicense());
        }
        
        // Add registry agency information
        if (raidDto.getIdentifier() != null && raidDto.getIdentifier().getRegistrationAgency() != null) {
            ObjectNode agency = objectMapper.createObjectNode();
            agency.put("@type", "Organization");
            
            if (raidDto.getIdentifier().getRegistrationAgency().getId() != null) {
                agency.put("@id", raidDto.getIdentifier().getRegistrationAgency().getId());
                agency.put("identifier", raidDto.getIdentifier().getRegistrationAgency().getId());
            }
            
            if (raidDto.getIdentifier().getRegistrationAgency().getSchemaUri() != null) {
                agency.put("url", raidDto.getIdentifier().getRegistrationAgency().getSchemaUri());
            }
            
            root.set("publisher", agency);
        }
        
        return root;
    }
    
    /**
     * Create the JSON-LD context that maps RAID properties to Schema.org vocabulary
     * @return ObjectNode containing the @context definition
     */
    private ObjectNode createContext() {
        ObjectNode context = objectMapper.createObjectNode();
        
        // Set the base context to Schema.org
        context.put("@vocab", "https://schema.org/");
        
        // Add additional namespaces
        context.put("raid", "https://raid.org/schema#");
        context.put("dcterms", "http://purl.org/dc/terms/");
        context.put("foaf", "http://xmlns.com/foaf/0.1/");
        context.put("skos", "http://www.w3.org/2004/02/skos/core#");
        
        // Add property mappings
        context.put("alternateName", "https://schema.org/alternateName");
        context.put("abstract", "https://schema.org/abstract");
        context.put("contributor", "https://schema.org/contributor");
        context.put("funder", "https://schema.org/funder");
        context.put("sponsor", "https://schema.org/sponsor");
        context.put("principalInvestigator", "https://schema.org/accountablePerson");
        context.put("leadOrSupervisor", "https://schema.org/accountablePerson");
        context.put("isPartOf", "https://schema.org/isPartOf");
        context.put("isRelatedTo", "https://schema.org/isRelatedTo");
        context.put("keywords", "https://schema.org/keywords");
        context.put("about", "https://schema.org/about");
        context.put("spatialCoverage", "https://schema.org/spatialCoverage");
        context.put("sameAs", "https://schema.org/sameAs");
        context.put("identifier", "https://schema.org/identifier");
        context.put("contentAccessMode", "https://schema.org/accessMode");
        context.put("publisher", "https://schema.org/publisher");
        
        return context;
    }
}