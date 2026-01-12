package au.org.raid.api.model.datacite.repository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataciteRepositoryProviderData {
    private String id;
    private String type;
}
