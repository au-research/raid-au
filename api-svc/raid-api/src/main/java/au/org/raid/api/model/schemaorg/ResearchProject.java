package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResearchProject {
    @JsonProperty("@context")
    @Builder.Default
    private String context = "https://schema.org";

    @JsonProperty("@type")
    @Builder.Default
    private String type = "ResearchProject";

    @JsonProperty("@id")
    private String id;
    private List<Identifier> identifier;
    private String name;
    private List<String> alternateName;
    private String foundingDate;
    private String dissolutionDate;
    private List<Description> description;
    private List<Member> member;
    private List<Sponsor> sponsor;
    private String license;
    private Publisher publisher;
    private List<Keyword> keywords;
    private List<Place> location;
}
