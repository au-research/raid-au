package au.org.raid.api.config.properties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "raid.contributor-validation")
public class ContributorValidationProperties {
    private String pattern;
    private ContributorTypeValidationProperties orcid;
    private ContributorTypeValidationProperties isni;

    @Getter
    @Setter
    @Builder
    public static class ContributorTypeValidationProperties {
        private String urlPrefix;
        private String schemaUri;
    }
}
