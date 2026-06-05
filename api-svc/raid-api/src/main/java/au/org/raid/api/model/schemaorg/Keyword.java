package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Keyword {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "DefinedTerm";
    private String termCode;
    private String inDefinedTermSet;
    private String alternateName;
}
