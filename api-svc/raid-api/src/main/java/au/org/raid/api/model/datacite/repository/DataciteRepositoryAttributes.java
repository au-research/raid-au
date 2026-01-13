package au.org.raid.api.model.datacite.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataciteRepositoryAttributes {
    private String name;
    private String symbol;
    private String systemEmail;
    private String clientType;
    private String passwordInput;
}
