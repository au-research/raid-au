package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.doi.DataciteTypes;
import au.org.raid.api.vocabularies.datacite.ResourceTypeGeneral;
import org.springframework.stereotype.Component;

@Component
public class DataciteTypesFactory {
    public DataciteTypes create() {
        return new DataciteTypes()
                .setResourceType("RAiD")
                .setResourceTypeGeneral(ResourceTypeGeneral.PROJECT.getName());
    }
}