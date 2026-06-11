package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Member {
    @JsonProperty("@type")
    private String type;
    private String identifier;
    private List<OrganizationRole> memberOf;
}
