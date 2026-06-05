package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrganizationRole {
    @Builder.Default
    @JsonProperty("@type")
    private String type = "OrganizationRole";
    private String roleName;
    private String startDate;
    private String endDate;
}
