package au.org.raid.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "raid.identifier")
public class IdentifierProperties {
    private String namePrefix;
    private String license;
    private String landingPrefix;
    private String registrationAgencyIdentifier;
    private String handleUrlPrefix;
    private String schemaUri;
}
