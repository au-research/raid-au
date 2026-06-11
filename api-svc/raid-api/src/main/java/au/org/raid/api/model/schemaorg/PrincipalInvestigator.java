package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PrincipalInvestigator {
    @JsonProperty("@type")
    private String type = "Person";
    @JsonProperty("@id")
    private String id;
    private String identifier;
    private List<String> roleName;
    private OrganizationRole hasOccupation;
}
