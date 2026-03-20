package au.org.raid.api.service.rdf;

import au.org.raid.idl.raidv2.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for converting RaidDto to RDF model
 */
@Slf4j
@Service
public class RaidRdfService {

    private static final String RAID_TYPE = "https://raid.org/schema#RAiD";
    private static final String RAID_NS = "https://raid.org/schema#";
    private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    private static final String VCARD_NS = "http://www.w3.org/2006/vcard/ns#";
    private static final String COAR_NS = "https://vocabularies.coar-repositories.org/";
    private static final String SCHEMA_NS = "https://schema.org/";
    private static final String BIBO_NS = "http://purl.org/ontology/bibo/";
    private static final String GEO_NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";

    /**
     * Convert a RaidDto to a Jena RDF Model
     * @param raidDto The RaidDto to convert
     * @return RDF Model representation of the RaidDto
     */
    public Model toRdfModel(RaidDto raidDto) {
        Model model = ModelFactory.createDefaultModel();
        
        // Set common namespaces
        model.setNsPrefix("raid", RAID_NS);
        model.setNsPrefix("dc", DCTerms.NS);
        model.setNsPrefix("foaf", FOAF_NS);
        model.setNsPrefix("vcard", VCARD_NS);
        model.setNsPrefix("skos", SKOS.getURI());
        model.setNsPrefix("coar", COAR_NS);
        model.setNsPrefix("schema", SCHEMA_NS);
        model.setNsPrefix("bibo", BIBO_NS);
        model.setNsPrefix("geo", GEO_NS);

        // Create property constants for core properties
        Property hasTitle = model.createProperty(RAID_NS, "hasTitle");
        Property hasDescription = model.createProperty(RAID_NS, "hasDescription");
        Property hasIdentifier = model.createProperty(DCTerms.NS, "identifier");
        Property hasSchemaUri = model.createProperty(RAID_NS, "schemaUri");
        Property hasStartDate = model.createProperty(RAID_NS, "startDate");
        Property hasEndDate = model.createProperty(RAID_NS, "endDate");
        Property hasVersion = model.createProperty(RAID_NS, "version");
        Property hasLicense = model.createProperty(DCTerms.NS, "license");
        Property hasRaidAgencyUrl = model.createProperty(RAID_NS, "raidAgencyUrl");
        Property hasLanguage = model.createProperty(DCTerms.NS, "language");
        
        // Create properties for access-related attributes
        Property hasAccess = model.createProperty(RAID_NS, "hasAccess");
        Property hasAccessType = model.createProperty(RAID_NS, "accessType");
        Property hasAccessStatement = model.createProperty(RAID_NS, "accessStatement");
        Property hasEmbargoExpiry = model.createProperty(RAID_NS, "embargoExpiry");
        
        // Create properties for contributors and organizations
        Property hasContributor = model.createProperty(RAID_NS, "hasContributor");
        Property hasOrganisation = model.createProperty(RAID_NS, "hasOrganisation");
        Property hasPosition = model.createProperty(RAID_NS, "hasPosition");
        Property hasRole = model.createProperty(RAID_NS, "hasRole");
        Property isLeader = model.createProperty(RAID_NS, "isLeader");
        Property isContact = model.createProperty(RAID_NS, "isContact");
        Property hasEmail = model.createProperty(VCARD_NS, "email");
        Property hasUuid = model.createProperty(RAID_NS, "uuid");
        Property hasStatus = model.createProperty(RAID_NS, "status");
        
        // Create properties for related objects and raids
        Property hasRelatedObject = model.createProperty(RAID_NS, "hasRelatedObject");
        Property hasRelatedRaid = model.createProperty(RAID_NS, "hasRelatedRaid");
        Property hasRelationType = model.createProperty(RAID_NS, "relationType");
        Property hasCategory = model.createProperty(RAID_NS, "category");
        
        // Create properties for additional elements
        Property hasAlternateUrl = model.createProperty(RAID_NS, "alternateUrl");
        Property hasAlternateIdentifier = model.createProperty(RAID_NS, "alternateIdentifier");
        Property hasSpatialCoverage = model.createProperty(DCTerms.NS, "spatial");
        Property hasPlace = model.createProperty(SCHEMA_NS, "Place");
        Property hasSubject = model.createProperty(DCTerms.NS, "subject");
        Property hasKeyword = model.createProperty(RAID_NS, "keyword");
        
        // Create properties for identifier elements
        Property hasRegistrationAgency = model.createProperty(RAID_NS, "registrationAgency");
        Property hasOwner = model.createProperty(RAID_NS, "owner");
        Property hasServicePoint = model.createProperty(RAID_NS, "servicePoint");

        // Create the RAID resource
        Resource raid = model.createResource(raidDto.getIdentifier().getId());
        raid.addProperty(RDF.type, model.createResource(RAID_TYPE));
        
        // Add identifier properties
        raid.addProperty(hasIdentifier, raidDto.getIdentifier().getId());
        raid.addProperty(hasSchemaUri, raidDto.getIdentifier().getSchemaUri().getValue());
        
        if (raidDto.getIdentifier().getVersion() != null) {
            raid.addProperty(hasVersion, String.valueOf(raidDto.getIdentifier().getVersion()));
        }
        
        if (raidDto.getIdentifier().getLicense() != null) {
            raid.addProperty(hasLicense, raidDto.getIdentifier().getLicense());
        }
        
        if (raidDto.getIdentifier().getRaidAgencyUrl() != null) {
            raid.addProperty(hasRaidAgencyUrl, raidDto.getIdentifier().getRaidAgencyUrl());
        }
        
        // Add registration agency
        if (raidDto.getIdentifier().getRegistrationAgency() != null) {
            RegistrationAgency regAgency = raidDto.getIdentifier().getRegistrationAgency();
            Resource regAgencyResource = model.createResource(regAgency.getId())
                    .addProperty(hasSchemaUri, regAgency.getSchemaUri().getValue());
            raid.addProperty(hasRegistrationAgency, regAgencyResource);
        }
        
        // Add owner information
        if (raidDto.getIdentifier().getOwner() != null) {
            Owner owner = raidDto.getIdentifier().getOwner();
            Resource ownerResource = model.createResource(owner.getId())
                    .addProperty(hasSchemaUri, owner.getSchemaUri().getValue());
            
            if (owner.getServicePoint() != null) {
                ownerResource.addProperty(hasServicePoint, String.valueOf(owner.getServicePoint()));
            }
            
            raid.addProperty(hasOwner, ownerResource);
        }
        
        // Add date information
        if (raidDto.getDate() != null) {
            if (raidDto.getDate().getStartDate() != null) {
                raid.addProperty(hasStartDate, raidDto.getDate().getStartDate().toString());
            }
            if (raidDto.getDate().getEndDate() != null) {
                raid.addProperty(hasEndDate, raidDto.getDate().getEndDate().toString());
            }
        }

        // Add titles
        if (raidDto.getTitle() != null) {
            raidDto.getTitle().forEach(title -> {
                Resource titleResource = model.createResource()
                        .addProperty(DCTerms.title, title.getText());
                
                if (title.getType() != null) {
                    titleResource.addProperty(DCTerms.type, title.getType().getId().getValue());
                }
                
                if (title.getStartDate() != null) {
                    titleResource.addProperty(hasStartDate, title.getStartDate().toString());
                }
                
                if (title.getEndDate() != null) {
                    titleResource.addProperty(hasEndDate, title.getEndDate().toString());
                }
                
                if (title.getLanguage() != null) {
                    titleResource.addProperty(hasLanguage, title.getLanguage().getId());
                }
                
                raid.addProperty(hasTitle, titleResource);
            });
        }

        // Add descriptions
        if (raidDto.getDescription() != null) {
            raidDto.getDescription().forEach(description -> {
                Resource descResource = model.createResource()
                        .addProperty(DCTerms.description, description.getText());
                
                if (description.getType() != null) {
                    descResource.addProperty(DCTerms.type, description.getType().getId().getValue());
                }
                
                if (description.getLanguage() != null) {
                    descResource.addProperty(hasLanguage, description.getLanguage().getId());
                }
                
                raid.addProperty(hasDescription, descResource);
            });
        }
        
        // Add access information
        if (raidDto.getAccess() != null) {
            Access access = raidDto.getAccess();
            Resource accessResource = model.createResource();
            
            if (access.getType() != null) {
                accessResource.addProperty(hasAccessType, access.getType().getId().getValue());
            }
            
            if (access.getStatement() != null) {
                Resource statementResource = model.createResource();
                statementResource.addProperty(SKOS.prefLabel, access.getStatement().getText());
                
                if (access.getStatement().getLanguage() != null) {
                    statementResource.addProperty(hasLanguage, access.getStatement().getLanguage().getId());
                }
                
                accessResource.addProperty(hasAccessStatement, statementResource);
            }
            
            if (access.getEmbargoExpiry() != null) {
                accessResource.addProperty(hasEmbargoExpiry, access.getEmbargoExpiry().toString());
            }
            
            raid.addProperty(hasAccess, accessResource);
        }
        
        // Add contributors
        if (raidDto.getContributor() != null) {
            raidDto.getContributor().forEach(contributor -> {
                Resource contributorResource = model.createResource(contributor.getId())
                        .addProperty(hasSchemaUri, contributor.getSchemaUri().getValue());
                
                // Add contributor status
                Optional.ofNullable(contributor.getStatus())
                        .ifPresent(status -> contributorResource.addProperty(hasStatus, status));
                
                // Add contributor email
                Optional.ofNullable(contributor.getEmail())
                        .ifPresent(email -> contributorResource.addProperty(hasEmail, email));
                
                // Add contributor UUID
                Optional.ofNullable(contributor.getUuid())
                        .ifPresent(uuid -> contributorResource.addProperty(hasUuid, uuid));
                
                // Add leader and contact flags
                Optional.ofNullable(contributor.getLeader())
                        .ifPresent(leader -> {
                            if (leader) {
                                contributorResource.addProperty(isLeader, "true");
                            }
                        });
                
                Optional.ofNullable(contributor.getContact())
                        .ifPresent(contact -> {
                            if (contact) {
                                contributorResource.addProperty(isContact, "true");
                            }
                        });
                
                // Add positions
                if (contributor.getPosition() != null) {
                    contributor.getPosition().forEach(position -> {
                        Resource positionResource = model.createResource(position.getId().getValue())
                                .addProperty(hasSchemaUri, position.getSchemaUri().getValue());
                        
                        if (position.getStartDate() != null) {
                            positionResource.addProperty(hasStartDate, position.getStartDate().toString());
                        }
                        
                        if (position.getEndDate() != null) {
                            positionResource.addProperty(hasEndDate, position.getEndDate().toString());
                        }
                        
                        contributorResource.addProperty(hasPosition, positionResource);
                    });
                }
                
                // Add roles
                if (contributor.getRole() != null) {
                    contributor.getRole().forEach(role -> {
                        Resource roleResource = model.createResource(role.getId().getValue())
                                .addProperty(hasSchemaUri, role.getSchemaUri().getValue());
                        
                        contributorResource.addProperty(hasRole, roleResource);
                    });
                }
                
                raid.addProperty(hasContributor, contributorResource);
            });
        }
        
        // Add organizations
        if (raidDto.getOrganisation() != null) {
            raidDto.getOrganisation().forEach(organization -> {
                Resource orgResource = model.createResource(organization.getId())
                        .addProperty(hasSchemaUri, organization.getSchemaUri().getValue());
                
                // Add roles
                if (organization.getRole() != null) {
                    organization.getRole().forEach(role -> {
                        Resource roleResource = model.createResource(role.getId().getValue())
                                .addProperty(hasSchemaUri, role.getSchemaUri().getValue());
                        
                        if (role.getStartDate() != null) {
                            roleResource.addProperty(hasStartDate, role.getStartDate().toString());
                        }
                        
                        if (role.getEndDate() != null) {
                            roleResource.addProperty(hasEndDate, role.getEndDate().toString());
                        }
                        
                        orgResource.addProperty(hasRole, roleResource);
                    });
                }
                
                raid.addProperty(hasOrganisation, orgResource);
            });
        }
        
        // Add subjects
        if (raidDto.getSubject() != null) {
            raidDto.getSubject().forEach(subject -> {
                Resource subjectResource = model.createResource(subject.getId())
                        .addProperty(hasSchemaUri, subject.getSchemaUri().getValue());
                
                // Add keywords
                if (subject.getKeyword() != null) {
                    subject.getKeyword().forEach(keyword -> {
                        Resource keywordResource = model.createResource()
                                .addProperty(SKOS.prefLabel, keyword.getText());
                        
                        if (keyword.getLanguage() != null) {
                            keywordResource.addProperty(hasLanguage, keyword.getLanguage().getId());
                        }
                        
                        subjectResource.addProperty(hasKeyword, keywordResource);
                    });
                }
                
                raid.addProperty(hasSubject, subjectResource);
            });
        }
        
        // Add related RAIDs
        if (raidDto.getRelatedRaid() != null) {
            raidDto.getRelatedRaid().forEach(relatedRaid -> {
                Resource relatedRaidResource = model.createResource(relatedRaid.getId());
                
                if (relatedRaid.getType() != null) {
                    relatedRaidResource.addProperty(hasRelationType, model.createResource(relatedRaid.getType().getId().getValue())
                            .addProperty(hasSchemaUri, relatedRaid.getType().getSchemaUri().getValue()));
                }
                
                raid.addProperty(hasRelatedRaid, relatedRaidResource);
            });
        }
        
        // Add related objects
        if (raidDto.getRelatedObject() != null) {
            raidDto.getRelatedObject().forEach(relatedObject -> {
                Resource relatedObjectResource = model.createResource(relatedObject.getId())
                        .addProperty(hasSchemaUri, relatedObject.getSchemaUri().getValue());
                
                if (relatedObject.getType() != null) {
                    relatedObjectResource.addProperty(DCTerms.type, model.createResource(relatedObject.getType().getId().getValue())
                            .addProperty(hasSchemaUri, relatedObject.getType().getSchemaUri().getValue()));
                }
                
                // Add categories
                if (relatedObject.getCategory() != null) {
                    relatedObject.getCategory().forEach(category -> {
                        relatedObjectResource.addProperty(hasCategory, model.createResource(category.getId().getValue())
                                .addProperty(hasSchemaUri, category.getSchemaUri().getValue()));
                    });
                }
                
                raid.addProperty(hasRelatedObject, relatedObjectResource);
            });
        }
        
        // Add alternate URLs
        if (raidDto.getAlternateUrl() != null) {
            raidDto.getAlternateUrl().forEach(url -> {
                raid.addProperty(hasAlternateUrl, url.getUrl());
            });
        }
        
        // Add alternate identifiers
        if (raidDto.getAlternateIdentifier() != null) {
            raidDto.getAlternateIdentifier().forEach(altId -> {
                Resource altIdResource = model.createResource();
                
                if (altId.getId() != null) {
                    altIdResource.addProperty(DCTerms.identifier, altId.getId());
                }
                
                if (altId.getType() != null) {
                    altIdResource.addProperty(DCTerms.type, altId.getType());
                }
                
                raid.addProperty(hasAlternateIdentifier, altIdResource);
            });
        }
        
        // Add spatial coverage
        if (raidDto.getSpatialCoverage() != null) {
            raidDto.getSpatialCoverage().forEach(spatialCoverage -> {
                Resource spatialResource = model.createResource(spatialCoverage.getId())
                        .addProperty(hasSchemaUri, spatialCoverage.getSchemaUri().getValue());
                
                // Add places
                if (spatialCoverage.getPlace() != null) {
                    spatialCoverage.getPlace().forEach(place -> {
                        Resource placeResource = model.createResource()
                                .addProperty(SKOS.prefLabel, place.getText());
                        
                        if (place.getLanguage() != null) {
                            placeResource.addProperty(hasLanguage, place.getLanguage().getId());
                        }
                        
                        spatialResource.addProperty(hasPlace, placeResource);
                    });
                }
                
                raid.addProperty(hasSpatialCoverage, spatialResource);
            });
        }

        return model;
    }
}