package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Person {
    @Builder.Default
    @JsonProperty("@type")
    private String type = "Person";
    private String identifier;
    private List<OrganizationRole> memberOf;
}
