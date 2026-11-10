package au.org.raid.api.model.datacite.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataciteRight {

    @JsonProperty("rights")
    private String rights;

    @JsonProperty("rightsUri")
    private String rightsURI;

    @JsonProperty("rightsIdentifier")
    private String rightsIdentifier;

    @JsonProperty("rightsIdentifierScheme")
    private String rightsIdentifierScheme;

    @JsonProperty("schemeUri")
    private String schemeUri;
}
