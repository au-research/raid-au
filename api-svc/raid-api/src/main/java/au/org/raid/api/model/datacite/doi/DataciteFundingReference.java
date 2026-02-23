package au.org.raid.api.model.datacite.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataciteFundingReference {
    private String funderName;
    private String funderIdentifier;
    private String funderIdentifierType;
    private String schemeUri;
    private String awardUri;
    private String awardTitle;
    private String awardNumber;
}
