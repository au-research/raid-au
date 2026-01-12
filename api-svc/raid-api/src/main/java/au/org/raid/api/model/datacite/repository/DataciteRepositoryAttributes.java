package au.org.raid.api.model.datacite.repository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataciteRepositoryAttributes {
    private String name;
    private String symbol;
    private String systemEmail;
    private String clientType;
}
