package au.org.raid.api.model.datacite.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataciteDescription {
    private String description;
    private String descriptionType;
    private String lang;
}
