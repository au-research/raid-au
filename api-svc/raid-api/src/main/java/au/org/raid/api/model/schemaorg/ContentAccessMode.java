package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContentAccessMode {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "CreativeWork";
    private String conditionsOfAccess;
    private String accessibilitySummary;
}
