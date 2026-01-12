package au.org.raid.api.model.datacite.repository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataciteRepositoryData {
    private String type;
    private DataciteRepositoryAttributes attributes;
    private DataciteRepositoryRelationships relationships;

}
