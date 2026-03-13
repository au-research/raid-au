package au.org.raid.api.config.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "raid.orcid-integration")
public class OrcidIntegrationProperties {
    private String orcidUrlPrefix;
    private String apiKey;
    private RaidListener raidListener;
    private ContributorIdLookup contributorIdLookup;

    @Getter
    @Setter
    public static class RaidListener {
        private String uri;
    }

    @Getter
    @Setter
    public static class ContributorIdLookup {
        private String uri;
    }
}
