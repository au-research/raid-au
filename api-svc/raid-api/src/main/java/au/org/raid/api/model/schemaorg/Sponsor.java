package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Sponsor {
    @JsonProperty("@type")
    private String type = "Organization";
    @JsonProperty("@id")
    private String id;
    private String identifier;
    private OrganizationRole roleOccupation;
}
