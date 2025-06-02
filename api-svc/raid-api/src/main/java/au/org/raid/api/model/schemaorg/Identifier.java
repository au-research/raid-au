package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Identifier {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "PropertyValue";
    private String identifier;
    @JsonProperty("PropertyID")
    private String propertyId;
    private String valueReference;
}
