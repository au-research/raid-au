package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Context {
    @JsonProperty("@vocab")
    private String vocab = "https://schema.org";
    private String raid = "https://raid,org";
    private String dcterms = "http://purl.org/dcterms";
    private String foaf = "http://xmlns.com/foaf/0.1/";
    private String alternateName = "https://schema.org/alternateName";
    @JsonProperty("@abstract")
    private String abstractContext = "https://schema.org/alternateName";
    private String contributor = "https://schema.org/contributor";
    private String funder = "https://schema.org/funder";
    private String sponsor = "https://schema.org/sponsor";
    private String principalInvestigator = "https://schema.org/accountablePerson";
    private String leadOrSupervisor = "https://schema.org/accountablePerson";
    private String isPartOf = "https://schema.org/isPartOf";
    private String isRelatedTo = "https://schema.org/isRelatedTo";
    private String keywords = "https://schema.org/keywords";
    private String about = "https://schema.org/about";
    private String spatialCoverage = "https://schema.org/spatialCoverage";
    private String sameAs = "https://schema.org/sameAs";
    private String identifier = "https://schema.org/identifier";
    private String contentAccessMode = "https://schema.org/accessMode";
    private String publisher = "https://schema.org/publisher";
}
