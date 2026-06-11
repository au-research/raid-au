package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Place {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "Place";
    private String identifier;
    private String name;
}
