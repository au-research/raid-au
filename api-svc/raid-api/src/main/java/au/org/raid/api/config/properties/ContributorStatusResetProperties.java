package au.org.raid.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "raid.contributor-status-reset")
@Data
public class ContributorStatusResetProperties {
    private boolean enabled = true;
    private int batchSize = 50;
    private long throttleMillis = 1000;
    private long pollDelayMillis = 60000;
}
