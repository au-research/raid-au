package au.org.raid.api.model.datacite.doi;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;


@Data
@Accessors(chain = true)
public class DataciteDto {
    public String schemaVersion;
    public String type;
    private DataciteAttributesDto attributes;
    private List<DataciteIdentifier> dataciteIdentifiers = new ArrayList<>();
}
