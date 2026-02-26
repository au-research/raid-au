package au.org.raid.api.model.datacite.doi;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class DataciteCreator {
    private String name;
    private String nameType;
    private List<NameIdentifier> nameIdentifiers;
}
