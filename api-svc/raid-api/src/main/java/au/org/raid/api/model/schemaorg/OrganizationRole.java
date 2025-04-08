package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationRole {
    @JsonProperty("@type")
    private String type;
    private String roleName;
    private TemporalCoverage temporalCoverage;
}
