package au.org.raid.api.model.datacite.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataciteRight {

    @JsonProperty("rights")
    private String rights;

    @JsonProperty("rightsUri")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String rightsURI;
}
