package au.org.raid.api.dto.orcid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrcidName {
    @JsonProperty("given-names")
    private OrcidStringValue givenNames;
    @JsonProperty("family-name")
    private OrcidStringValue familyName;
}
