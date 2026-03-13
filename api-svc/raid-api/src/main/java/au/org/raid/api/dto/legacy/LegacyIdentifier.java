package au.org.raid.api.dto.legacy;

import lombok.Data;

@Data
public class LegacyIdentifier {
    private Integer version;
    private String identifier;
    private String raidAgencyUrl;
    private String identifierOwner;
    private String identifierSchemaURI;
    private Long identifierServicePoint;
    private String identifierRegistrationAgency;
}
