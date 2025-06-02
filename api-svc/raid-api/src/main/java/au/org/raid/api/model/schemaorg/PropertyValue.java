package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PropertyValue {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "PropertyValue";
    private String propertyId;
    private String value;
    private String valueReference;
}
