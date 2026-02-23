package au.org.raid.api.model.datacite.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataciteRelatedIdentifier {
    private String relatedIdentifier;
    private String relatedIdentifierType;
    private String relationType;
    private String resourceTypeGeneral;
}
