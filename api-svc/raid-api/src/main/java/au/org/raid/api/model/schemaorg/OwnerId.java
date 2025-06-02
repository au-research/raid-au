package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerId {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "PropertyValue";

    @JsonProperty("propertyID")
    @Builder.Default
    private String propertyId = "raid:owner";

    private String value;
}
