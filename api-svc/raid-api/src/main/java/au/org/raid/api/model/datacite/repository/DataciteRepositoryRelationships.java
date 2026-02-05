package au.org.raid.api.model.datacite.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataciteRepositoryRelationships {
    private DataciteRepositoryProvider provider;
    private DataciteRepositoryPrefixes prefixes;
}
