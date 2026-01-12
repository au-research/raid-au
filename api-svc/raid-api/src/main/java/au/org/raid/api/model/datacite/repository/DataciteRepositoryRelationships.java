package au.org.raid.api.model.datacite.repository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataciteRepositoryRelationships {
    private DataciteRepositoryProvider provider;
}
