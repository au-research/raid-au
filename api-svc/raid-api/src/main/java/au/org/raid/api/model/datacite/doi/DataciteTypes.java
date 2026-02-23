package au.org.raid.api.model.datacite.doi;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataciteTypes {
    private String resourceType;
    private String resourceTypeGeneral;
}
