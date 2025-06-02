package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Description {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "TextObject";

    private String additionalType;
    private String inLanguage;
}
