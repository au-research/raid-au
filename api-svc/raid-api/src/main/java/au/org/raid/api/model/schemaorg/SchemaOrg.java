package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaOrg {
    @JsonProperty("@context")
    private Context context;

    @JsonProperty("@type")
    private String type = "ResearchProject";

    @JsonProperty("@id")
    private String id;
    private String identifier;
    private String name;
    private String startDate;
    private String endDate;
    private ContentAccessMode contentAccessMode;
    private List<Contributor> contributor;
    private List<Contributor> principalInvestigator;
    private List<Sponsor> sponsor;
    private String license;
    private Publisher publisher;
}
