package au.org.raid.api.model.schemaorg;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationAgencyId {
    @JsonProperty("@type")
    @Builder.Default
    private String type = "PropertyValue";

    @JsonProperty("propertyID")
    @Builder.Default
    private String propertyId = "raid:registrationAgency";

    private String value;
}
