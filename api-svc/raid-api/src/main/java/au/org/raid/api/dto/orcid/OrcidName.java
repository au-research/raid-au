package au.org.raid.api.dto.orcid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class OrcidName {
    @JsonProperty("given-names")
    private OrcidStringValue givenNames;
    @JsonProperty("family-name")
    private OrcidStringValue familyName;
}
